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
 * Запуск и фазы экрана глазами редьюсера: индикатор по фазам (AWP-38), «слов
 * нет» (TFP-19, AWP-54), ошибка (AWP-24, AWP-53), окно на месте индикатора
 * (AWP-01, AWP-40, AWP-41), пометка «в словаре» (AWP-39) и повтор запроса
 * (TFP-26). Адреса строк спеки — `TFP-NN_k`, `AWP-NN_k`.
 */
class ScanCandidatesReducerTest {
    private val reducer = reducer()

    // ===== Индикатор по фазам =====

    @Test
    fun `AWP-38_1 the screen opens with the recognizing indicator and no error`() {
        val state = fresh()

        assertEquals(ScanPhase.RECOGNIZING, state.phase)
        assertEquals(Fx.URI, state.uri)
        assertNull(state.errorRes)
        assertFalse(state.hasRetryButton)
    }

    @Test
    fun `AWP-38_2 recognized tokens switch the indicator to analyzing and send the request`() {
        val result = reducer.testReduce(fresh(), Msg.TextRecognized(Fx.tokens))

        assertEquals(ScanPhase.ANALYZING, result.state().phase)
        assertEquals(Fx.tokens, result.state().tokens)
        result.assertEffects(setOf<Effect>(DatasourceEffect.ExtractLemmas(Fx.tokens)))
    }

    @Test
    fun `TFP-25_1 tokens go to the request as recognized with repeats and in order`() {
        val words = listOf(
            "Las", "nubes", "corrían", "por", "el", "cielo.",
            "El", "perro", "duerme", "en", "el", "jardín.",
        )
        val tokens = words.map { LookupToken(it, 0.9f, "es") }

        val result = reducer.testReduce(fresh(), Msg.TextRecognized(tokens))

        result.assertEffects(setOf<Effect>(DatasourceEffect.ExtractLemmas(tokens)))
        assertEquals(words, result.state().tokens.map { it.text })
    }

    // ===== Ответ сервера =====

    @Test
    fun `AWP-01_1 the answer keeps the dictionary of the photo and asks its form with the texts of all lists`() {
        val analyzing = reducer.recognized()

        val result = reducer.testReduce(analyzing, Fx.found())

        assertEquals(ScanPhase.FORM_LOADING, result.state().phase)
        assertEquals(Fx.DICT, result.state().dictionaryId)
        assertEquals(Fx.TEXTBOOK_TEXT, result.state().text)
        assertEquals(Fx.TEXTBOOK_TRANSLATION, result.state().translation)
        assertEquals(Fx.textbookLists(), result.state().lists)
        val texts = Fx.textbookLexical + Fx.textbookProper + Fx.textbookFunction
        result.assertEffects(setOf<Effect>(DatasourceEffect.LoadForm(Fx.DICT, texts)))
    }

    @Test
    fun `AWP-01_1 lemma texts of the form request go without repeats across the lists`() {
        val lists = Fx.lists(lexical = listOf("have", "run"), function = listOf("have", "a", "a"))

        val effects = reducer.effectsOf(reducer.recognized(), Fx.found(lists = lists))

        assertEquals(
            setOf<Effect>(DatasourceEffect.LoadForm(Fx.DICT, listOf("have", "run", "a"))),
            effects,
        )
    }

    // ===== Слов не найдено =====

    @Test
    fun `TFP-19_1 AWP-54_1 no text on the photo shows no words found and sends no request`() {
        val result = reducer.testReduce(fresh(), Msg.TextEmpty)

        assertEquals(ScanPhase.NO_WORDS, result.state().phase)
        assertNull(result.state().errorRes)
        result.assertNoEffects()
    }

    @Test
    fun `TFP-19_2 an answer without words shows no words found and no window`() {
        val analyzing = reducer.recognized()

        val result = reducer.testReduce(analyzing, Msg.LemmasEmpty)

        assertEquals(ScanPhase.NO_WORDS, result.state().phase)
        assertTrue(result.state().lists.allTexts().isEmpty())
        assertNull(result.state().form)
        result.assertNoEffects()
    }

    // ===== Ошибки =====

    @Test
    fun `TFP-26_4 AWP-53_6 an unread photo shows the error text with close only`() {
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
    fun `TFP-26_3 a denial by key or limit shows the error text with close only`() {
        val analyzing = reducer.recognized()

        val noAccessMsg = Msg.LemmasDenied(R.string.photo_words_error_no_access)
        val limitMsg = Msg.LemmasDenied(R.string.photo_words_error_rate_limited)

        val noAccess = reducer.step(analyzing, noAccessMsg)
        val limit = reducer.step(analyzing, limitMsg)

        assertEquals(ScanPhase.ERROR, noAccess.phase)
        assertEquals(R.string.photo_words_error_no_access, noAccess.errorRes)
        assertFalse(noAccess.hasRetryButton)
        assertEquals(R.string.photo_words_error_rate_limited, limit.errorRes)
        assertFalse(limit.hasRetryButton)
    }

    @Test
    fun `AWP-24_1 AWP-24_4 AWP-24_5 a failed form or a dead dictionary shows the error only`() {
        val loading = reducer.answered()

        val failed = reducer.step(loading, Msg.FormFailed(R.string.photo_add_form_failed))
        val missing = reducer.step(loading, Msg.FormFailed(R.string.photo_add_dictionary_missing))

        assertEquals(ScanPhase.ERROR, failed.phase)
        assertEquals(R.string.photo_add_form_failed, failed.errorRes)
        assertEquals(R.string.photo_add_dictionary_missing, missing.errorRes)
        assertFalse(failed.hasRetryButton)
        assertNull(failed.form)
    }

    @Test
    fun `TFP-26_2 a model that did not answer shows the analysis unavailable text with a retry`() {
        val analyzing = reducer.recognized()

        val result = reducer.testReduce(
            analyzing,
            Msg.LemmasUnavailable(R.string.scan_analysis_unavailable),
        )

        assertEquals(ScanPhase.ERROR, result.state().phase)
        assertEquals(R.string.scan_analysis_unavailable, result.state().errorRes)
        assertTrue(result.state().hasRetryButton)
        assertNull(result.state().form)
        result.assertNoEffects()
    }

    // ===== Окно на месте индикатора =====

    @Test
    fun `AWP-40_1 the textbook paragraph opens the window with the lists in text order and no error`() {
        val loading = reducer.answered()

        val window = reducer.step(loading, Msg.FormLoaded(Fx.form()))

        assertEquals(ScanPhase.FORM_LOADING, loading.phase)
        assertEquals(ScanPhase.WINDOW, window.phase)
        assertEquals(Fx.textbookLexical, window.lists.lexical.texts())
        assertEquals(Fx.textbookProper, window.lists.proper.texts())
        assertEquals(Fx.textbookFunction, window.lists.function.texts())
        assertTrue(window.lists.unparsed.isEmpty())
        assertEquals(Fx.form(), window.form)
        assertNull(window.errorRes)
        assertFalse(window.hasRetryButton)
    }

    @Test
    fun `AWP-40_2 the dense page keeps the unparsed list as sent, after the other lists`() {
        val lists = Fx.lists(lexical = listOf("gran", "maestra"), unparsed = Fx.denseUnparsed)

        val window = reducer.window(found = Fx.found(lists = lists))

        assertEquals(ScanPhase.WINDOW, window.phase)
        assertEquals(listOf("gran", "maestra"), window.lists.lexical.texts())
        assertEquals(Fx.denseUnparsed, window.lists.unparsed.texts())
    }

    @Test
    fun `AWP-40_6 three empty lists and sh and e in unparsed still open the window`() {
        val lists = Fx.lists(unparsed = listOf("sh", "e"))

        val window = reducer.window(found = Fx.found(lists = lists))

        assertEquals(ScanPhase.WINDOW, window.phase)
        assertTrue(window.lists.lexical.isEmpty() && window.lists.proper.isEmpty())
        assertTrue(window.lists.function.isEmpty())
        assertEquals(listOf("sh", "e"), window.lists.unparsed.texts())
        assertNull(window.commonGroupId)
    }

    @Test
    fun `AWP-41_1 the translation of the answer is in the window`() {
        val found = Fx.found(
            lists = Fx.lists(
                lexical = listOf("nube", "correr", "cielo"),
                function = listOf("el", "por"),
            ),
            text = "Las nubes corrían por el cielo.",
            translation = "Облака бежали по небу.",
        )

        val window = reducer.window(found = found)

        assertEquals("Las nubes corrían por el cielo.", window.text)
        assertEquals("Облака бежали по небу.", window.translation)
    }

    @Test
    fun `AWP-41_2 the window opens with the translation folded, a tap expands it and the next folds it`() {
        val window = reducer.window()

        val expanded = reducer.testReduce(window, Msg.ToggleTranslation)
        val folded = reducer.step(expanded.state(), Msg.ToggleTranslation)

        assertFalse(window.isTranslationExpanded)
        assertTrue(expanded.state().isTranslationExpanded)
        assertFalse(folded.isTranslationExpanded)
        expanded.assertNoEffects()
    }

    // ===== Пометка «в словаре» =====

    @Test
    fun `AWP-39_1 a lemma study in the dictionary marks study in lexical and nothing else`() {
        val window = reducer.window(form = Fx.form(existing = listOf(Fx.study1)))

        assertEquals(listOf("study"), window.lists.lexical.marked())
        assertTrue(window.lists.proper.marked().isEmpty())
        assertTrue(window.lists.function.marked().isEmpty())
    }

    @Test
    fun `AWP-39_2 the mark ignores case - Study in the dictionary marks study`() {
        val study = Fx.existing(Fx.STUDY_WORD, "Study")

        val window = reducer.window(form = Fx.form(existing = listOf(study)))

        assertEquals(listOf("study"), window.lists.lexical.marked())
    }

    @Test
    fun `AWP-39_3 no lemma with the text - no mark anywhere`() {
        val window = reducer.window(form = Fx.form(existing = listOf(Fx.run1)))

        assertEquals(0, window.lists.countInDictionary())
    }

    @Test
    fun `AWP-39_4 ёж in the dictionary does not mark еж`() {
        val lists = Fx.lists(lexical = listOf("еж"))

        val window = reducer.window(
            found = Fx.found(lists = lists),
            form = Fx.form(existing = listOf(Fx.existing(5L, "ёж"))),
        )

        assertEquals(0, window.lists.countInDictionary())
    }

    // ===== Повтор запроса =====

    @Test
    fun `TFP-26_1 no network shows close and retry, retry analyzes again with the same tokens`() {
        val unavailable = reducer.step(
            reducer.recognized(),
            Msg.LemmasUnavailable(R.string.photo_words_error_server),
        )

        val retried = reducer.testReduce(unavailable, Msg.RetryLemmas)

        assertEquals(ScanPhase.ERROR, unavailable.phase)
        assertTrue(unavailable.hasRetryButton)
        assertEquals(ScanPhase.ANALYZING, retried.state().phase)
        assertNull(retried.state().errorRes)
        assertFalse(retried.state().hasRetryButton)
        retried.assertEffects(setOf<Effect>(DatasourceEffect.ExtractLemmas(Fx.tokens)))
    }

    @Test
    fun `TFP-26_1 a retry after a retry sends the very same tokens again`() {
        val unavailable = reducer.step(
            reducer.recognized(),
            Msg.LemmasUnavailable(R.string.photo_words_error_server),
        )
        val retried = reducer.step(unavailable, Msg.RetryLemmas)
        val failedAgain = reducer.step(
            retried,
            Msg.LemmasUnavailable(R.string.scan_analysis_unavailable),
        )

        val effects = reducer.effectsOf(failedAgain, Msg.RetryLemmas)

        assertEquals(setOf<Effect>(DatasourceEffect.ExtractLemmas(Fx.tokens)), effects)
    }

    // ===== Прочее =====

    @Test
    fun `an action of the shared widgets that the screen does not use changes nothing`() {
        val window = reducer.window()

        val result = reducer.testReduce(window, Msg.NoOperation)

        assertEquals(window, result.state())
        result.assertNoEffects()
    }
}
