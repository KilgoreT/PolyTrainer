package me.apomazkin.scancandidates.mate

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertEffects
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import me.apomazkin.candidates.AddFailureUi
import me.apomazkin.candidates.CandidatesSheetUi
import me.apomazkin.core_resources.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Закрытие глазами редьюсера: сразу, когда недобавленных слов с
 * лексемами-кандидатами нет (AWP-60), с вопросом и числом таких слов (AWP-51),
 * пока виден индикатор (AWP-24), из «слов нет» и ошибки (AWP-54); во время
 * записи крестик и «назад» не действуют (AWP-51.12). Адреса строк спеки —
 * `AWP-NN_k`.
 */
class CloseReducerTest {
    private val reducer = reducer()

    private val back = setOf<Effect>(NavigationEffect.Back)
    private val two = Fx.lists(lexical = listOf("ran", "bank"))

    /** Загружены карточки «ran» и «bank». */
    private fun twoLoaded(): ScanCandidatesState = reducer.steps(
        reducer.withCard("ran", Fx.ran, lists = two),
        Msg.LoadLexemes("bank"),
        Fx.lexemes("bank", Fx.bank),
    )

    private fun savedRun(): ScanCandidatesState = reducer.step(
        reducer.windowOf("run", form = Fx.form(existing = listOf(Fx.runExisting))),
        Msg.OpenSavedCard("run"),
    )

    // ===== Без вопроса (AWP-60) =====

    @Test
    fun `AWP-60_1 closing the window with no word loaded leaves at once whatever was chosen`() {
        val touched = reducer.steps(
            reducer.windowOf("ran"),
            Msg.SetCommonGroup(Fx.POTTER),
            Msg.ToggleTranslation,
            Msg.OpenSheet(CandidatesSheetUi.CommonGroup),
        )

        val result = reducer.testReduce(touched, Msg.RequestClose)

        assertEquals(touched, result.state())
        assertFalse(result.state().isConfirmingClose)
        result.assertEffects(back)
    }

    @Test
    fun `AWP-60_2 all loaded words added leaves at once`() {
        val added = reducer.steps(
            twoLoaded(),
            Msg.AddWord("ran"),
            Msg.WordAdded("ran"),
            Msg.AddWord("bank"),
            Msg.WordAdded("bank"),
        )

        val result = reducer.testReduce(added, Msg.RequestClose)

        assertFalse(added.isAdding)
        result.assertEffects(back)
    }

    // ===== Вопрос при закрытии (AWP-51) =====

    @Test
    fun `AWP-51_1 AWP-14_2 one of two loaded words added asks the question with the count of the other`() {
        val oneAdded = reducer.steps(twoLoaded(), Msg.AddWord("ran"), Msg.WordAdded("ran"))

        val result = reducer.testReduce(oneAdded, Msg.RequestClose)

        assertTrue(result.state().isConfirmingClose)
        assertEquals(1, result.state().pendingWords)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-51_3 stay closes the question and keeps the window as it was`() {
        val loaded = twoLoaded()
        val asking = reducer.step(loaded, Msg.RequestClose)

        val result = reducer.testReduce(asking, Msg.Stay)

        assertTrue(asking.isConfirmingClose)
        assertEquals(2, asking.pendingWords)
        assertEquals(loaded, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `AWP-51_4 close in the question leaves without a write`() {
        val asking = reducer.step(twoLoaded(), Msg.RequestClose)

        val result = reducer.testReduce(asking, Msg.ConfirmClose)

        result.assertEffects(back)
    }

    @Test
    fun `AWP-51_5 a loaded word deleted is not counted`() {
        val oneDeleted = reducer.step(twoLoaded(), Msg.DeleteWord("bank"))
        val bothDeleted = reducer.step(oneDeleted, Msg.DeleteWord("ran"))

        val one = reducer.testReduce(oneDeleted, Msg.RequestClose)
        val none = reducer.testReduce(bothDeleted, Msg.RequestClose)

        assertEquals(1, one.state().pendingWords)
        none.assertEffects(back)
    }

    @Test
    fun `AWP-51_7 a word in the dictionary opened without a check is not counted`() {
        val result = reducer.testReduce(savedRun(), Msg.RequestClose)

        result.assertEffects(back)
    }

    @Test
    fun `AWP-51_8 not found or an error on the word is not counted`() {
        val loading = reducer.step(reducer.windowOf("accorc"), Msg.LoadLexemes("accorc"))
        val notFound = reducer.step(loading, Msg.LexemesNotFound("accorc", R.string.scan_not_found))
        val error = reducer.step(loading, Msg.LexemesUnavailable("accorc", R.string.photo_words_error_server))

        reducer.testReduce(notFound, Msg.RequestClose).assertEffects(back)
        reducer.testReduce(error, Msg.RequestClose).assertEffects(back)
    }

    @Test
    fun `AWP-51_9 a word still loading is counted`() {
        val loading = reducer.step(reducer.windowOf("sibling"), Msg.LoadLexemes("sibling"))

        val result = reducer.testReduce(loading, Msg.RequestClose)

        assertTrue(result.state().isConfirmingClose)
        assertEquals(1, result.state().pendingWords)
        result.assertNoEffects()
    }

    @Test
    fun `AWP-51_10 a checked word with a new lexeme is counted`() {
        val checked = reducer.steps(savedRun(), Msg.LoadLexemes("run"), Fx.lexemes("run", Fx.run))

        val result = reducer.testReduce(checked, Msg.RequestClose)

        assertTrue(result.state().isConfirmingClose)
        assertEquals(1, result.state().pendingWords)
    }

    @Test
    fun `AWP-51_11 a checked word whose lexemes are all in the dictionary is not counted`() {
        val run = Fx.existing(Fx.RUN_WORD, "run", listOf("бежать, бегать", "управлять"))
        val saved = reducer.step(reducer.windowOf("run", form = Fx.form(existing = listOf(run))), Msg.OpenSavedCard("run"))
        val checked = reducer.steps(saved, Msg.LoadLexemes("run"), Fx.lexemes("run", Fx.run))

        val result = reducer.testReduce(checked, Msg.RequestClose)

        assertTrue(checked.lemmaCard("run").lexemes.all { it.exists })
        result.assertEffects(back)
    }

    @Test
    fun `AWP-51_12 while a word is being written the close request does nothing, after a failure it asks`() {
        val adding = reducer.step(reducer.withCard("ran", Fx.ran), Msg.AddWord("ran"))
        val failed = reducer.step(adding, Msg.AddFailed("ran", AddFailureUi.Failed))

        val whileAdding = reducer.testReduce(adding, Msg.RequestClose)
        val afterFailure = reducer.testReduce(failed, Msg.RequestClose)

        assertEquals(adding, whileAdding.state())
        whileAdding.assertNoEffects()
        assertTrue(afterFailure.state().isConfirmingClose)
        assertEquals(1, afterFailure.state().pendingWords)
    }

    // ===== Пока виден индикатор и без окна (AWP-24, AWP-54) =====

    @Test
    fun `AWP-24_3 closing while the indicator is visible leaves at once`() {
        val states = listOf(fresh(), reducer.recognized(), reducer.answered())

        states.forEach { state ->
            val result = reducer.testReduce(state, Msg.RequestClose)

            assertEquals(state, result.state())
            result.assertEffects(back)
        }
    }

    @Test
    fun `AWP-54_2 close from no words leaves to the tab, close from an error too`() {
        val noWords = reducer.step(fresh(), Msg.TextEmpty)
        val failure = Msg.RecognitionFailed(R.string.photo_words_error_recognition)
        val error = reducer.step(fresh(), failure)

        reducer.testReduce(noWords, Msg.RequestClose).assertEffects(back)
        reducer.testReduce(error, Msg.RequestClose).assertEffects(back)
    }
}
