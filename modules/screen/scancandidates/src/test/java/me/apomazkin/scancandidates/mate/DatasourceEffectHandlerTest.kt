package me.apomazkin.scancandidates.mate

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LookupDenyReason
import me.apomazkin.lexeme.LookupOutcome
import me.apomazkin.lexeme.RecognitionOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Хендлер эффектов: исход юзкейса → сообщение на каждый случай (`when` в ветке
 * эффекта), доступ к файлу галереи отпускается в любом исходе (Д52), провал
 * эффекта объявлен в нём самом (`onFail`). Юзкейс — лямбдами.
 */
class DatasourceEffectHandlerTest {
    private val uri = Fx.URI

    private fun run(
        useCase: FakeUseCase,
        effect: DatasourceEffect,
    ): List<Msg> {
        val messages = mutableListOf<Msg>()
        val handler = DatasourceEffectHandler(useCase, NoopLogger, Dispatchers.Unconfined)
        runBlocking { handler.runEffect(effect) { messages += it } }
        return messages
    }

    // ===== Распознавание =====

    @Test
    fun `RecognizeText with text sends the tokens as recognized and releases the file after`() {
        val useCase = FakeUseCase(recognizeImpl = { RecognitionOutcome.Text(Fx.tokens) })

        val messages = run(useCase, DatasourceEffect.RecognizeText(uri))

        assertEquals(listOf<Msg>(Msg.TextRecognized(Fx.tokens)), messages)
        assertEquals(listOf("recognize:$uri", "release:$uri"), useCase.calls)
    }

    @Test
    fun `RecognizeText without text sends TextEmpty and releases the file too`() {
        val useCase = FakeUseCase(recognizeImpl = { RecognitionOutcome.Empty })

        val messages = run(useCase, DatasourceEffect.RecognizeText(uri))

        assertEquals(listOf<Msg>(Msg.TextEmpty), messages)
        assertEquals(listOf("recognize:$uri", "release:$uri"), useCase.calls)
    }

    @Test
    fun `RecognizeText that throws still releases the file and lets the error go to onFail`() {
        val useCase = FakeUseCase(recognizeImpl = { throw IllegalStateException("boom") })
        val effect = DatasourceEffect.RecognizeText(uri)

        assertThrows(IllegalStateException::class.java) { run(useCase, effect) }

        assertEquals(listOf("recognize:$uri", "release:$uri"), useCase.calls)
        assertEquals(
            Msg.RecognitionFailed(R.string.photo_words_error_recognition),
            effect.onFail(IllegalStateException()),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `RecognizeText cancelled with the screen still releases the file`() = runTest {
        var released = false
        val useCase = FakeUseCase(
            recognizeImpl = { awaitCancellation() },
            releaseImpl = {
                delay(10)
                released = true
            },
        )
        val io = StandardTestDispatcher(testScheduler)
        val handler = DatasourceEffectHandler(useCase, NoopLogger, io)

        val job = launch { handler.runEffect(DatasourceEffect.RecognizeText(uri)) {} }
        runCurrent()
        job.cancel(CancellationException("screen closed"))
        advanceUntilIdle()

        assertTrue(released)
    }

    // ===== Запрос слов =====

    @Test
    fun `LookupWords with words sends them in UI types with the dictionary and advice`() {
        val useCase = FakeUseCase(
            lookupImpl = {
                LookupOutcome.Words(
                    dictionaryId = Fx.DICT,
                    words = listOf(DomainFx.perro),
                    recommendations = listOf(DomainFx.genderRecommendation),
                )
            },
        )

        val messages = run(useCase, DatasourceEffect.LookupWords(Fx.tokens))

        val found = messages.single() as Msg.WordsFound
        assertEquals(Fx.DICT, found.dictionaryId)
        assertEquals(listOf("perro"), found.words.map { it.text })
        val lexeme = found.words.single().lemmas.single().lexemes.single()
        assertEquals("ruwiktionary/1.noun.1", lexeme.key)
        assertEquals(
            listOf(DomainFx.TRANSLATION, DomainFx.PART_OF_SPEECH),
            lexeme.componentMatches.map { it.componentId },
        )
        assertEquals(1, found.words.single().remarks.size)
        assertEquals(listOf("Род"), found.recommendations.map { it.name })
        assertEquals(listOf(Fx.tokens), useCase.lookedUp)
    }

    @Test
    fun `LookupWords without words sends WordsEmpty`() {
        val useCase = FakeUseCase(lookupImpl = { LookupOutcome.NoWords })

        val messages = run(useCase, DatasourceEffect.LookupWords(Fx.tokens))

        assertEquals(listOf<Msg>(Msg.WordsEmpty), messages)
    }

    @Test
    fun `LookupWords denied by key or limit sends the text of the reason without a retry`() {
        val expected = mapOf(
            LookupDenyReason.NO_ACCESS_KEY to R.string.photo_words_error_no_access,
            LookupDenyReason.UNAUTHORIZED to R.string.photo_words_error_no_access,
            LookupDenyReason.RATE_LIMITED to R.string.photo_words_error_rate_limited,
            LookupDenyReason.REQUEST_TOO_LARGE to R.string.photo_words_error_too_many,
            LookupDenyReason.UNSUPPORTED_LANGUAGE to R.string.photo_words_error_language,
        )

        expected.forEach { (reason, errorRes) ->
            val useCase = FakeUseCase(lookupImpl = { LookupOutcome.Denied(reason) })

            val messages = run(useCase, DatasourceEffect.LookupWords(Fx.tokens))

            assertEquals(reason.name, listOf<Msg>(Msg.LookupDenied(errorRes)), messages)
        }
    }

    @Test
    fun `LookupWords with an unavailable model sends the server unavailable text with a retry`() {
        val useCase = FakeUseCase(
            lookupImpl = { LookupOutcome.Denied(LookupDenyReason.LLM_UNAVAILABLE) },
        )

        val messages = run(useCase, DatasourceEffect.LookupWords(Fx.tokens))

        assertEquals(
            listOf<Msg>(Msg.LookupUnavailable(R.string.scan_server_unavailable)),
            messages,
        )
    }

    @Test
    fun `LookupWords with no answer from the server sends the did not respond text with retry`() {
        val useCase = FakeUseCase(lookupImpl = { LookupOutcome.Unavailable })

        val messages = run(useCase, DatasourceEffect.LookupWords(Fx.tokens))

        assertEquals(
            listOf<Msg>(Msg.LookupUnavailable(R.string.photo_words_error_server)),
            messages,
        )
    }

    @Test
    fun `LookupWords that throws goes to onFail with the server did not respond text`() {
        val useCase = FakeUseCase(lookupImpl = { throw IllegalStateException("no dictionary") })
        val effect = DatasourceEffect.LookupWords(Fx.tokens)

        assertThrows(IllegalStateException::class.java) { run(useCase, effect) }

        assertEquals(
            Msg.LookupUnavailable(R.string.photo_words_error_server),
            effect.onFail(IllegalStateException()),
        )
    }

    // ===== Форма словаря =====

    @Test
    fun `LoadForm with a form sends it in UI types and asks it by dictionary and lemmas`() {
        val useCase = FakeUseCase(loadFormImpl = { _, _ -> FormOutcome.Loaded(DomainFx.form) })

        val messages = run(useCase, DatasourceEffect.LoadForm(Fx.DICT, listOf("perro")))

        val loaded = messages.single() as Msg.FormLoaded
        assertEquals(Fx.DICT, loaded.form.dictionaryId)
        assertEquals(
            listOf(DomainFx.TRANSLATION, DomainFx.PART_OF_SPEECH),
            loaded.form.components.map { it.id },
        )
        val options = loaded.form.options.getValue(DomainFx.PART_OF_SPEECH)
        assertEquals(listOf(DomainFx.NOUN), options.map { it.id })
        assertEquals(listOf(DomainFx.GROUP), loaded.form.groups.map { it.id })
        assertEquals(listOf(DomainFx.PERRO_WORD), loaded.form.existingLemmas.map { it.wordId })
        assertEquals(listOf(Fx.DICT to listOf("perro")), useCase.formRequests)
    }

    @Test
    fun `AWP-24_4 LoadForm for a deleted dictionary sends the dictionary deleted text`() {
        val useCase = FakeUseCase(loadFormImpl = { _, _ -> FormOutcome.DictionaryMissing })

        val messages = run(useCase, DatasourceEffect.LoadForm(Fx.DICT, emptyList()))

        assertEquals(
            listOf<Msg>(Msg.FormFailed(R.string.photo_add_dictionary_missing)),
            messages,
        )
    }

    @Test
    fun `AWP-24_1 AWP-24_5 LoadForm that failed sends the could not build the window text`() {
        val useCase = FakeUseCase(loadFormImpl = { _, _ -> FormOutcome.Failed("boom") })

        val messages = run(useCase, DatasourceEffect.LoadForm(Fx.DICT, emptyList()))

        assertEquals(listOf<Msg>(Msg.FormFailed(R.string.photo_add_form_failed)), messages)
    }

    @Test
    fun `LoadForm that throws goes to onFail with the could not build the window text`() {
        val useCase = FakeUseCase(loadFormImpl = { _, _ -> throw IllegalStateException("boom") })
        val effect = DatasourceEffect.LoadForm(Fx.DICT, emptyList())

        assertThrows(IllegalStateException::class.java) { run(useCase, effect) }

        assertEquals(
            Msg.FormFailed(R.string.photo_add_form_failed),
            effect.onFail(IllegalStateException()),
        )
    }

    // ===== Запись =====

    @Test
    fun `AddWords written sends the counts of lexemes and words`() {
        val added = AddWordsBatchOutcome.Added(
            wordIds = listOf(1L, 2L),
            lexemeIds = listOf(10L, 11L, 12L),
        )
        val useCase = FakeUseCase(addImpl = { added })

        val messages = run(useCase, DatasourceEffect.AddWords(DomainFx.batch))

        assertEquals(listOf<Msg>(Msg.AddFinished(lexemeCount = 3, wordCount = 2)), messages)
        assertEquals(listOf(DomainFx.batch), useCase.batches)
    }

    @Test
    fun `AddWords rejected by a dead group, word or component sends one message each`() {
        val group = FakeUseCase(addImpl = { AddWordsBatchOutcome.Rejected.GroupMissing(701L) })
        val word = FakeUseCase(addImpl = { AddWordsBatchOutcome.Rejected.WordMissing(5L) })
        val type = FakeUseCase(addImpl = { AddWordsBatchOutcome.Rejected.TypeUnavailable(4L) })

        val effect = DatasourceEffect.AddWords(DomainFx.batch)

        assertEquals(
            listOf<Msg>(Msg.AddGroupGone(AddFailureUi.GroupMissing(701L))),
            run(group, effect),
        )
        assertEquals(
            listOf<Msg>(Msg.AddWordGone(AddFailureUi.WordMissing(5L))),
            run(word, effect),
        )
        assertEquals(
            listOf<Msg>(Msg.AddComponentGone(AddFailureUi.TypeUnavailable(4L))),
            run(type, effect),
        )
    }

    @Test
    fun `AddWords rejected without a core or for a deleted dictionary sends AddFailed`() {
        val core = FakeUseCase(addImpl = { AddWordsBatchOutcome.Rejected.NoCoreValue(0, 1) })
        val dictionary = FakeUseCase(addImpl = { AddWordsBatchOutcome.Rejected.DictionaryMissing })

        val effect = DatasourceEffect.AddWords(DomainFx.batch)

        assertEquals(listOf<Msg>(Msg.AddFailed(AddFailureUi.NoCoreValue)), run(core, effect))
        assertEquals(
            listOf<Msg>(Msg.AddFailed(AddFailureUi.DictionaryMissing)),
            run(dictionary, effect),
        )
    }

    @Test
    fun `AddWords that throws goes to onFail with the failed reason`() {
        val useCase = FakeUseCase(addImpl = { throw IllegalStateException("rolled back") })
        val effect = DatasourceEffect.AddWords(DomainFx.batch)

        assertThrows(IllegalStateException::class.java) { run(useCase, effect) }

        assertEquals(Msg.AddFailed(AddFailureUi.Failed), effect.onFail(IllegalStateException()))
    }

    // ===== Эффекты =====

    @Test
    fun `heavy effects print counts instead of the payload`() {
        assertEquals("LookupWords(tokens=2)", DatasourceEffect.LookupWords(Fx.tokens).toString())
        assertEquals(
            "LoadForm(dictionaryId=1, lemmas=2)",
            DatasourceEffect.LoadForm(Fx.DICT, listOf("run", "bank")).toString(),
        )
        assertEquals(
            "AddWords(dictionaryId=1, words=1, lexemes=1)",
            DatasourceEffect.AddWords(DomainFx.batch).toString(),
        )
    }

    @Test
    fun `the handler serves the whole family of data effects`() {
        val handler = DatasourceEffectHandler(FakeUseCase(), NoopLogger)

        assertEquals(DatasourceEffect::class, handler.effectFamily)
    }
}
