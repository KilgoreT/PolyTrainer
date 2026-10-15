package me.apomazkin.wordstab.logic.photo

import me.apomazkin.candidates.PhotoAddFailure
import me.apomazkin.candidates.PhotoDenyReason
import me.apomazkin.candidates.PhotoFormOutcome
import me.apomazkin.candidates.PhotoLookupOutcome
import me.apomazkin.candidates.PhotoWord
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertEffects
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.textrecognition.RecognizedWord
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.logic.DatasourceEffect
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.PhotoPhase
import me.apomazkin.wordstab.logic.PhotoWordsState
import me.apomazkin.wordstab.logic.WordsTabState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Диалог «слова с фото» глазами reducer'а: инварианты запуска (WFP-01…03),
 * Ш5 → Ш6 (WFP-13, 60), ответ сервера → сборка окна (Ш10 → Ш1 AWP),
 * ветки В3…В7 (WFP-20…65). Адреса строк спеки в именах — `WFT-NN_k`.
 */
class PhotoWordsReducerTest {

    private val reducer = reducer()

    private fun token(text: String) = RecognizedWord(text = text, confidence = 0.9f, language = "es")

    private fun words(vararg words: PhotoWord) = PhotoLookupOutcome.Words(Fx.DICT, words.toList())

    // ===== Инварианты запуска =====

    @Test
    fun `WFP-01_1 a run after a closed run gets a bigger number`() {
        val first = reducer.testReduce(idle(), Msg.OpenPhotoSourceChooser).state()
        val closed = reducer.testReduce(first, Msg.ClosePhotoWords).state()
        val second = reducer.testReduce(closed, Msg.OpenPhotoSourceChooser).state()

        assertEquals(1L, first.photo().requestId)
        assertEquals(2L, second.photo().requestId)
    }

    @Test
    fun `WFP-01_2 thousand random event sequences never decrease the run number`() {
        repeat(1000) { seed ->
            var state = idle()
            var last = 0L
            for (message in randomMessages(Random(seed), state)) {
                state = reducer.testReduce(state, message).state()
                assertTrue("seed $seed: ${state.photo()}", state.photo().requestId >= last)
                last = state.photo().requestId
            }
        }
    }

    @Test
    fun `WFP-02_1 photo request answer and form of run two all carry number two`() {
        val chooser = reducer.testReduce(idle(), Msg.OpenPhotoSourceChooser).state()
        val picked = reducer.testReduce(chooser, Msg.PhotoPicked(uri = "content://photo/1"))
        val runId = picked.state().photo().requestId
        picked.assertEffects(setOf<Effect>(DatasourceEffect.RecognizeWords(uri = "content://photo/1", requestId = runId)))

        val looking = reducer.testReduce(picked.state(), Msg.TokensReady(listOf(token("perro")), runId))
        looking.assertEffects(setOf<Effect>(DatasourceEffect.LookupWords(listOf(token("perro")), runId)))

        val loading = reducer.testReduce(looking.state(), Msg.WordsLookedUp(words(Fx.ran), runId))
        loading.assertEffects(setOf<Effect>(DatasourceEffect.LoadPhotoForm(dictionaryId = Fx.DICT, lemmaTexts = listOf("run"), requestId = runId)))

        val shown = reducer.testReduce(loading.state(), Msg.PhotoFormLoaded(PhotoFormOutcome.Loaded(Fx.form(), null), runId))

        assertEquals(2L, runId)
        assertEquals(2L, loading.state().photo().requestId)
        assertEquals(2L, shown.state().photo().requestId)
        assertEquals(listOf("run"), shown.state().photo().cards.map { it.lemma })
    }

    @Test
    fun `WFP-02_2 WFP-18_1 WFP-18_2 return without a photo from camera or gallery shows the chooser again with the same run number`() {
        val chooser = reducer.testReduce(idle(), Msg.OpenPhotoSourceChooser).state()

        val again = reducer.testReduce(chooser, Msg.PhotoPickCancelled)

        assertTrue(again.state().photo().isChoosingSource)
        assertEquals(chooser.photo().requestId, again.state().photo().requestId)
        again.assertNoEffects()
    }

    @Test
    fun `WFP-03_1 result of run one is ignored when run two is current`() {
        val stale = reducer.step(reducer.step(reducer.step(idle(), Msg.PhotoPicked("content://photo/1")), Msg.ClosePhotoWords), Msg.OpenPhotoSourceChooser)

        val result = reducer.testReduce(stale, Msg.WordsLookedUp(words(Fx.ran), requestId = 1L))

        assertEquals(stale, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `WFP-03_2 WFP-03_3 WFP-03_4 AWP-24_2 random sequences never change state from a foreign number nor show indicator when closed nor window with indicator`() {
        repeat(1000) { seed ->
            var state = idle()
            for (message in randomMessages(Random(seed), state)) {
                val before = state
                state = reducer.testReduce(state, message).state()
                val foreign = message.runIdOrNull()?.let { it != before.photo().requestId || !before.photo().isOpen } ?: false
                if (foreign && message !is Msg.PhotoPicked) {
                    assertEquals("seed $seed: foreign $message changed state", before.photo(), state.photo())
                }
                if (!state.photo().isOpen) {
                    assertEquals("seed $seed: indicator while closed", PhotoPhase.IDLE, state.photo().phase)
                }
                if (state.photo().cards.isNotEmpty()) {
                    assertEquals("seed $seed: window with indicator", PhotoPhase.IDLE, state.photo().phase)
                }
            }
        }
    }

    // ===== Ш3, Ш4: снимок и распознавание =====

    @Test
    fun `WFP-13_1 picked photo shows the indicator in the recognizing phase with a new run number`() {
        val chooser = reducer.step(idle(), Msg.OpenPhotoSourceChooser)

        val result = reducer.testReduce(chooser, Msg.PhotoPicked(uri = "content://photo/1"))

        assertEquals(PhotoPhase.RECOGNIZING, result.state().photo().phase)
        assertTrue(result.state().photo().isBusy)
        assertEquals(2L, result.state().photo().requestId)
        result.assertEffects(setOf<Effect>(DatasourceEffect.RecognizeWords(uri = "content://photo/1", requestId = 2L)))
    }

    @Test
    fun `WFP-22_1 WFP-22_2 picked photo from camera or gallery is accepted when the dialog is closed after process death`() {
        val result = reducer.testReduce(idle(), Msg.PhotoPicked(uri = "content://photo/2"))

        assertTrue(result.state().photo().isOpen)
        assertEquals(PhotoPhase.RECOGNIZING, result.state().photo().phase)
    }

    @Test
    fun `WFP-22_3 pick cancelled after process death opens nothing`() {
        val result = reducer.testReduce(idle(), Msg.PhotoPickCancelled)

        assertFalse(result.state().photo().isOpen)
        result.assertNoEffects()
    }

    @Test
    fun `WFP-20_1 recognition failure hides the indicator and shows the error text`() {
        val recognizing = reducer.step(idle(), Msg.PhotoPicked("content://photo/1"))

        val result = reducer.testReduce(recognizing, Msg.RecognitionFailed(errorRes = 5, requestId = 1L))

        assertEquals(5, result.state().photo().errorRes)
        assertEquals(PhotoPhase.IDLE, result.state().photo().phase)
        assertTrue(result.state().photo().isOpen)
    }

    // ===== Ш5 → Ш6: токены и запрос =====

    @Test
    fun `WFP-13_2 tokens ready switch the indicator to the looking phase and send LookupWords`() {
        val recognizing = reducer.step(idle(), Msg.PhotoPicked("content://photo/1"))
        val tokens = listOf(token("corrían"), token("fuera"))

        val result = reducer.testReduce(recognizing, Msg.TokensReady(tokens, requestId = 1L))

        assertEquals(PhotoPhase.LOOKING, result.state().photo().phase)
        assertTrue(result.state().photo().isBusy)
        result.assertEffects(setOf<Effect>(DatasourceEffect.LookupWords(tokens, requestId = 1L)))
    }

    @Test
    fun `WFP-19_1 no tokens show no words found without a request`() {
        val recognizing = reducer.step(idle(), Msg.PhotoPicked("content://photo/1"))

        val result = reducer.testReduce(recognizing, Msg.TokensReady(emptyList(), requestId = 1L))

        assertTrue(result.state().photo().isEmptyResult)
        assertEquals(PhotoPhase.IDLE, result.state().photo().phase)
        result.assertNoEffects()
    }

    @Test
    fun `WFP-21_1 tokens of a run closed during recognition send nothing`() {
        val closed = reducer.step(reducer.step(idle(), Msg.PhotoPicked("content://photo/1")), Msg.ClosePhotoWords)

        val result = reducer.testReduce(closed, Msg.TokensReady(listOf(token("perro")), requestId = 1L))

        assertFalse(result.state().photo().isOpen)
        result.assertNoEffects()
    }

    // ===== Ш10 → Ш1 (AWP): ответ =====

    @Test
    fun `AWP-01_1 answer with words keeps the dictionary of the photo and loads its form`() {
        val answered = reducer.answered(Fx.fuera, Fx.ran)

        assertEquals(Fx.DICT, answered.photo().dictionaryId)
        assertEquals(PhotoPhase.FORM, answered.photo().phase)
        assertEquals(listOf("fuera", "ran"), answered.photo().words.map { it.text })
        assertNull(answered.photo().errorRes)
    }

    @Test
    fun `WFP-19_2 empty answer shows no words found`() {
        val looking = reducer.step(reducer.step(idle(), Msg.PhotoPicked("content://photo/1")), Msg.TokensReady(listOf(token("x")), 1L))

        val result = reducer.testReduce(looking, Msg.WordsLookedUp(words(), requestId = 1L))

        assertTrue(result.state().photo().isEmptyResult)
        assertTrue(result.state().photo().words.isEmpty())
        assertEquals(PhotoPhase.IDLE, result.state().photo().phase)
    }

    // ===== В3, В4: ошибка и отмена =====

    @Test
    fun `WFT-23_1 WFP-20_1 unavailable server shows server did not respond without the indicator`() {
        val looking = reducer.step(reducer.step(idle(), Msg.PhotoPicked("content://photo/1")), Msg.TokensReady(listOf(token("x")), 1L))

        val result = reducer.testReduce(looking, Msg.WordsLookedUp(PhotoLookupOutcome.Unavailable, requestId = 1L))

        assertEquals(R.string.photo_words_error_server, result.state().photo().errorRes)
        assertEquals(PhotoPhase.IDLE, result.state().photo().phase)
        assertTrue(result.state().photo().words.isEmpty())
    }

    @Test
    fun `WFP-20_2 WFP-21_2 answer of a cancelled run shows neither list nor error`() {
        val cancelled = reducer.step(reducer.step(reducer.step(idle(), Msg.PhotoPicked("content://photo/1")), Msg.TokensReady(listOf(token("x")), 1L)), Msg.ClosePhotoWords)

        val late = reducer.testReduce(cancelled, Msg.WordsLookedUp(words(Fx.ran), requestId = 1L))
        val lateError = reducer.testReduce(cancelled, Msg.WordsLookedUp(PhotoLookupOutcome.Unavailable, requestId = 1L))

        assertEquals(cancelled, late.state())
        assertEquals(cancelled, lateError.state())
        late.assertNoEffects()
        lateError.assertNoEffects()
    }

    @Test
    fun `WFP-21_3 cancel then a new run keeps the first answer away from the chooser and from the second result`() {
        val cancelled = reducer.step(reducer.step(reducer.step(idle(), Msg.PhotoPicked("content://photo/1")), Msg.TokensReady(listOf(token("x")), 1L)), Msg.ClosePhotoWords)
        val chooser = reducer.step(cancelled, Msg.OpenPhotoSourceChooser)

        val onChooser = reducer.step(chooser, Msg.WordsLookedUp(words(Fx.ran), requestId = 1L))
        assertEquals(chooser, onChooser)

        val second = reducer.step(onChooser, Msg.PhotoPicked(uri = "content://photo/2"))
        val secondLooking = reducer.step(second, Msg.TokensReady(listOf(token("second")), second.photo().requestId))
        val lateFirst = reducer.step(secondLooking, Msg.WordsLookedUp(words(Fx.ran), requestId = 1L))

        assertEquals(secondLooking, lateFirst)
        assertEquals(PhotoPhase.LOOKING, lateFirst.photo().phase)
    }

    // ===== В5: отказ =====

    @Test
    fun `WFT-63_1 WFT-19_1 unauthorized or missing key shows no access to the server`() {
        val looking = reducer.step(reducer.step(idle(), Msg.PhotoPicked("content://photo/1")), Msg.TokensReady(listOf(token("x")), 1L))

        val unauthorized = reducer.step(looking, Msg.WordsLookedUp(PhotoLookupOutcome.Denied(PhotoDenyReason.UNAUTHORIZED), 1L))
        val noKey = reducer.step(looking, Msg.WordsLookedUp(PhotoLookupOutcome.Denied(PhotoDenyReason.NO_ACCESS_KEY), 1L))

        assertEquals(R.string.photo_words_error_no_access, unauthorized.photo().errorRes)
        assertEquals(R.string.photo_words_error_no_access, noKey.photo().errorRes)
        assertTrue(unauthorized.photo().words.isEmpty())
    }

    @Test
    fun `WFT-63_2 WFT-63_3 WFT-63_4 rate limit size and language denials map to their texts`() {
        val looking = reducer.step(reducer.step(idle(), Msg.PhotoPicked("content://photo/1")), Msg.TokensReady(listOf(token("x")), 1L))

        assertEquals(R.string.photo_words_error_rate_limited, reducer.step(looking, Msg.WordsLookedUp(PhotoLookupOutcome.Denied(PhotoDenyReason.RATE_LIMITED), 1L)).photo().errorRes)
        assertEquals(R.string.photo_words_error_too_many, reducer.step(looking, Msg.WordsLookedUp(PhotoLookupOutcome.Denied(PhotoDenyReason.REQUEST_TOO_LARGE), 1L)).photo().errorRes)
        assertEquals(R.string.photo_words_error_language, reducer.step(looking, Msg.WordsLookedUp(PhotoLookupOutcome.Denied(PhotoDenyReason.UNSUPPORTED_LANGUAGE), 1L)).photo().errorRes)
    }

    // ===== В7: перезапуск =====

    @Test
    fun `WFP-23_1 AWP-27_1 fresh state after process death shows neither indicator nor window`() {
        val fresh = PhotoWordsState()

        assertFalse(fresh.isOpen)
        assertFalse(fresh.isBusy)
        assertTrue(fresh.cards.isEmpty())
        assertNull(fresh.form)
        assertNull(fresh.errorRes)
    }

    @Test
    fun `closing the dialog keeps the run number for the next run`() {
        val shown = reducer.step(reducer.step(idle(), Msg.PhotoPicked("content://photo/1")), Msg.TokensReady(listOf(token("x")), 1L))

        val result = reducer.testReduce(shown, Msg.ClosePhotoWords)

        assertEquals(PhotoWordsState(requestId = 1L), result.state().photo())
        result.assertNoEffects()
    }

    // ===== обвязка случайных последовательностей =====

    private fun Msg.runIdOrNull(): Long? = when (this) {
        is Msg.TokensReady -> requestId
        is Msg.WordsLookedUp -> requestId
        is Msg.RecognitionFailed -> requestId
        is Msg.PhotoFormLoaded -> requestId
        is Msg.PhotoAddFinished -> requestId
        is Msg.PhotoAddRejected -> requestId
        else -> null
    }

    /** Случайные события с перемешанными номерами запуска вокруг текущего, включая окно. */
    private fun randomMessages(random: Random, start: WordsTabState): List<Msg> {
        var runId = start.photoWordsState.requestId
        return List(random.nextInt(5, 40)) {
            val id = runId + random.nextInt(-2, 3)
            when (random.nextInt(16)) {
                0 -> Msg.OpenPhotoSourceChooser.also { runId++ }
                1 -> Msg.PhotoPicked(uri = "content://photo/$it").also { runId++ }
                2 -> Msg.PhotoPickCancelled
                3 -> Msg.TokensReady(listOf(token("w$it")), id)
                4 -> Msg.TokensReady(emptyList(), id)
                5 -> Msg.WordsLookedUp(words(Fx.ran, Fx.bank), id)
                6 -> Msg.RecognitionFailed(errorRes = 1, requestId = id)
                7 -> Msg.PhotoFormLoaded(PhotoFormOutcome.Loaded(Fx.form(), null), id)
                8 -> Msg.PhotoFormLoaded(PhotoFormOutcome.DictionaryMissing, id)
                9 -> Msg.DeletePhotoLexeme("run", "ru/v1")
                10 -> Msg.RequestPhotoAdd
                11 -> Msg.ConfirmPhotoAdd
                12 -> Msg.PhotoAddFinished(lexemeCount = 3, wordCount = 2, requestId = id)
                13 -> Msg.PhotoAddRejected(PhotoAddFailure.Failed, id)
                14 -> Msg.RequestClosePhotoWords
                else -> Msg.ClosePhotoWords
            }
        }
    }
}
