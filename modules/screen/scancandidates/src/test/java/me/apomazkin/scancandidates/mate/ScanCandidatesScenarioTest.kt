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
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.CardOwnGroupUi
import me.apomazkin.candidates.CardTargetUi
import me.apomazkin.candidates.RemarkUiItem
import me.apomazkin.candidates.countPhrase
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LookupOutcome
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
    fun `scenario - photo to window to write closes the screen with the counts`() {
        val window = reducer.window(Fx.ran, Fx.bank, form = Fx.form(existing = listOf(Fx.bank1)))
        val grouped = reducer.step(window, Msg.SetCommonGroup(Fx.POTTER))
        val asking = reducer.step(grouped, Msg.RequestAdd)

        val adding = reducer.testReduce(asking, Msg.ConfirmAdd)
        val batch = (adding.effects().single() as DatasourceEffect.AddWords).batch
        val done = reducer.testReduce(adding.state(), Msg.AddFinished(3, 2))

        assertEquals(
            "3 значения в 2 слова",
            countPhrase(asking.summary.lexemeCount, asking.summary.wordCount),
        )
        assertTrue(adding.state().isAdding)
        assertEquals(listOf("run", "bank"), batch.words.map { it.text })
        assertEquals(setOf<Effect>(ScanNavigationEffect.CloseWithResult(3, 2)), done.effects())
    }

    @Test
    fun `scenario - no network then retry then the answer builds the window`() {
        val unavailable = reducer.step(
            reducer.recognized(),
            Msg.LookupUnavailable(R.string.photo_words_error_server),
        )

        val retried = reducer.testReduce(unavailable, Msg.RetryLookup)
        val answered = reducer.step(
            retried.state(),
            Msg.WordsFound(Fx.DICT, listOf(Fx.perro), emptyList()),
        )
        val window = reducer.step(answered, Msg.FormLoaded(Fx.form()))

        assertEquals(ScanPhase.LOOKING, retried.state().phase)
        assertEquals(setOf<Effect>(DatasourceEffect.LookupWords(Fx.tokens)), retried.effects())
        assertEquals(ScanPhase.WINDOW, window.phase)
        assertEquals(listOf("perro"), window.cards.map { it.lemma })
        assertNull(window.errorRes)
        assertFalse(window.hasRetryButton)
    }

    @Test
    fun `scenario - rejected by a dead group, group reset, retry succeeds`() {
        val window = reducer.step(reducer.window(Fx.ran), Msg.SetCommonGroup(Fx.POTTER))
        val adding = reducer.step(reducer.step(window, Msg.RequestAdd), Msg.ConfirmAdd)
        val rejected = reducer.step(
            adding,
            Msg.AddGroupGone(AddFailureUi.GroupMissing(Fx.POTTER)),
        )

        val retry = reducer.testReduce(rejected, Msg.RetryAdd)
        val done = reducer.testReduce(retry.state(), Msg.AddFinished(2, 1))

        val batch = (retry.effects().single() as DatasourceEffect.AddWords).batch
        assertTrue(batch.words.all { it.groupId == null })
        assertTrue(retry.state().isAdding)
        assertNull(retry.state().addFailure)
        assertEquals(setOf<Effect>(ScanNavigationEffect.CloseWithResult(2, 1)), done.effects())
    }

    @Test
    fun `scenario - deletions then back asks, stay keeps, close leaves without a write`() {
        val window = reducer.window(Fx.ran, Fx.bank)
        val edited = reducer.step(
            reducer.step(window, Msg.DeleteCard("bank")),
            Msg.DeleteLexeme("run", "ruwiktionary/1.verb.1"),
        )
        val asked = reducer.step(edited, Msg.RequestClose)
        val stayed = reducer.step(asked, Msg.Stay)

        val closed = reducer.testReduce(reducer.step(stayed, Msg.RequestClose), Msg.ConfirmClose)

        assertTrue(asked.isConfirmingClose)
        assertEquals(listOf("run"), stayed.cards.map { it.lemma })
        assertEquals(1, stayed.card("run").lexemes.size)
        assertFalse(stayed.isConfirmingClose)
        assertEquals(setOf<Effect>(NavigationEffect.Back), closed.effects())
    }

    @Test
    fun `scenario - one window with a new word, a lexeme into an old word, a whole word`() {
        val form = Fx.form(existing = listOf(Fx.bank1, Fx.bank2))
        val window = reducer.window(Fx.ran, Fx.bank, Fx.perro, form = form)
        val trimmed = reducer.step(window, Msg.DeleteLexeme("run", "ruwiktionary/1.verb.2"))
        val secondBank = CardTargetUi.Existing(Fx.BANK_2)
        val moved = reducer.step(trimmed, Msg.SetCardTarget("bank", secondBank))
        val finance = CardOwnGroupUi.Group(Fx.FINANCE)
        val grouped = reducer.step(moved, Msg.SetCardGroup("perro", finance))

        val batch = reducer.batchOf(grouped)

        assertEquals(listOf("run", "bank", "perro"), batch.words.map { it.text })
        assertEquals(listOf<Long?>(null, Fx.BANK_2, null), batch.words.map { it.wordId })
        assertEquals(listOf(1, 1, 3), batch.words.map { it.lexemes.size })
        assertEquals(listOf<Long?>(null, null, Fx.FINANCE), batch.words.map { it.groupId })
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
        lookupImpl = {
            LookupOutcome.Words(
                dictionaryId = Fx.DICT,
                words = listOf(DomainFx.perro),
                recommendations = listOf(DomainFx.genderRecommendation),
            )
        },
        loadFormImpl = { _, _ -> FormOutcome.Loaded(DomainFx.form) },
        addImpl = { AddWordsBatchOutcome.Added(wordIds = listOf(100L), lexemeIds = listOf(1000L)) },
    )

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
    fun `loop - from the photo to the result on the navigation`() = runMateTest { scope ->
        val useCase = answeringUseCase()
        val navigation = RecordingNavigation()
        val mate = assemble(useCase, navigation, scope)

        testScheduler.advanceUntilIdle()
        val window = mate.state.value
        mate.accept(Msg.RequestAdd)
        mate.accept(Msg.ConfirmAdd)
        testScheduler.advanceUntilIdle()

        assertEquals(ScanPhase.WINDOW, window.phase)
        assertEquals(listOf("perro"), window.cards.map { it.lemma })
        assertEquals(CardTargetUi.Existing(DomainFx.PERRO_WORD), window.card("perro").target)
        assertEquals(2, window.remarks.size)
        assertTrue(window.remarks.last() is RemarkUiItem.ComponentRecommended)
        assertEquals(
            listOf("recognize:${Fx.URI}", "release:${Fx.URI}", "lookup:2", "form:1", "add:1"),
            useCase.calls,
        )
        assertEquals(listOf("perro"), useCase.formRequests.single().second)
        assertEquals(DomainFx.PERRO_WORD, useCase.batches.single().words.single().wordId)
        assertEquals(
            listOf<NavigationEffect>(ScanNavigationEffect.CloseWithResult(1, 1)),
            navigation.effects,
        )
    }

    @Test
    fun `loop - WFP-11_2 an unread photo shows the error, no retry, file released`() =
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
    fun `loop - a photo without text shows no words and asks no server`() = runMateTest { scope ->
        val useCase = FakeUseCase(recognizeImpl = { RecognitionOutcome.Empty })
        val mate = assemble(useCase, RecordingNavigation(), scope)

        testScheduler.advanceUntilIdle()

        assertEquals(ScanPhase.NO_WORDS, mate.state.value.phase)
        assertEquals(listOf("recognize:${Fx.URI}", "release:${Fx.URI}"), useCase.calls)
    }

    @Test
    fun `loop - retry after no network asks again with the same tokens`() = runMateTest { scope ->
        var attempts = 0
        val answering = answeringUseCase()
        val useCase = FakeUseCase(
            recognizeImpl = answering.recognizeImpl,
            lookupImpl = { tokens ->
                attempts++
                if (attempts == 1) LookupOutcome.Unavailable else answering.lookupImpl(tokens)
            },
            loadFormImpl = answering.loadFormImpl,
        )
        val mate = assemble(useCase, RecordingNavigation(), scope)
        testScheduler.advanceUntilIdle()
        val failed = mate.state.value

        mate.accept(Msg.RetryLookup)
        testScheduler.advanceUntilIdle()

        assertEquals(ScanPhase.ERROR, failed.phase)
        assertEquals(R.string.photo_words_error_server, failed.errorRes)
        assertTrue(failed.hasRetryButton)
        assertEquals(ScanPhase.WINDOW, mate.state.value.phase)
        assertEquals(listOf(Fx.tokens, Fx.tokens), useCase.lookedUp)
    }

    @Test
    fun `loop - a server call that throws shows the error with a retry`() = runMateTest { scope ->
        val answering = answeringUseCase()
        val useCase = FakeUseCase(
            recognizeImpl = answering.recognizeImpl,
            lookupImpl = { throw IllegalStateException("no dictionary") },
        )
        val mate = assemble(useCase, RecordingNavigation(), scope)

        testScheduler.advanceUntilIdle()

        assertEquals(ScanPhase.ERROR, mate.state.value.phase)
        assertEquals(R.string.photo_words_error_server, mate.state.value.errorRes)
        assertTrue(mate.state.value.hasRetryButton)
    }

    @Test
    fun `loop - a failed write shows the failure and retry writes again`() = runMateTest { scope ->
        var attempts = 0
        val answering = answeringUseCase()
        val useCase = FakeUseCase(
            recognizeImpl = answering.recognizeImpl,
            lookupImpl = answering.lookupImpl,
            loadFormImpl = answering.loadFormImpl,
            addImpl = { batch ->
                attempts++
                check(attempts > 1) { "rolled back" }
                answering.addImpl(batch)
            },
        )
        val navigation = RecordingNavigation()
        val mate = assemble(useCase, navigation, scope)
        testScheduler.advanceUntilIdle()

        mate.accept(Msg.RequestAdd)
        mate.accept(Msg.ConfirmAdd)
        testScheduler.advanceUntilIdle()
        val failed = mate.state.value
        mate.accept(Msg.RetryAdd)
        testScheduler.advanceUntilIdle()

        assertEquals(AddFailureUi.Failed, failed.addFailure)
        assertFalse(failed.isAdding)
        assertEquals(2, useCase.batches.size)
        assertEquals(useCase.batches[0], useCase.batches[1])
        assertEquals(
            listOf<NavigationEffect>(ScanNavigationEffect.CloseWithResult(1, 1)),
            navigation.effects,
        )
    }

    @Test
    fun `loop - closing an untouched window leaves at once`() = runMateTest { scope ->
        val navigation = RecordingNavigation()
        val mate = assemble(answeringUseCase(), navigation, scope)
        testScheduler.advanceUntilIdle()

        mate.accept(Msg.RequestClose)
        testScheduler.advanceUntilIdle()

        assertFalse(mate.state.value.isConfirmingClose)
        assertEquals(listOf<NavigationEffect>(NavigationEffect.Back), navigation.effects)
    }

    @Test
    fun `loop - closing a dirty window asks first, writes nothing`() = runMateTest { scope ->
        val useCase = answeringUseCase()
        val navigation = RecordingNavigation()
        val mate = assemble(useCase, navigation, scope)
        testScheduler.advanceUntilIdle()

        mate.accept(Msg.DeleteCard("perro"))
        mate.accept(Msg.RequestClose)
        testScheduler.advanceUntilIdle()
        val asking = mate.state.value
        val askedBefore = navigation.effects.toList()
        mate.accept(Msg.ConfirmClose)
        testScheduler.advanceUntilIdle()

        assertTrue(asking.isConfirmingClose)
        assertTrue(askedBefore.isEmpty())
        assertEquals(listOf<NavigationEffect>(NavigationEffect.Back), navigation.effects)
        assertEquals(0, useCase.batches.size)
    }

    @Test
    fun `loop - WFP-21_1 a recognition that ends after the screen is closed goes nowhere`() =
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
    fun `loop - WFP-20_2 AWP-24_2 an error after the screen is closed is not shown`() =
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
    fun `loop - WFP-21_2 an answer after the screen is closed goes nowhere`() =
        runMateTest { scope ->
            val gate = CompletableDeferred<LookupOutcome>()
            val useCase = FakeUseCase(
                recognizeImpl = { RecognitionOutcome.Text(Fx.tokens) },
                lookupImpl = { gate.await() },
            )
            val mate = assemble(useCase, RecordingNavigation(), scope)
            testScheduler.advanceUntilIdle()
            val looking = mate.state.value

            scope.cancel()
            gate.complete(LookupOutcome.Words(Fx.DICT, listOf(DomainFx.perro), emptyList()))
            testScheduler.advanceUntilIdle()

            val calls = listOf("recognize:${Fx.URI}", "release:${Fx.URI}", "lookup:2")
            assertEquals(ScanPhase.LOOKING, looking.phase)
            assertEquals(looking, mate.state.value)
            assertEquals(calls, useCase.calls)
        }
}
