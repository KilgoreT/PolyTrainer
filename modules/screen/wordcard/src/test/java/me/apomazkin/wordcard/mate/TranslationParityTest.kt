package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §6 Anti-regression: Translation = обычный компонент (BuiltIn(TRANSLATION)).
 * Снимок поведения старого TranslationManagementTest на новой generic-модели
 * (НЕ 1:1 — §9.6: 7 релевантных из 15 + явные девиации).
 */
class TranslationParityTest {

    private val reducer = WordCardReducer()
    private fun savedK(id: Long) = ComponentValueKey.Saved(ComponentValueId(id))
    private fun pK(k: Long) = ComponentValueKey.Pristine(k)

    @Test
    fun `translation_create_gives_empty_editable_pristine`() {
        val initial = loaded(
            availableTypes = listOf(ctype(50L, TR, isMultiple = false)),
            lexemes = listOf(lexeme(1L, emptyList())),
        )
        val result = reducer.testReduce(initial, Msg.CreateComponentValue(1L, ComponentTypeId(50L)))
        val cv = result.state().lexemeList.single().components.single()
        assertTrue(cv.isPristine)
        assertTrue(cv.isEdit)
        assertEquals("", cv.origin)
        assertEquals("", cv.edited)
        assertEquals(TR, cv.componentTypeRef)
    }

    @Test
    fun `translation_commit_update_on_saved_emits_UpdateValue`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, ref = TR, origin = "old", isEdit = true, edited = "new")))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(7L, savedK(5L)))
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
    fun `translation_remove_saved_emits_RemoveComponentValue`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, ref = TR, origin = "x"), savedCv(6L)))))
        val result = reducer.testReduce(initial, Msg.RemoveComponentValueRequested(7L, savedK(5L)))
        assertTrue(result.state().isPendingDbOp)
        result.assertEffects(setOf(DatasourceEffect.RemoveComponentValue(ComponentValueId(5L), 7L)))
    }

    @Test
    fun `translation_pristine_first_commit_on_NOT_IN_DB_creates_lexeme`() {
        val initial = loaded(lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(1L, ref = TR, edited = "hello")))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(NOT_IN_DB, pK(1L)))
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
    fun `translation_refresh_closes_edit`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, ref = TR, origin = "old", isEdit = true, isCommitting = true, edited = "typing")))))
        val result = reducer.testReduce(initial, Msg.RefreshLexemeComponents(7L, listOf(domainCv(5L, 7L, "dbval", ref = TR))))
        val cv = result.state().lexemeList.single().components.single()
        assertEquals("dbval", cv.origin)
        assertTrue(!cv.isEdit && !cv.isCommitting)
    }

    @Test
    fun `translation_pending_guard`() {
        val initial = loaded(isPendingDbOp = true, lexemes = listOf(lexeme(7L, listOf(savedCv(5L, ref = TR, origin = "x", isEdit = true, edited = "y")))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(7L, savedK(5L)))
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `translation_local_remove_pristine_no_effect`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(pristineCv(1L, ref = TR, edited = ""), savedCv(9L)))))
        val result = reducer.testReduce(initial, Msg.CommitComponentValueEdit(7L, pK(1L)))
        assertTrue(result.state().lexemeList.single().components.none { it.isPristine })
        result.assertNoEffects()
    }
}
