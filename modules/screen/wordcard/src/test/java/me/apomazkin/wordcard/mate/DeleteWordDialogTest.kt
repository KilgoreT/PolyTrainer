package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Reducer tests for delete-word dialog and RemoveWord under sealed WordState.
 *
 * Test cases:
 * - OpenDeleteWordDialog guard: wordState !is Loaded → state unchanged.
 * - OpenDeleteWordDialog on Loaded → showWarningDialog=true.
 * - CloseDeleteWordDialog → showWarningDialog=false.
 * - RemoveWord guard: wordState !is Loaded ∨ id mismatch → state unchanged.
 * - RemoveWord on matching Loaded → Effect RemoveWord, isPendingDbOp=true.
 * - Global guard isPendingDbOp on RemoveWord.
 */
class DeleteWordDialogTest {

    private fun loaded(
        id: Long = 1L,
        showWarningDialog: Boolean = false,
        isPendingDbOp: Boolean = false,
    ): WordCardState = WordCardState(
        isLoading = false,
        isPendingDbOp = isPendingDbOp,
        wordState = WordState.Loaded(
            id = id,
            added = Date(0L),
            value = "w",
            showWarningDialog = showWarningDialog,
        ),
    )

    @Test
    fun `given NotLoaded when OpenDeleteWordDialog then state unchanged`() {
        val reducer = WordCardReducer()
        val initial = WordCardState(isLoading = false, wordState = WordState.NotLoaded)

        val result = reducer.testReduce(initial, Msg.OpenDeleteWordDialog)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `given Loaded when OpenDeleteWordDialog then showWarningDialog true`() {
        val reducer = WordCardReducer()
        val initial = loaded()

        val result = reducer.testReduce(initial, Msg.OpenDeleteWordDialog)

        val loaded = result.state().wordState as WordState.Loaded
        assertTrue(loaded.showWarningDialog)
        result.assertNoEffects()
    }

    @Test
    fun `when CloseDeleteWordDialog then showWarningDialog false`() {
        val reducer = WordCardReducer()
        val initial = loaded(showWarningDialog = true)

        val result = reducer.testReduce(initial, Msg.CloseDeleteWordDialog)

        val loaded = result.state().wordState as WordState.Loaded
        assertFalse(loaded.showWarningDialog)
        result.assertNoEffects()
    }

    @Test
    fun `given NotLoaded when RemoveWord then state unchanged`() {
        val reducer = WordCardReducer()
        val initial = WordCardState(isLoading = false, wordState = WordState.NotLoaded)

        val result = reducer.testReduce(initial, Msg.RemoveWord(wordId = 99L))

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `given Loaded with mismatching id when RemoveWord then state unchanged`() {
        val reducer = WordCardReducer()
        val initial = loaded(id = 1L)

        val result = reducer.testReduce(initial, Msg.RemoveWord(wordId = 99L))

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `given Loaded with matching id when RemoveWord then emits RemoveWord effect and pending`() {
        val reducer = WordCardReducer()
        val initial = loaded(id = 7L, showWarningDialog = true)

        val result = reducer.testReduce(initial, Msg.RemoveWord(wordId = 7L))

        assertTrue(result.state().isPendingDbOp)
        result.assertEffects(setOf(DatasourceEffect.RemoveWord(wordId = 7L)))
    }

    @Test
    fun `given isPendingDbOp true when RemoveWord then state unchanged (global guard)`() {
        val reducer = WordCardReducer()
        val initial = loaded(id = 7L, isPendingDbOp = true)

        val result = reducer.testReduce(initial, Msg.RemoveWord(wordId = 7L))

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }
}
