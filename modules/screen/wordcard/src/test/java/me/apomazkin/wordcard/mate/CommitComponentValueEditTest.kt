package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §1.4 CommitComponentValueEdit — матрица commitDecision 4×(pristine/saved)×(NOT_IN_DB/real).
 */
class CommitComponentValueEditTest {

    private val reducer = WordCardReducer()
    private fun savedKey(id: Long) = ComponentValueKey.Saved(ComponentValueId(id))
    private fun pKey(k: Long) = ComponentValueKey.Pristine(k)

    @Test
    fun `1 NoOp not editing`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, isEdit = false)))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(7L, savedKey(5L)))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `2 NoOp edited equals origin closes edit no pending`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "same", isEdit = true, edited = "same")))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(7L, savedKey(5L)))
        val cv = result.state().lexemeList.single().components.single()
        assertFalse(cv.isEdit)
        assertEquals("", cv.edited)
        assertEquals("same", cv.origin)
        assertFalse(result.state().isPendingDbOp)
        result.assertNoEffects()
    }

    @Test
    fun `3 NoOp trimmed equals origin`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "same", isEdit = true, edited = "  same  ")))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(7L, savedKey(5L)))
        assertFalse(result.state().lexemeList.single().components.single().isEdit)
        result.assertNoEffects()
    }

    @Test
    fun `4 LocalRemove pristine empty stays pending false`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(pristineCv(1L, edited = "   "), savedCv(5L)))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(7L, pKey(1L)))
        assertTrue("pristine удалён", result.state().lexemeList.single().components.none { it.isPristine })
        assertFalse(result.state().isPendingDbOp)
        result.assertNoEffects()
    }

    @Test
    fun `5 LocalRemove cascades empty NOT_IN_DB`() {
        val initial = loaded(lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(1L, edited = "")))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(NOT_IN_DB, pKey(1L)))
        assertTrue(result.state().lexemeList.none { it.id == NOT_IN_DB && it.components.isEmpty() })
        assertFalse(result.state().isCreatingLexeme)
        assertFalse(result.state().isPendingDbOp)
        result.assertNoEffects()
    }

    @Test
    fun `6 PessimisticRemove empty edit nonempty origin`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "x", isEdit = true, edited = "")))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(7L, savedKey(5L)))
        val cv = result.state().lexemeList.single().components.single()
        assertTrue(result.state().isPendingDbOp)
        assertTrue(cv.isCommitting)
        result.assertEffects(setOf(DatasourceEffect.RemoveComponentValue(ComponentValueId(5L), 7L)))
    }

    @Test
    fun `7 Update saved holds edit sets committing`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "old", isEdit = true, edited = "new")))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(7L, savedKey(5L)))
        val cv = result.state().lexemeList.single().components.single()
        assertTrue(result.state().isPendingDbOp)
        assertTrue("A10 edit держится", cv.isEdit)
        assertTrue(cv.isCommitting)
        assertEquals("new", cv.edited)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.UpdateValue(
                    wordId = 7L, dictionaryId = 3L, lexemeId = 7L,
                    componentValueId = ComponentValueId(5L),
                    componentTypeId = ComponentTypeId(50L), componentTypeRef = TR,
                    data = textValuesOf("new"),
                ),
            ),
        )
    }

    @Test
    fun `8 Update pristine real lexeme emits AddValue`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(pristineCv(1L, edited = "new")))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(7L, pKey(1L)))
        assertTrue(result.state().isPendingDbOp)
        assertTrue("pristine остаётся до Inserted-flip", result.state().lexemeList.single().components.any { it.pristineKey == 1L })
        result.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.AddValue(
                    wordId = 7L, dictionaryId = 3L, lexemeId = 7L, pristineKey = 1L,
                    componentTypeId = ComponentTypeId(50L), componentTypeRef = TR,
                    data = textValuesOf("new"),
                ),
            ),
        )
    }

    @Test
    fun `9 Update pristine NOT_IN_DB emits CreateLexeme`() {
        val initial = loaded(lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(1L, edited = "hello")))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(NOT_IN_DB, pKey(1L)))
        assertTrue(result.state().isPendingDbOp)
        assertTrue(result.state().isCreatingLexeme)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.CreateLexeme(
                    wordId = 7L, dictionaryId = 3L, pristineKey = 1L,
                    componentTypeId = ComponentTypeId(50L), componentTypeRef = TR,
                    data = textValuesOf("hello"),
                ),
            ),
        )
    }

    @Test
    fun `10 Update trims before effect`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "old", isEdit = true, edited = "  new  ")))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(7L, savedKey(5L)))
        result.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.UpdateValue(
                    wordId = 7L, dictionaryId = 3L, lexemeId = 7L,
                    componentValueId = ComponentValueId(5L),
                    componentTypeId = ComponentTypeId(50L), componentTypeRef = TR,
                    data = textValuesOf("new"),
                ),
            ),
        )
    }

    @Test
    fun `11 guarded by pending`() {
        val initial = loaded(
            isPendingDbOp = true,
            lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "x", isEdit = true, edited = "")))),
        )
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(7L, savedKey(5L)))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }
}
