package me.apomazkin.wordstab.logic.photo

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertEffects
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.textrecognition.RecognizedWord
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.deps.PhotoDenyReason
import me.apomazkin.wordstab.deps.PhotoLookupOutcome
import me.apomazkin.wordstab.deps.PhotoWord
import me.apomazkin.wordstab.deps.PhotoWordStatus
import me.apomazkin.wordstab.logic.DatasourceEffect
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.PhotoPhase
import me.apomazkin.wordstab.logic.PhotoWordsState
import me.apomazkin.wordstab.logic.WordsTabReducer
import me.apomazkin.wordstab.logic.WordsTabState
import me.apomazkin.wordstab.logic.hidePhotoWords
import me.apomazkin.wordstab.logic.showPhotoSourceChooser
import me.apomazkin.wordstab.logic.startLooking
import me.apomazkin.wordstab.logic.startRecognizing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Диалог «слова с фото» глазами редьюсера: инварианты запуска (WFT-01…03),
 * Ш5 → Ш6 (WFT-16, 60), Ш10 → Ш11 (WFT-56), ветки В3…В7 (WFT-61…65).
 * Адреса строк спеки в именах — `WFT-NN_k` (точка в имени метода JVM
 * запрещена).
 */
class PhotoWordsReducerTest {

    private val reducer = WordsTabReducer(
            logger = object : LexemeLogger {
                override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {}
            },
    )

    private fun idle() = WordsTabState(isLoading = false)

    private fun token(text: String) = RecognizedWord(text = text, confidence = 0.9f, language = "es")

    private fun found(text: String, lemma: String, partOfSpeech: String) =
        PhotoWord(text = text, status = PhotoWordStatus.FOUND, lemma = lemma, partOfSpeech = partOfSpeech, meaning = null, gloss = "to $lemma")

    private fun notFound(text: String) =
        PhotoWord(text = text, status = PhotoWordStatus.NOT_FOUND, lemma = null, partOfSpeech = null, meaning = null, gloss = null)

    private fun words(vararg words: PhotoWord) = PhotoLookupOutcome.Words(words.toList())

    private fun WordsTabState.photo() = photoWordsState

    // ===== Инварианты запуска =====

    @Test
    fun `WFT-01_1 a run after a closed run gets a bigger number`() {
        val first = reducer.testReduce(idle(), Msg.OpenPhotoSourceChooser).state()
        val closed = reducer.testReduce(first, Msg.ClosePhotoWords).state()
        val second = reducer.testReduce(closed, Msg.OpenPhotoSourceChooser).state()

        assertEquals(1L, first.photo().requestId)
        assertEquals(2L, second.photo().requestId)
    }

    @Test
    fun `WFT-01_2 thousand random event sequences never decrease the run number`() {
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
    fun `WFT-02_1 photo request and answer of run two all carry number two`() {
        val chooser = reducer.testReduce(idle(), Msg.OpenPhotoSourceChooser).state()
        val picked = reducer.testReduce(chooser, Msg.PhotoPicked(uri = "content://photo/1"))
        val runId = picked.state().photo().requestId
        picked.assertEffects(setOf<Effect>(DatasourceEffect.RecognizeWords(uri = "content://photo/1", requestId = runId)))

        val looking = reducer.testReduce(picked.state(), Msg.TokensReady(listOf(token("perro")), runId))
        looking.assertEffects(setOf<Effect>(DatasourceEffect.LookupWords(listOf(token("perro")), runId)))

        val shown = reducer.testReduce(looking.state(), Msg.WordsLookedUp(words(found("perro", "perro", "noun")), runId))

        assertEquals(2L, runId)
        assertEquals(2L, looking.state().photo().requestId)
        assertEquals(2L, shown.state().photo().requestId)
        assertEquals(listOf("perro"), shown.state().photo().words.map { it.text })
    }

    @Test
    fun `WFT-02_2 WFT-59_1 WFT-59_2 return without a photo from camera or gallery shows the chooser again with the same run number`() {
        val chooser = reducer.testReduce(idle(), Msg.OpenPhotoSourceChooser).state()

        val again = reducer.testReduce(chooser, Msg.PhotoPickCancelled)

        assertTrue(again.state().photo().isChoosingSource)
        assertEquals(chooser.photo().requestId, again.state().photo().requestId)
        again.assertNoEffects()
    }

    @Test
    fun `WFT-03_1 result of run one is ignored when run two is current`() {
        val stale = idle().startRecognizing().hidePhotoWords().showPhotoSourceChooser()

        val result = reducer.testReduce(stale, Msg.WordsLookedUp(words(found("stale", "stale", "noun")), requestId = 1L))

        assertEquals(stale, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `WFT-03_2 WFT-03_3 WFT-03_4 random sequences never change state from a foreign number nor show indicator when closed nor list with indicator`() {
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
                if (state.photo().words.isNotEmpty()) {
                    assertEquals("seed $seed: list with indicator", PhotoPhase.IDLE, state.photo().phase)
                }
            }
        }
    }

    // ===== Ш3, Ш4: снимок и распознавание =====

    @Test
    fun `WFT-16_1 picked photo shows the indicator in the recognizing phase with a new run number`() {
        val chooser = idle().showPhotoSourceChooser()

        val result = reducer.testReduce(chooser, Msg.PhotoPicked(uri = "content://photo/1"))

        assertEquals(PhotoPhase.RECOGNIZING, result.state().photo().phase)
        assertTrue(result.state().photo().isBusy)
        assertEquals(2L, result.state().photo().requestId)
        result.assertEffects(setOf<Effect>(DatasourceEffect.RecognizeWords(uri = "content://photo/1", requestId = 2L)))
    }

    @Test
    fun `WFT-64_1 WFT-64_2 picked photo from camera or gallery is accepted when the dialog is closed after process death`() {
        val result = reducer.testReduce(idle(), Msg.PhotoPicked(uri = "content://photo/2"))

        assertTrue(result.state().photo().isOpen)
        assertEquals(PhotoPhase.RECOGNIZING, result.state().photo().phase)
    }

    @Test
    fun `WFT-64_3 pick cancelled after process death opens nothing`() {
        val result = reducer.testReduce(idle(), Msg.PhotoPickCancelled)

        assertFalse(result.state().photo().isOpen)
        result.assertNoEffects()
    }

    @Test
    fun `WFT-61_1 recognition failure hides the indicator and shows the error text`() {
        val recognizing = idle().startRecognizing()

        val result = reducer.testReduce(recognizing, Msg.RecognitionFailed(errorRes = 5, requestId = 1L))

        assertEquals(5, result.state().photo().errorRes)
        assertEquals(PhotoPhase.IDLE, result.state().photo().phase)
        assertTrue(result.state().photo().isOpen)
    }

    // ===== Ш5 → Ш6: токены и запрос =====

    @Test
    fun `WFT-16_2 tokens ready switch the indicator to the looking phase and send LookupWords`() {
        val recognizing = idle().startRecognizing()
        val tokens = listOf(token("corrían"), token("fuera"))

        val result = reducer.testReduce(recognizing, Msg.TokensReady(tokens, requestId = 1L))

        assertEquals(PhotoPhase.LOOKING, result.state().photo().phase)
        assertTrue(result.state().photo().isBusy)
        result.assertEffects(setOf<Effect>(DatasourceEffect.LookupWords(tokens, requestId = 1L)))
    }

    @Test
    fun `WFT-60_1 no tokens show no words found without a request`() {
        val recognizing = idle().startRecognizing()

        val result = reducer.testReduce(recognizing, Msg.TokensReady(emptyList(), requestId = 1L))

        assertTrue(result.state().photo().isEmptyResult)
        assertEquals(PhotoPhase.IDLE, result.state().photo().phase)
        result.assertNoEffects()
    }

    @Test
    fun `WFT-62_1 tokens of a run closed during recognition send nothing`() {
        val closed = idle().startRecognizing().hidePhotoWords()

        val result = reducer.testReduce(closed, Msg.TokensReady(listOf(token("perro")), requestId = 1L))

        assertFalse(result.state().photo().isOpen)
        result.assertNoEffects()
    }

    // ===== Ш10 → Ш11: ответ =====

    @Test
    fun `WFT-56_1 words of the answer are shown in answer order on the place of the indicator`() {
        val looking = idle().startRecognizing().startLooking()
        val answer = words(found("fuera", "fuera", "adverb"), found("corrían", "correr", "verb"))

        val result = reducer.testReduce(looking, Msg.WordsLookedUp(answer, requestId = 1L))

        assertEquals(listOf("fuera", "corrían"), result.state().photo().words.map { it.text })
        assertEquals(PhotoPhase.IDLE, result.state().photo().phase)
        assertNull(result.state().photo().errorRes)
        result.assertNoEffects()
    }

    @Test
    fun `WFT-56_7 found word carries the lemma and part of speech of the first candidate`() {
        val looking = idle().startRecognizing().startLooking()

        val result = reducer.testReduce(looking, Msg.WordsLookedUp(words(found("corrían", "correr", "verb")), requestId = 1L))

        val word = result.state().photo().words.single()
        assertEquals("correr", word.lemma)
        assertEquals("verb", word.partOfSpeech)
    }

    @Test
    fun `WFT-60_2 WFT-56_6 empty answer shows no words found`() {
        val looking = idle().startRecognizing().startLooking()

        val result = reducer.testReduce(looking, Msg.WordsLookedUp(words(), requestId = 1L))

        assertTrue(result.state().photo().isEmptyResult)
        assertTrue(result.state().photo().words.isEmpty())
        assertEquals(PhotoPhase.IDLE, result.state().photo().phase)
    }

    // ===== В3, В4: ошибка и отмена =====

    @Test
    fun `WFT-23_1 WFT-61_1 unavailable server shows server did not respond without the indicator`() {
        val looking = idle().startRecognizing().startLooking()

        val result = reducer.testReduce(looking, Msg.WordsLookedUp(PhotoLookupOutcome.Unavailable, requestId = 1L))

        assertEquals(R.string.photo_words_error_server, result.state().photo().errorRes)
        assertEquals(PhotoPhase.IDLE, result.state().photo().phase)
        assertTrue(result.state().photo().words.isEmpty())
    }

    @Test
    fun `WFT-61_2 WFT-62_2 answer of a cancelled run shows neither list nor error`() {
        val cancelled = idle().startRecognizing().startLooking().hidePhotoWords()

        val late = reducer.testReduce(cancelled, Msg.WordsLookedUp(words(found("perro", "perro", "noun")), requestId = 1L))
        val lateError = reducer.testReduce(cancelled, Msg.WordsLookedUp(PhotoLookupOutcome.Unavailable, requestId = 1L))

        assertEquals(cancelled, late.state())
        assertEquals(cancelled, lateError.state())
        late.assertNoEffects()
        lateError.assertNoEffects()
    }

    @Test
    fun `WFT-62_3 cancel then a new run keeps the first answer away from the chooser and from the second result`() {
        val cancelled = idle().startRecognizing().startLooking().hidePhotoWords()
        val chooser = reducer.testReduce(cancelled, Msg.OpenPhotoSourceChooser).state()

        val onChooser = reducer.testReduce(chooser, Msg.WordsLookedUp(words(found("first", "first", "noun")), requestId = 1L)).state()
        assertEquals(chooser, onChooser)

        val second = reducer.testReduce(onChooser, Msg.PhotoPicked(uri = "content://photo/2")).state()
        val secondLooking = reducer.testReduce(second, Msg.TokensReady(listOf(token("second")), second.photo().requestId)).state()
        val lateFirst = reducer.testReduce(secondLooking, Msg.WordsLookedUp(words(found("first", "first", "noun")), requestId = 1L)).state()

        assertEquals(secondLooking, lateFirst)
        assertEquals(PhotoPhase.LOOKING, lateFirst.photo().phase)
    }

    @Test
    fun `WFT-62_4 cancel then a new photo shows the second result`() {
        val cancelled = idle().startRecognizing().startLooking().hidePhotoWords()
        val second = reducer.testReduce(cancelled, Msg.PhotoPicked(uri = "content://photo/2")).state()
        val runId = second.photo().requestId
        val looking = reducer.testReduce(second, Msg.TokensReady(listOf(token("second")), runId)).state()

        val shown = reducer.testReduce(looking, Msg.WordsLookedUp(words(found("second", "second", "adjective")), runId)).state()

        assertEquals(listOf("second"), shown.photo().words.map { it.text })
    }

    // ===== В5: отказ =====

    @Test
    fun `WFT-63_1 WFT-19_1 unauthorized or missing key shows no access to the server`() {
        val looking = idle().startRecognizing().startLooking()

        val unauthorized = reducer.testReduce(looking, Msg.WordsLookedUp(PhotoLookupOutcome.Denied(PhotoDenyReason.UNAUTHORIZED), 1L))
        val noKey = reducer.testReduce(looking, Msg.WordsLookedUp(PhotoLookupOutcome.Denied(PhotoDenyReason.NO_ACCESS_KEY), 1L))

        assertEquals(R.string.photo_words_error_no_access, unauthorized.state().photo().errorRes)
        assertEquals(R.string.photo_words_error_no_access, noKey.state().photo().errorRes)
        assertTrue(unauthorized.state().photo().words.isEmpty())
    }

    @Test
    fun `WFT-63_2 rate limited shows daily limit reached`() {
        val looking = idle().startRecognizing().startLooking()

        val result = reducer.testReduce(looking, Msg.WordsLookedUp(PhotoLookupOutcome.Denied(PhotoDenyReason.RATE_LIMITED), 1L))

        assertEquals(R.string.photo_words_error_rate_limited, result.state().photo().errorRes)
    }

    @Test
    fun `WFT-63_3 request too large shows too many words`() {
        val looking = idle().startRecognizing().startLooking()

        val result = reducer.testReduce(looking, Msg.WordsLookedUp(PhotoLookupOutcome.Denied(PhotoDenyReason.REQUEST_TOO_LARGE), 1L))

        assertEquals(R.string.photo_words_error_too_many, result.state().photo().errorRes)
    }

    @Test
    fun `WFT-63_4 unsupported language shows dictionary language is not supported`() {
        val looking = idle().startRecognizing().startLooking()

        val result = reducer.testReduce(looking, Msg.WordsLookedUp(PhotoLookupOutcome.Denied(PhotoDenyReason.UNSUPPORTED_LANGUAGE), 1L))

        assertEquals(R.string.photo_words_error_language, result.state().photo().errorRes)
    }

    // ===== В7: перезапуск =====

    @Test
    fun `WFT-65_1 fresh state after process death shows neither indicator nor list`() {
        val fresh = PhotoWordsState()

        assertFalse(fresh.isOpen)
        assertFalse(fresh.isBusy)
        assertTrue(fresh.words.isEmpty())
        assertNull(fresh.errorRes)
    }

    @Test
    fun `closing the dialog keeps the run number for the next run`() {
        val shown = idle().startRecognizing().startLooking()

        val result = reducer.testReduce(shown, Msg.ClosePhotoWords)

        assertEquals(PhotoWordsState(requestId = 1L), result.state().photo())
        result.assertNoEffects()
    }

    // ===== обвязка случайных последовательностей =====

    private fun Msg.runIdOrNull(): Long? = when (this) {
        is Msg.TokensReady -> requestId
        is Msg.WordsLookedUp -> requestId
        is Msg.RecognitionFailed -> requestId
        else -> null
    }

    /** Случайные события с перемешанными номерами запуска вокруг текущего. */
    private fun randomMessages(random: Random, start: WordsTabState): List<Msg> {
        var runId = start.photoWordsState.requestId
        return List(random.nextInt(5, 30)) {
            val id = runId + random.nextInt(-2, 3)
            when (random.nextInt(8)) {
                0 -> Msg.OpenPhotoSourceChooser.also { runId++ }
                1 -> Msg.PhotoPicked(uri = "content://photo/$it").also { runId++ }
                2 -> Msg.PhotoPickCancelled
                3 -> Msg.TokensReady(listOf(token("w$it")), id)
                4 -> Msg.TokensReady(emptyList(), id)
                5 -> Msg.WordsLookedUp(words(found("w$it", "w$it", "noun")), id)
                6 -> Msg.RecognitionFailed(errorRes = 1, requestId = id)
                else -> Msg.ClosePhotoWords
            }
        }
    }
}
