package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertSingleEffect
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §1.10 OperationFailed (F7 блокер + A10 isCommitting + flush isExiting).
 */
class OperationFailedTest {

    private val reducer = WordCardReducer()

    @Test
    fun `F7_clears_pending_and_emits_error`() {
        val result = reducer.testReduce(loaded(isPendingDbOp = true), Msg.OperationFailed(R_STRING_X))
        assertFalse(result.state().isPendingDbOp)
        result.assertSingleEffect<UiEffect.ShowErrorSnackbar>()
    }

    @Test
    fun `F7_does_not_touch_pristine_or_edit`() {
        val initial = loaded(
            isPendingDbOp = true,
            lexemes = listOf(
                lexeme(1L, listOf(pristineCv(10L, edited = "draft"), savedCv(5L, isEdit = true, edited = "x"))),
            ),
        )
        val result = reducer.testReduce(initial, Msg.OperationFailed(R_STRING_X))
        assertEquals("изменён только isPendingDbOp", initial.lexemeList, result.state().lexemeList)
        assertFalse(result.state().isPendingDbOp)
    }

    @Test
    fun `F7_messageRes_passed_through`() {
        val result = reducer.testReduce(loaded(isPendingDbOp = true), Msg.OperationFailed(R_STRING_X))
        result.assertEffects(setOf(UiEffect.ShowErrorSnackbar(R_STRING_X)))
    }

    @Test
    fun `F7_not_guarded_runs_while_pending`() {
        val result = reducer.testReduce(loaded(isPendingDbOp = true), Msg.OperationFailed(R_STRING_X))
        assertFalse(result.state().isPendingDbOp)
    }

    @Test
    fun `F7_clears_isCommitting_keeps_edit`() {
        val initial = loaded(
            isPendingDbOp = true,
            lexemes = listOf(
                lexeme(1L, listOf(savedCv(5L, isEdit = true, isCommitting = true, origin = "old", edited = "new"))),
            ),
        )
        val result = reducer.testReduce(initial, Msg.OperationFailed(R_STRING_X))
        val cv = result.state().lexemeList.single().components.single()
        assertFalse("isCommitting снят", cv.isCommitting)
        assertTrue("isEdit цел", cv.isEdit)
        assertEquals("edited цел", "new", cv.edited)
        assertFalse(result.state().isPendingDbOp)
    }
}
