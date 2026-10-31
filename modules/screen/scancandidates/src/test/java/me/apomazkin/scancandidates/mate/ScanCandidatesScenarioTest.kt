package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateEffectHandler
import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.runMateTest
import io.github.kilgoret.mate.test.testReduce
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LemmasOutcome
import me.apomazkin.lexeme.LexemesOutcome
import me.apomazkin.lexeme.RecognitionOutcome
import me.apomazkin.scancandidates.ScanCandidatesAssembly
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Сценарии пользователя. Первые — цепочки сообщений через редьюсер, ответы
 * хендлера подаются вручную. Последние — весь цикл `Mate` с юзкейсом лямбдами и
 * записью переходов: проверяют, что редьюсер, хендлер и сборка встают вместе.
 */
class ScanCandidatesScenarioTest {
    private val reducer = reducer()

    // ===== Цепочки сообщений =====

    @Test
    fun `scenario - photo to window by the textbook paragraph with the dictionary marks`() {
        val analyzing = reducer.recognized()
        val loading = reducer.step(analyzing, Fx.found())

        val window = reducer.step(loading, Msg.FormLoaded(Fx.form(existing = listOf(Fx.study1))))

        assertEquals(ScanPhase.ANALYZING, analyzing.phase)
        assertEquals(ScanPhase.FORM_LOADING, loading.phase)
        assertEquals(ScanPhase.WINDOW, window.phase)
        assertEquals(41, window.lists.allTexts().size)
        assertEquals(listOf("study"), window.lists.lexical.marked())
        assertEquals(Fx.TEXTBOOK_TRANSLATION, window.translation)
        assertFalse(window.isTranslationExpanded)
        assertNull(window.errorRes)
    }

    @Test
    fun `scenario - no network then retry then the answer builds the window`() {
        val unavailable = reducer.step(
            reducer.recognized(),
            Msg.LemmasUnavailable(R.string.photo_words_error_server),
        )

        val retried = reducer.testReduce(unavailable, Msg.RetryLemmas)
        val answered = reducer.step(retried.state(), Fx.found())
        val window = reducer.step(answered, Msg.FormLoaded(Fx.form()))

        assertEquals(ScanPhase.ANALYZING, retried.state().phase)
        assertEquals(setOf<Effect>(DatasourceEffect.ExtractLemmas(Fx.tokens)), retried.effects())
        assertEquals(ScanPhase.WINDOW, window.phase)
        assertEquals(Fx.textbookLexical, window.lists.lexical.texts())
        assertNull(window.errorRes)
        assertFalse(window.hasRetryButton)
    }

    @Test
    fun `scenario - AWP-43_1 AWP-47_1 AWP-62_1 tap, plate, create, the row is gone`() {
        val two = Fx.lists(lexical = listOf("run", "bank"))
        val window = reducer.window(found = Fx.found(lists = two))
        val bankKey = "ruwiktionary/1.noun.1"

        val loading = reducer.testReduce(window, Msg.LoadLexemes("bank"))
        val loaded = reducer.step(loading.state(), Fx.lexemes("bank", Fx.bank))
        val adding = reducer.testReduce(loaded, Msg.AddLexeme("bank", bankKey))
        val added = reducer.step(adding.state(), Msg.WordCreated("bank", bankKey, 9L))

        assertEquals(CardPhase.LOADING, loading.state().card("bank").phase)
        assertTrue(loading.effects().single() is DatasourceEffect.LookupLemma)
        assertEquals(CardPhase.CANDIDATES, loaded.card("bank").phase)
        assertTrue(loaded.lexeme("bank", bankKey).canWrite)
        assertTrue(adding.state().card("bank").isAdding)
        assertTrue(adding.effects().single() is DatasourceEffect.CreateWord)
        assertEquals(listOf("run"), added.lists.lexical.texts())
        assertFalse(added.isAdding)
        assertEquals(ScanPhase.WINDOW, added.phase)
    }

    @Test
    fun `scenario - AWP-63_1 AWP-20_3 create the first plate, the saved plate appears, the second goes by transfer, the row is gone`() {
        val runKey = "ruwiktionary/1.verb.1"
        val manageKey = "ruwiktionary/1.verb.2"
        val loaded = reducer.withCard("run", Fx.run)

        val created = reducer.created(loaded, "run", runKey, Fx.RUN_CREATED)
        val reloaded = reducer.step(created, Msg.LemmasReloaded("run", listOf(Fx.runCreated)))
        val transferring = reducer.testReduce(reloaded, Msg.AddLexeme("run", manageKey))
        val transferred = reducer.step(transferring.state(), Msg.LexemeAdded("run", manageKey))

        assertEquals(listOf(Fx.savedWord(Fx.runCreated)), reloaded.card("run").saved)
        assertTrue(reloaded.word("run").isInDictionary)
        assertEquals(CardTargetUi.Existing(Fx.RUN_CREATED), reloaded.lexeme("run", manageKey).target)
        val effect = transferring.effects().single() as DatasourceEffect.TransferLexeme
        assertEquals(Fx.RUN_CREATED, effect.batch.words.single().wordId)
        assertNull(transferred.lists.wordOf("run"))
        assertFalse(transferred.isAdding)
    }

    // ===== Весь цикл Mate =====

    private class RecordingNavigation : MateEffectHandler<Nothing, NavigationEffect> {
        val effects = mutableListOf<NavigationEffect>()
        override val effectFamily = NavigationEffect::class

        override suspend fun runEffect(
            effect: NavigationEffect,
            consumer: (Nothing) -> Unit,
        ) {
            effects += effect
        }
    }

    private fun answeringUseCase() = FakeUseCase(
        recognizeImpl = { RecognitionOutcome.Text(Fx.tokens) },
        lemmasImpl = { LemmasOutcome.Found(dictionaryId = Fx.DICT, lemmas = DomainFx.textbook) },
        loadFormImpl = { _, _ -> FormOutcome.Loaded(DomainFx.form) },
        lexemesImpl = { _, lemma, _ -> LexemesOutcome.Found(DomainFx.lookupWord(lemma), emptyList()) },
        addWordImpl = { AddWordsBatchOutcome.Added(wordIds = listOf(9L), lexemeIds = listOf(90L)) },
    )

    /** Отвечающий юзкейс, у которого значения слова ждут ворот [gate]. */
    private fun gatedUseCase(gate: CompletableDeferred<LexemesOutcome>): FakeUseCase {
        val answering = answeringUseCase()
        return FakeUseCase(
            recognizeImpl = answering.recognizeImpl,
            lemmasImpl = answering.lemmasImpl,
            loadFormImpl = answering.loadFormImpl,
            lexemesImpl = { _, _, _ -> gate.await() },
        )
    }

    private fun TestScope.assemble(
        useCase: FakeUseCase,
        navigation: RecordingNavigation,
        scope: CoroutineScope,
    ) = ScanCandidatesAssembly.create(
        useCase = useCase,
        logger = NoopLogger,
        navigationHandler = navigation,
        uri = Fx.URI,
        coroutineScope = scope,
        io = StandardTestDispatcher(testScheduler),
    )

    @Test
    fun `loop - from the photo to the window with the lists and the marks, then close`() = runMateTest { scope ->
        val useCase = answeringUseCase()
        val navigation = RecordingNavigation()
        val mate = assemble(useCase, navigation, scope)

        testScheduler.advanceUntilIdle()
        val window = mate.state.value
        mate.accept(Msg.RequestClose)
        testScheduler.advanceUntilIdle()

        assertEquals(ScanPhase.WINDOW, window.phase)
        assertEquals(Fx.textbookLexical, window.lists.lexical.texts())
        assertEquals(Fx.textbookProper, window.lists.proper.texts())
        assertEquals(Fx.textbookFunction, window.lists.function.texts())
        assertEquals(listOf("study"), window.lists.lexical.marked())
        assertEquals(Fx.TEXTBOOK_TEXT, window.text)
        assertEquals(
            listOf("recognize:${Fx.URI}", "release:${Fx.URI}", "lemmas:2", "form:1"),
            useCase.calls,
        )
        assertEquals(41, useCase.formRequests.single().second.size)
        assertEquals(listOf<NavigationEffect>(NavigationEffect.Back), navigation.effects)
    }

    @Test
    fun `loop - AWP-43_1 AWP-62_1 a tap loads the plates of the word, create writes the plate and the row leaves the list`() =
        runMateTest { scope ->
            val useCase = answeringUseCase()
            val mate = assemble(useCase, RecordingNavigation(), scope)
            testScheduler.advanceUntilIdle()

            mate.accept(Msg.LoadLexemes("link"))
            testScheduler.advanceUntilIdle()
            val loaded = mate.state.value
            mate.accept(Msg.AddLexeme("link", "ruwiktionary/1.noun.1"))
            testScheduler.advanceUntilIdle()

            assertEquals(CardPhase.CANDIDATES, loaded.card("link").phase)
            assertTrue(loaded.lexeme("link", "ruwiktionary/1.noun.1").canWrite)
            assertEquals(
                listOf("Перевод: перевод", "Часть речи: существительное"),
                loaded.shown("link", "ruwiktionary/1.noun.1"),
            )
            assertEquals(Fx.textbookLexical - "link", mate.state.value.lists.lexical.texts())
            assertEquals("link", useCase.batches.single().words.single().text)
            assertTrue(useCase.calls.containsAll(listOf("lexemes:link", "add:link")))
            assertFalse(mate.state.value.isAdding)
        }

    @Test
    fun `loop - AWP-43_6 AWP-14_3 the state survives a re-read while loading and after an edit, one request only`() =
        runMateTest { scope ->
            val gate = CompletableDeferred<LexemesOutcome>()
            val useCase = gatedUseCase(gate)
            val mate = assemble(useCase, RecordingNavigation(), scope)
            testScheduler.advanceUntilIdle()

            mate.accept(Msg.LoadLexemes("link"))
            testScheduler.advanceUntilIdle()
            val loading = mate.state.value
            // Поворот экрана: UI пересоздан и читает стейт заново — тот же, второго запроса нет.
            val reread = mate.state.value
            gate.complete(LexemesOutcome.Found(DomainFx.lookupWord("link"), emptyList()))
            testScheduler.advanceUntilIdle()
            mate.accept(Msg.SetOption(Fx.componentRef("link", "ruwiktionary/1.noun.1", Fx.PART_OF_SPEECH), null))
            testScheduler.advanceUntilIdle()
            val edited = mate.state.value

            assertEquals(CardPhase.LOADING, loading.card("link").phase)
            assertEquals(loading, reread)
            assertEquals(1, useCase.lexemeRequests.size)
            assertEquals(CardPhase.CANDIDATES, edited.card("link").phase)
            assertEquals(emptyList<Long>(), edited.chosen("link", "ruwiktionary/1.noun.1", Fx.PART_OF_SPEECH))
            assertEquals(edited, mate.state.value)
        }

    @Test
    fun `loop - AWP-48_4 a word removed while loading does not come back with the late answer`() =
        runMateTest { scope ->
            val gate = CompletableDeferred<LexemesOutcome>()
            val useCase = gatedUseCase(gate)
            val mate = assemble(useCase, RecordingNavigation(), scope)
            testScheduler.advanceUntilIdle()

            mate.accept(Msg.LoadLexemes("link"))
            testScheduler.advanceUntilIdle()
            mate.accept(Msg.RemoveWord("link"))
            testScheduler.advanceUntilIdle()
            val deleted = mate.state.value
            gate.complete(LexemesOutcome.Found(DomainFx.lookupWord("link"), emptyList()))
            testScheduler.advanceUntilIdle()

            assertNull(deleted.lists.wordOf("link"))
            assertEquals(deleted, mate.state.value)
            assertEquals(0, mate.state.value.lists.countPending())
        }

    @Test
    fun `loop - TFP-11_1 TFP-11_2 an unread photo shows the error, no retry, file released`() =
        runMateTest { scope ->
            val useCase = FakeUseCase(recognizeImpl = { error("not an image") })
            val navigation = RecordingNavigation()
            val mate = assemble(useCase, navigation, scope)

            testScheduler.advanceUntilIdle()

            val state = mate.state.value
            assertEquals(ScanPhase.ERROR, state.phase)
            assertEquals(R.string.photo_words_error_recognition, state.errorRes)
            assertFalse(state.hasRetryButton)
            assertEquals(listOf("recognize:${Fx.URI}", "release:${Fx.URI}"), useCase.calls)
            assertTrue(navigation.effects.isEmpty())
        }

    @Test
    fun `loop - TFP-19_1 AWP-54_1 a photo without text shows no words and asks no server`() =
        runMateTest { scope ->
            val useCase = FakeUseCase(recognizeImpl = { RecognitionOutcome.Empty })
            val mate = assemble(useCase, RecordingNavigation(), scope)

            testScheduler.advanceUntilIdle()

            assertEquals(ScanPhase.NO_WORDS, mate.state.value.phase)
            assertEquals(listOf("recognize:${Fx.URI}", "release:${Fx.URI}"), useCase.calls)
        }

    @Test
    fun `loop - TFP-26_1 retry after no network asks again with the same tokens`() = runMateTest { scope ->
        var attempts = 0
        val answering = answeringUseCase()
        val useCase = FakeUseCase(
            recognizeImpl = answering.recognizeImpl,
            lemmasImpl = { tokens ->
                attempts++
                if (attempts == 1) LemmasOutcome.Unavailable else answering.lemmasImpl(tokens)
            },
            loadFormImpl = answering.loadFormImpl,
        )
        val mate = assemble(useCase, RecordingNavigation(), scope)
        testScheduler.advanceUntilIdle()
        val failed = mate.state.value

        mate.accept(Msg.RetryLemmas)
        testScheduler.advanceUntilIdle()

        assertEquals(ScanPhase.ERROR, failed.phase)
        assertEquals(R.string.photo_words_error_server, failed.errorRes)
        assertTrue(failed.hasRetryButton)
        assertEquals(ScanPhase.WINDOW, mate.state.value.phase)
        assertEquals(listOf(Fx.tokens, Fx.tokens), useCase.extracted)
    }

    @Test
    fun `loop - a server call that throws shows the error with a retry`() = runMateTest { scope ->
        val answering = answeringUseCase()
        val useCase = FakeUseCase(
            recognizeImpl = answering.recognizeImpl,
            lemmasImpl = { throw IllegalStateException("no dictionary") },
        )
        val mate = assemble(useCase, RecordingNavigation(), scope)

        testScheduler.advanceUntilIdle()

        assertEquals(ScanPhase.ERROR, mate.state.value.phase)
        assertEquals(R.string.photo_words_error_server, mate.state.value.errorRes)
        assertTrue(mate.state.value.hasRetryButton)
    }

    @Test
    fun `loop - AWP-60_1 closing the window leaves at once`() = runMateTest { scope ->
        val navigation = RecordingNavigation()
        val mate = assemble(answeringUseCase(), navigation, scope)
        testScheduler.advanceUntilIdle()

        mate.accept(Msg.RequestClose)
        testScheduler.advanceUntilIdle()

        assertEquals(ScanPhase.WINDOW, mate.state.value.phase)
        assertEquals(listOf<NavigationEffect>(NavigationEffect.Back), navigation.effects)
    }

    @Test
    fun `loop - TFP-21_1 a recognition that ends after the screen is closed goes nowhere`() =
        runMateTest { scope ->
            val gate = CompletableDeferred<RecognitionOutcome>()
            val useCase = FakeUseCase(recognizeImpl = { gate.await() })
            val mate = assemble(useCase, RecordingNavigation(), scope)
            testScheduler.advanceUntilIdle()
            val recognizing = mate.state.value

            scope.cancel()
            gate.complete(RecognitionOutcome.Text(Fx.tokens))
            testScheduler.advanceUntilIdle()

            assertEquals(ScanPhase.RECOGNIZING, recognizing.phase)
            assertEquals(recognizing, mate.state.value)
            assertEquals(listOf("recognize:${Fx.URI}", "release:${Fx.URI}"), useCase.calls)
        }

    @Test
    fun `loop - TFP-11_3 AWP-53_7 an error after the screen is closed is not shown`() =
        runMateTest { scope ->
            val gate = CompletableDeferred<RecognitionOutcome>()
            val useCase = FakeUseCase(recognizeImpl = { gate.await() })
            val mate = assemble(useCase, RecordingNavigation(), scope)
            testScheduler.advanceUntilIdle()
            val recognizing = mate.state.value

            scope.cancel()
            gate.completeExceptionally(IllegalStateException("not an image"))
            testScheduler.advanceUntilIdle()

            assertEquals(recognizing, mate.state.value)
            assertNull(mate.state.value.errorRes)
        }

    @Test
    fun `loop - TFP-21_2 an answer after the screen is closed goes nowhere`() =
        runMateTest { scope ->
            val gate = CompletableDeferred<LemmasOutcome>()
            val useCase = FakeUseCase(
                recognizeImpl = { RecognitionOutcome.Text(Fx.tokens) },
                lemmasImpl = { gate.await() },
            )
            val mate = assemble(useCase, RecordingNavigation(), scope)
            testScheduler.advanceUntilIdle()
            val analyzing = mate.state.value

            scope.cancel()
            gate.complete(LemmasOutcome.Found(Fx.DICT, DomainFx.textbook))
            testScheduler.advanceUntilIdle()

            val calls = listOf("recognize:${Fx.URI}", "release:${Fx.URI}", "lemmas:2")
            assertEquals(ScanPhase.ANALYZING, analyzing.phase)
            assertEquals(analyzing, mate.state.value)
            assertEquals(calls, useCase.calls)
        }

    @Test
    fun `loop - AWP-24_2 a form that fails after the screen is closed changes nothing`() =
        runMateTest { scope ->
            val gate = CompletableDeferred<FormOutcome>()
            val answering = answeringUseCase()
            val useCase = FakeUseCase(
                recognizeImpl = answering.recognizeImpl,
                lemmasImpl = answering.lemmasImpl,
                loadFormImpl = { _, _ -> gate.await() },
            )
            val mate = assemble(useCase, RecordingNavigation(), scope)
            testScheduler.advanceUntilIdle()
            val loading = mate.state.value

            scope.cancel()
            gate.completeExceptionally(IllegalStateException("db closed"))
            testScheduler.advanceUntilIdle()

            assertEquals(ScanPhase.FORM_LOADING, loading.phase)
            assertEquals(loading, mate.state.value)
            assertNull(mate.state.value.errorRes)
        }

    @Test
    fun `loop - AWP-24_3 closing while the form loads leaves at once`() = runMateTest { scope ->
        val gate = CompletableDeferred<FormOutcome>()
        val answering = answeringUseCase()
        val useCase = FakeUseCase(
            recognizeImpl = answering.recognizeImpl,
            lemmasImpl = answering.lemmasImpl,
            loadFormImpl = { _, _ -> gate.await() },
        )
        val navigation = RecordingNavigation()
        val mate = assemble(useCase, navigation, scope)
        testScheduler.advanceUntilIdle()

        mate.accept(Msg.RequestClose)
        testScheduler.advanceUntilIdle()

        assertEquals(ScanPhase.FORM_LOADING, mate.state.value.phase)
        assertEquals(listOf<NavigationEffect>(NavigationEffect.Back), navigation.effects)
    }
}
