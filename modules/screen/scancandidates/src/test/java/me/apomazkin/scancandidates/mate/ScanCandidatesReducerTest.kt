package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertEffects
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.LookupToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Запуск и фазы экрана глазами редьюсера: индикатор по фазам (WFP-13), «слов
 * нет» (WFP-19), ошибка (WFP-20), окно на месте индикатора (WFP-24, AWP-01,
 * AWP-24) и повтор запроса (WFP-26). Адреса строк спеки — `WFP-NN_k`, `AWP-NN_k`.
 */
class ScanCandidatesReducerTest {
    private val reducer = reducer()

    // ===== Индикатор по фазам =====

    @Test
    fun `WFP-13_1 the screen opens with the recognizing indicator and no error`() {
        val state = fresh()

        assertEquals(ScanPhase.RECOGNIZING, state.phase)
        assertEquals(Fx.URI, state.uri)
        assertNull(state.errorRes)
        assertFalse(state.hasRetryButton)
    }

    @Test
    fun `WFP-13_2 recognized tokens switch the indicator to looking and send the request`() {
        val result = reducer.testReduce(fresh(), Msg.TextRecognized(Fx.tokens))

        assertEquals(ScanPhase.LOOKING, result.state().phase)
        assertEquals(Fx.tokens, result.state().tokens)
        result.assertEffects(setOf<Effect>(DatasourceEffect.LookupWords(Fx.tokens)))
    }

    @Test
    fun `WFP-25_1 tokens go to the request as recognized with repeats and in order`() {
        val words = listOf(
            "Las", "nubes", "corrían", "por", "el", "cielo.",
            "El", "perro", "duerme", "en", "el", "jardín.",
        )
        val tokens = words.map { LookupToken(it, 0.9f, "es") }

        val result = reducer.testReduce(fresh(), Msg.TextRecognized(tokens))

        result.assertEffects(setOf<Effect>(DatasourceEffect.LookupWords(tokens)))
        assertEquals(words, result.state().tokens.map { it.text })
    }

    @Test
    fun `WFP-25_3 WFP-25_5 the same word twice keeps each confidence and the order is not fixed`() {
        val tokens = listOf(
            LookupToken("observarlo.", 0.8f, "es"),
            LookupToken("the", 0.4f, "en"),
            LookupToken("Nemec", 0.9f, null),
            LookupToken("the", 0.9f, "en"),
        )

        val result = reducer.testReduce(fresh(), Msg.TextRecognized(tokens))

        result.assertEffects(setOf<Effect>(DatasourceEffect.LookupWords(tokens)))
        assertEquals(listOf(0.8f, 0.4f, 0.9f, 0.9f), result.state().tokens.map { it.confidence })
    }

    @Test
    fun `AWP-01_1 the answer keeps the dictionary of the photo and asks its form`() {
        val looking = reducer.recognized()

        val result = reducer.testReduce(
            looking,
            Msg.WordsFound(Fx.DICT, listOf(Fx.ran, Fx.running), listOf(Fx.genderRecommendation)),
        )

        assertEquals(ScanPhase.FORM_LOADING, result.state().phase)
        assertEquals(Fx.DICT, result.state().dictionaryId)
        assertEquals(listOf("ran", "running"), result.state().words.map { it.text })
        assertEquals(listOf(Fx.genderRecommendation), result.state().recommendations)
        result.assertEffects(
            setOf<Effect>(DatasourceEffect.LoadForm(Fx.DICT, listOf("run", "running"))),
        )
    }

    @Test
    fun `AWP-01_1 lemma texts of the form request go without repeats`() {
        val looking = reducer.recognized()

        val effects = reducer.effectsOf(
            looking,
            Msg.WordsFound(Fx.DICT, listOf(Fx.ran, Fx.runs, Fx.bank), emptyList()),
        )

        assertEquals(
            setOf<Effect>(DatasourceEffect.LoadForm(Fx.DICT, listOf("run", "bank"))),
            effects,
        )
    }

    // ===== Слов не найдено =====

    @Test
    fun `WFP-19_1 no text on the photo shows no words found and sends no request`() {
        val result = reducer.testReduce(fresh(), Msg.TextEmpty)

        assertEquals(ScanPhase.NO_WORDS, result.state().phase)
        assertNull(result.state().errorRes)
        result.assertNoEffects()
    }

    @Test
    fun `WFP-19_2 WFP-24_2 an empty answer shows no words found and no window`() {
        val looking = reducer.recognized()

        val result = reducer.testReduce(looking, Msg.WordsEmpty)

        assertEquals(ScanPhase.NO_WORDS, result.state().phase)
        assertTrue(result.state().cards.isEmpty())
        assertNull(result.state().form)
        result.assertNoEffects()
    }

    // ===== Ошибки =====

    @Test
    fun `WFP-20_1 WFP-26_4 an unread photo shows the error text with close only`() {
        val result = reducer.testReduce(
            fresh(),
            Msg.RecognitionFailed(R.string.photo_words_error_recognition),
        )

        assertEquals(ScanPhase.ERROR, result.state().phase)
        assertEquals(R.string.photo_words_error_recognition, result.state().errorRes)
        assertFalse(result.state().hasRetryButton)
        result.assertNoEffects()
    }

    @Test
    fun `WFP-20_1 WFP-26_3 a denial by key or limit shows the error text with close only`() {
        val looking = reducer.recognized()

        val noAccess = reducer.step(looking, Msg.LookupDenied(R.string.photo_words_error_no_access))
        val limit = reducer.step(looking, Msg.LookupDenied(R.string.photo_words_error_rate_limited))

        assertEquals(ScanPhase.ERROR, noAccess.phase)
        assertEquals(R.string.photo_words_error_no_access, noAccess.errorRes)
        assertFalse(noAccess.hasRetryButton)
        assertEquals(R.string.photo_words_error_rate_limited, limit.errorRes)
        assertFalse(limit.hasRetryButton)
    }

    @Test
    fun `AWP-24_1 AWP-24_4 AWP-24_5 a failed form or a dead dictionary shows the error only`() {
        val loading = reducer.answered(Fx.ran)

        val failed = reducer.step(loading, Msg.FormFailed(R.string.photo_add_form_failed))
        val missing = reducer.step(loading, Msg.FormFailed(R.string.photo_add_dictionary_missing))

        assertEquals(ScanPhase.ERROR, failed.phase)
        assertEquals(R.string.photo_add_form_failed, failed.errorRes)
        assertEquals(R.string.photo_add_dictionary_missing, missing.errorRes)
        assertFalse(failed.hasRetryButton)
        assertTrue(failed.cards.isEmpty())
        assertNull(failed.form)
    }

    // ===== Окно на месте индикатора =====

    @Test
    fun `WFP-24_1 an answer with a found word opens the window with its card`() {
        val loading = reducer.answered(Fx.perro)

        val window = reducer.step(loading, Msg.FormLoaded(Fx.form()))

        assertEquals(ScanPhase.FORM_LOADING, loading.phase)
        assertEquals(ScanPhase.WINDOW, window.phase)
        assertEquals(listOf("perro"), window.cards.map { it.lemma })
        assertEquals(Fx.form(), window.form)
    }

    @Test
    fun `WFP-24_4 AWP-12_3 all words not found open the window with the not added block only`() {
        val window = reducer.window(Fx.accorc, Fx.notFound("qwrt"))

        assertEquals(ScanPhase.WINDOW, window.phase)
        assertTrue(window.cards.isEmpty())
        assertEquals(listOf("accorc", "qwrt"), window.notAdded.map { it.text })
        assertTrue(window.isNotAddedExpanded)
        assertFalse(window.summary.isEnabled)
    }

    @Test
    fun `WFP-24_5 WFP-26_2 a server unavailable answer shows no window, offers a retry`() {
        val looking = reducer.recognized()

        val result = reducer.testReduce(
            looking,
            Msg.LookupUnavailable(R.string.scan_server_unavailable),
        )

        assertEquals(ScanPhase.ERROR, result.state().phase)
        assertEquals(R.string.scan_server_unavailable, result.state().errorRes)
        assertTrue(result.state().hasRetryButton)
        assertTrue(result.state().cards.isEmpty())
        assertNull(result.state().form)
        result.assertNoEffects()
    }

    // ===== Повтор запроса =====

    @Test
    fun `WFP-26_1 no network shows close and retry, retry looks again with the same tokens`() {
        val unavailable = reducer.step(
            reducer.recognized(),
            Msg.LookupUnavailable(R.string.photo_words_error_server),
        )

        val retried = reducer.testReduce(unavailable, Msg.RetryLookup)

        assertEquals(ScanPhase.ERROR, unavailable.phase)
        assertTrue(unavailable.hasRetryButton)
        assertEquals(ScanPhase.LOOKING, retried.state().phase)
        assertNull(retried.state().errorRes)
        assertFalse(retried.state().hasRetryButton)
        retried.assertEffects(setOf<Effect>(DatasourceEffect.LookupWords(Fx.tokens)))
    }

    @Test
    fun `WFP-26_1 a retry after a retry sends the very same tokens again`() {
        val unavailable = reducer.step(
            reducer.recognized(),
            Msg.LookupUnavailable(R.string.photo_words_error_server),
        )
        val retried = reducer.step(unavailable, Msg.RetryLookup)
        val failedAgain = reducer.step(
            retried,
            Msg.LookupUnavailable(R.string.scan_server_unavailable),
        )

        val effects = reducer.effectsOf(failedAgain, Msg.RetryLookup)

        assertEquals(setOf<Effect>(DatasourceEffect.LookupWords(Fx.tokens)), effects)
    }

    // ===== Прочее =====

    @Test
    fun `an action of the card dialog that the screen does not use changes nothing`() {
        val window = reducer.window(Fx.ran)

        val result = reducer.testReduce(window, Msg.NoOperation)

        assertEquals(window, result.state())
        result.assertNoEffects()
    }
}
