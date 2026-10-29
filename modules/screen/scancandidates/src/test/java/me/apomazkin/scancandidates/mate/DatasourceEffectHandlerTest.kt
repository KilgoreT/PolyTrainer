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
import me.apomazkin.lexeme.LemmaContext
import me.apomazkin.lexeme.LemmasOutcome
import me.apomazkin.lexeme.LexemesOutcome
import me.apomazkin.lexeme.LookupDenyReason
import me.apomazkin.lexeme.RecognitionOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Хендлер эффектов: исход юзкейса → сообщение на каждый случай (`when` в ветке
 * эффекта), доступ к файлу галереи отпускается в любом исходе (Д52), провал
 * эффекта объявлен в нём самом (`onFail`). Тексты отказов — AWP-53 до окна,
 * AWP-55 и AWP-61 на слове; запрос по лемме — LFL-103; исход записи — AWP-21.
 * Юзкейс — лямбдами.
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

    private val lookupRun = DatasourceEffect.LookupLemma(
        word = "run",
        lemma = "run",
        dictionaryId = Fx.DICT,
        text = Fx.TEXTBOOK_TEXT,
        translation = Fx.TEXTBOOK_TRANSLATION,
    )

    private val addLink = DatasourceEffect.AddWord(word = "link", batch = DomainFx.batch("link"))

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

    // ===== Запрос лемм =====

    @Test
    fun `ExtractLemmas with lemmas sends the text, the translation and the lists in UI types with the dictionary`() {
        val useCase = FakeUseCase(
            lemmasImpl = {
                LemmasOutcome.Found(dictionaryId = Fx.DICT, lemmas = DomainFx.sentence)
            },
        )

        val messages = run(useCase, DatasourceEffect.ExtractLemmas(Fx.tokens))

        val found = messages.single() as Msg.LemmasFound
        assertEquals(Fx.DICT, found.dictionaryId)
        assertEquals("Las nubes corrían por el cielo.", found.text)
        assertEquals("Облака бежали по небу.", found.translation)
        assertEquals(listOf("nube", "correr", "cielo"), found.lists.lexical.texts())
        assertEquals(listOf("el", "por"), found.lists.function.texts())
        assertTrue(found.lists.proper.isEmpty() && found.lists.unparsed.isEmpty())
        assertEquals(0, found.lists.countInDictionary())
        assertEquals(listOf(Fx.tokens), useCase.extracted)
    }

    @Test
    fun `ExtractLemmas without words sends LemmasEmpty`() {
        val useCase = FakeUseCase(lemmasImpl = { LemmasOutcome.NoWords })

        val messages = run(useCase, DatasourceEffect.ExtractLemmas(Fx.tokens))

        assertEquals(listOf<Msg>(Msg.LemmasEmpty), messages)
    }

    @Test
    fun `AWP-53_2 AWP-53_3 AWP-53_4 AWP-53_8 ExtractLemmas denied by key limit size or language sends the text of the reason without a retry`() {
        val expected = mapOf(
            LookupDenyReason.NO_ACCESS_KEY to R.string.photo_words_error_no_access,
            LookupDenyReason.UNAUTHORIZED to R.string.photo_words_error_no_access,
            LookupDenyReason.RATE_LIMITED to R.string.photo_words_error_rate_limited,
            LookupDenyReason.REQUEST_TOO_LARGE to R.string.photo_words_error_too_many,
            LookupDenyReason.UNSUPPORTED_LANGUAGE to R.string.photo_words_error_language,
        )

        expected.forEach { (reason, errorRes) ->
            val useCase = FakeUseCase(lemmasImpl = { LemmasOutcome.Denied(reason) })

            val messages = run(useCase, DatasourceEffect.ExtractLemmas(Fx.tokens))

            assertEquals(reason.name, listOf<Msg>(Msg.LemmasDenied(errorRes)), messages)
        }
    }

    @Test
    fun `AWP-53_1 ExtractLemmas with an unavailable model sends the analysis unavailable text with a retry`() {
        val useCase = FakeUseCase(
            lemmasImpl = { LemmasOutcome.Denied(LookupDenyReason.LLM_UNAVAILABLE) },
        )

        val messages = run(useCase, DatasourceEffect.ExtractLemmas(Fx.tokens))

        assertEquals(
            listOf<Msg>(Msg.LemmasUnavailable(R.string.scan_analysis_unavailable)),
            messages,
        )
    }

    @Test
    fun `AWP-53_5 ExtractLemmas with no answer from the server sends the did not respond text with a retry`() {
        val useCase = FakeUseCase(lemmasImpl = { LemmasOutcome.Unavailable })

        val messages = run(useCase, DatasourceEffect.ExtractLemmas(Fx.tokens))

        assertEquals(
            listOf<Msg>(Msg.LemmasUnavailable(R.string.photo_words_error_server)),
            messages,
        )
    }

    @Test
    fun `ExtractLemmas that throws goes to onFail with the server did not respond text`() {
        val useCase = FakeUseCase(lemmasImpl = { throw IllegalStateException("no dictionary") })
        val effect = DatasourceEffect.ExtractLemmas(Fx.tokens)

        assertThrows(IllegalStateException::class.java) { run(useCase, effect) }

        assertEquals(
            Msg.LemmasUnavailable(R.string.photo_words_error_server),
            effect.onFail(IllegalStateException()),
        )
    }

    // ===== Форма словаря =====

    @Test
    fun `LoadForm with a form sends it in UI types and asks it by dictionary and lemma texts`() {
        val useCase = FakeUseCase(loadFormImpl = { _, _ -> FormOutcome.Loaded(DomainFx.form) })

        val messages = run(useCase, DatasourceEffect.LoadForm(Fx.DICT, listOf("study")))

        val loaded = messages.single() as Msg.FormLoaded
        assertEquals(Fx.DICT, loaded.form.dictionaryId)
        assertEquals(
            listOf(DomainFx.TRANSLATION, DomainFx.PART_OF_SPEECH),
            loaded.form.components.map { it.id },
        )
        val options = loaded.form.options.getValue(DomainFx.PART_OF_SPEECH)
        assertEquals(listOf(DomainFx.NOUN), options.map { it.id })
        assertEquals(listOf(DomainFx.GROUP), loaded.form.groups.map { it.id })
        assertEquals(listOf(DomainFx.STUDY_WORD), loaded.form.existingLemmas.map { it.wordId })
        assertEquals(listOf(Fx.DICT to listOf("study")), useCase.formRequests)
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

    // ===== Значения слова (LFL-103, AWP-55, AWP-61) =====

    @Test
    fun `LFL-103_1 LookupLemma asks the lemma with the context of the photo at its dictionary and sends the found word in UI types`() {
        val useCase = FakeUseCase(
            lexemesImpl = { _, _, _ ->
                LexemesOutcome.Found(
                    word = DomainFx.lookupWord("run", translation = "бежать"),
                    recommendations = listOf(DomainFx.recommendation),
                    existing = listOf(DomainFx.existingChild),
                )
            },
        )

        val messages = run(useCase, lookupRun)

        val found = messages.single() as Msg.LexemesFound
        assertEquals("run", found.text)
        assertEquals("run", found.word.lemmas.single().text)
        assertEquals(listOf("бежать"), found.word.lemmas.single().lexemes.single().translations)
        assertTrue(found.word.lemmas.single().lexemes.single().inContext)
        assertEquals(listOf("Род"), found.recommendations.map { it.name })
        assertEquals(listOf(DomainFx.CHILD_WORD), found.existing.map { it.wordId })
        assertEquals(listOf(Fx.saved(9L, "ребёнок")), found.existing.single().lexemes)
        assertEquals(
            listOf(Triple(Fx.DICT, "run", LemmaContext(Fx.TEXTBOOK_TEXT, Fx.TEXTBOOK_TRANSLATION))),
            useCase.lexemeRequests,
        )
    }

    @Test
    fun `AWP-55_1 AWP-55_7 LookupLemma not found or without lexemes sends the text under the word`() {
        val notFound = run(FakeUseCase(lexemesImpl = { _, _, _ -> LexemesOutcome.NotFound }), lookupRun)
        val noLexemes = run(FakeUseCase(lexemesImpl = { _, _, _ -> LexemesOutcome.NoLexemes }), lookupRun)

        assertEquals(listOf<Msg>(Msg.LexemesNotFound("run", R.string.scan_not_found)), notFound)
        assertEquals(listOf<Msg>(Msg.LexemesNotFound("run", R.string.scan_no_lexemes)), noLexemes)
    }

    @Test
    fun `AWP-61_1 AWP-61_2 AWP-61_5 LookupLemma with an unavailable model, no answer or a failed word sends the reason with a retry`() {
        val model = run(
            FakeUseCase(lexemesImpl = { _, _, _ -> LexemesOutcome.Denied(LookupDenyReason.LLM_UNAVAILABLE) }),
            lookupRun,
        )
        val server = run(FakeUseCase(lexemesImpl = { _, _, _ -> LexemesOutcome.Unavailable }), lookupRun)
        val word = run(FakeUseCase(lexemesImpl = { _, _, _ -> LexemesOutcome.WordFailed }), lookupRun)

        assertEquals(listOf<Msg>(Msg.LexemesUnavailable("run", R.string.scan_server_unavailable)), model)
        assertEquals(listOf<Msg>(Msg.LexemesUnavailable("run", R.string.photo_words_error_server)), server)
        assertEquals(listOf<Msg>(Msg.LexemesUnavailable("run", R.string.scan_server_unavailable)), word)
    }

    @Test
    fun `AWP-61_3 AWP-61_4 AWP-61_6 AWP-61_8 LookupLemma denied by limit key language or size sends the text of the reason without a retry`() {
        val expected = mapOf(
            LookupDenyReason.NO_ACCESS_KEY to R.string.photo_words_error_no_access,
            LookupDenyReason.UNAUTHORIZED to R.string.photo_words_error_no_access,
            LookupDenyReason.RATE_LIMITED to R.string.photo_words_error_rate_limited,
            LookupDenyReason.REQUEST_TOO_LARGE to R.string.photo_words_error_too_many,
            LookupDenyReason.UNSUPPORTED_LANGUAGE to R.string.photo_words_error_language,
        )

        expected.forEach { (reason, errorRes) ->
            val useCase = FakeUseCase(lexemesImpl = { _, _, _ -> LexemesOutcome.Denied(reason) })

            val messages = run(useCase, lookupRun)

            assertEquals(reason.name, listOf<Msg>(Msg.LexemesDenied("run", errorRes)), messages)
        }
    }

    @Test
    fun `LookupLemma that throws goes to onFail with the server did not respond text under the word`() {
        val useCase = FakeUseCase(lexemesImpl = { _, _, _ -> throw IllegalStateException("no dictionary") })

        assertThrows(IllegalStateException::class.java) { run(useCase, lookupRun) }

        assertEquals(
            Msg.LexemesUnavailable("run", R.string.photo_words_error_server),
            lookupRun.onFail(IllegalStateException()),
        )
    }

    // ===== Запись слова (AWP-21) =====

    @Test
    fun `AddWord written sends WordAdded for the word of the effect`() {
        val useCase = FakeUseCase(
            addWordImpl = { AddWordsBatchOutcome.Added(wordIds = listOf(7L), lexemeIds = listOf(70L)) },
        )

        val messages = run(useCase, addLink)

        assertEquals(listOf<Msg>(Msg.WordAdded("link")), messages)
        assertEquals(listOf(DomainFx.batch("link")), useCase.batches)
    }

    @Test
    fun `AWP-21_3 AWP-21_4 AWP-21_6 AddWord rejected by group word or component sends its own message with the id`() {
        val group = run(
            FakeUseCase(addWordImpl = { AddWordsBatchOutcome.Rejected.GroupMissing(Fx.POTTER) }),
            addLink,
        )
        val word = run(
            FakeUseCase(addWordImpl = { AddWordsBatchOutcome.Rejected.WordMissing(Fx.BANK_1) }),
            addLink,
        )
        val component = run(
            FakeUseCase(addWordImpl = { AddWordsBatchOutcome.Rejected.TypeUnavailable(Fx.GLOSS) }),
            addLink,
        )

        assertEquals(listOf<Msg>(Msg.AddGroupGone("link", AddFailureUi.GroupMissing(Fx.POTTER))), group)
        assertEquals(listOf<Msg>(Msg.AddWordGone("link", AddFailureUi.WordMissing(Fx.BANK_1))), word)
        assertEquals(listOf<Msg>(Msg.AddComponentGone("link", AddFailureUi.TypeUnavailable(Fx.GLOSS))), component)
    }

    @Test
    fun `AWP-21_1 AddWord rejected without a core or without the dictionary sends the failure as is`() {
        val core = run(
            FakeUseCase(addWordImpl = { AddWordsBatchOutcome.Rejected.NoCoreValue(wordIndex = 0, lexemeIndex = 0) }),
            addLink,
        )
        val dictionary = run(
            FakeUseCase(addWordImpl = { AddWordsBatchOutcome.Rejected.DictionaryMissing }),
            addLink,
        )

        assertEquals(listOf<Msg>(Msg.AddFailed("link", AddFailureUi.NoCoreValue)), core)
        assertEquals(listOf<Msg>(Msg.AddFailed("link", AddFailureUi.DictionaryMissing)), dictionary)
    }

    @Test
    fun `AWP-21_1 AddWord that throws goes to onFail with the failed write under the word`() {
        val useCase = FakeUseCase(addWordImpl = { throw IllegalStateException("db closed") })

        assertThrows(IllegalStateException::class.java) { run(useCase, addLink) }

        assertEquals(Msg.AddFailed("link", AddFailureUi.Failed), addLink.onFail(IllegalStateException()))
    }

    // ===== Эффекты =====

    @Test
    fun `heavy effects print counts instead of the payload`() {
        val extract = DatasourceEffect.ExtractLemmas(Fx.tokens)

        assertEquals("ExtractLemmas(tokens=2)", extract.toString())
        assertEquals(
            "LoadForm(dictionaryId=1, lemmas=2)",
            DatasourceEffect.LoadForm(Fx.DICT, listOf("run", "bank")).toString(),
        )
        assertEquals(
            "LookupLemma(word=run, lemma=run, dictionaryId=1, text=${Fx.TEXTBOOK_TEXT.length})",
            lookupRun.toString(),
        )
        assertEquals("AddWord(word=link, lexemes=1)", addLink.toString())
    }

    @Test
    fun `the handler serves the whole family of data effects`() {
        val handler = DatasourceEffectHandler(FakeUseCase(), NoopLogger)

        assertEquals(DatasourceEffect::class, handler.effectFamily)
    }
}
