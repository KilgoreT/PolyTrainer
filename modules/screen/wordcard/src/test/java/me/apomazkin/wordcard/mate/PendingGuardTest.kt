package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * §1.11 isGuardedByPending — guarded глотаются при pending, not-guarded проходят.
 */
class PendingGuardTest {

    private val reducer = WordCardReducer()

    private fun pendingWithSaved() = loaded(
        isPendingDbOp = true,
        availableTypes = listOf(ctype(50L, TR, isMultiple = true)),
        lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "x", isEdit = false)))),
    )

    // --- Guarded: state не меняется, эффектов нет ---

    @Test
    fun `CommitComponentValueEdit guarded`() {
        val initial = loaded(
            isPendingDbOp = true,
            lexemes = listOf(lexeme(7L, listOf(savedCv(5L, isEdit = true, edited = "new", origin = "old")))),
        )
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(7L, ComponentValueKey.Saved(me.apomazkin.lexeme.ComponentValueId(5L))))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `EnterComponentValueEditMode_IS_guarded_parity_with_EnterTranslationEditMode`() {
        val initial = pendingWithSaved()
        val result = reducer.testReduce(initial, Msg.EnterComponentValueEditMode(7L, ComponentValueKey.Saved(me.apomazkin.lexeme.ComponentValueId(5L))))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `RemoveComponentValueRequested guarded`() {
        val initial = pendingWithSaved()
        val result = reducer.testReduce(initial, Msg.RemoveComponentValueRequested(7L, ComponentValueKey.Saved(me.apomazkin.lexeme.ComponentValueId(5L))))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `RemoveLexeme guarded`() {
        val initial = loaded(isPendingDbOp = true, lexemes = listOf(lexeme(8L, emptyList())))
        val result = reducer.testReduce(initial, Msg.RemoveLexeme(8L))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `RemoveWord guarded`() {
        val initial = loaded(isPendingDbOp = true)
        val result = reducer.testReduce(initial, Msg.RemoveWord(7L))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `CommitWordChanges guarded`() {
        val initial = loaded(isPendingDbOp = true)
        val result = reducer.testReduce(initial, Msg.CommitWordChanges)
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `EnterWordEditMode guarded`() {
        val initial = loaded(isPendingDbOp = true)
        val result = reducer.testReduce(initial, Msg.EnterWordEditMode)
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `OpenTopBarMenu guarded`() {
        val initial = loaded(isPendingDbOp = true)
        val result = reducer.testReduce(initial, Msg.OpenTopBarMenu)
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `OpenDeleteWordDialog guarded`() {
        val initial = loaded(isPendingDbOp = true)
        val result = reducer.testReduce(initial, Msg.OpenDeleteWordDialog)
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `OpenDeleteLexemeDialog guarded`() {
        val initial = loaded(isPendingDbOp = true, lexemes = listOf(lexeme(8L, emptyList())))
        val result = reducer.testReduce(initial, Msg.OpenDeleteLexemeDialog(8L))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    // --- NOT guarded ---

    @Test
    fun `CreateComponentValue_NOT_guarded_deviation_from_CreateTranslation`() {
        val initial = loaded(
            isPendingDbOp = true,
            availableTypes = listOf(ctype(50L, TR, isMultiple = true)),
            lexemes = listOf(lexeme(7L, emptyList())),
        )
        val result = reducer.testReduce(initial, Msg.CreateComponentValue(7L, ComponentTypeId(50L)))
        assertNotEquals("pristine добавлен несмотря на pending", initial.lexemeList, result.state().lexemeList)
    }

    @Test
    fun `OperationFailed not guarded`() {
        val result = reducer.testReduce(loaded(isPendingDbOp = true), Msg.OperationFailed(R_STRING_X))
        assertEquals(false, result.state().isPendingDbOp)
    }

    @Test
    fun `NoOperation_is_pure_noop`() {
        val initial = loaded(isPendingDbOp = true, lexemes = listOf(lexeme(7L, listOf(savedCv(5L)))))
        val result = reducer.testReduce(initial, Msg.NoOperation)
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }
}
