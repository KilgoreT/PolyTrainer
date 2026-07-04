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
 * Reducer-тесты Word inline edit (поведение слова — unchanged, IS479). Мигрировано на
 * generic-модель: word-edit-commit активной правки компонента идёт через UpsertComponentValue.
 */
class WordEditTest {

    private val reducer = WordCardReducer()

    /** loaded() (с dictionaryId) + word-edit поля. */
    private fun wordLoaded(
        value: String = "word",
        isEditMode: Boolean = false,
        edited: String = "",
        isPendingDbOp: Boolean = false,
        lexemes: List<LexemeState> = emptyList(),
    ): WordCardState {
        val base = loaded(wordId = 123L, isPendingDbOp = isPendingDbOp, lexemes = lexemes)
        return base.copy(
            wordState = (base.wordState as WordState.Loaded).copy(value = value, isEditMode = isEditMode, edited = edited),
        )
    }

    @Test
    fun `given NotLoaded when EnterWordEditMode then state unchanged`() {
        val initial = WordCardState(isLoading = false, wordState = WordState.NotLoaded)
        val result = reducer.testReduce(initial, Msg.EnterWordEditMode)
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `given Loaded when EnterWordEditMode then isEditMode true edited equals value`() {
        val result = reducer.testReduce(wordLoaded(value = "hello"), Msg.EnterWordEditMode)
        val loaded = result.state().wordState as WordState.Loaded
        assertTrue(loaded.isEditMode)
        assertEquals("hello", loaded.edited)
        result.assertNoEffects()
    }

    @Test
    fun `EnterWordEditMode commits open component edit (generic, Bug2-parity)`() {
        val initial = wordLoaded(
            lexemes = listOf(lexeme(1L, listOf(savedCv(5L, origin = "a", isEdit = true, edited = "edit")))),
        )
        val result = reducer.testReduce(initial, Msg.EnterWordEditMode)
        val loaded = result.state().wordState as WordState.Loaded
        assertTrue("word edit enabled", loaded.isEditMode)
        val cv = result.state().lexemeList.first().components.single()
        assertTrue("A10 edit держится", cv.isEdit)
        assertTrue(cv.isCommitting)
        assertTrue(result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.UpdateValue(
                    wordId = 123L, dictionaryId = 3L, lexemeId = 1L,
                    componentValueId = ComponentValueId(5L),
                    componentTypeId = ComponentTypeId(50L), componentTypeRef = TR,
                    data = textValuesOf("edit"),
                ),
            ),
        )
    }

    @Test
    fun `UpdateWordInput updates edited locally`() {
        val result = reducer.testReduce(wordLoaded(isEditMode = true, edited = "x"), Msg.UpdateWordInput(value = "abc"))
        assertEquals("abc", (result.state().wordState as WordState.Loaded).edited)
        result.assertNoEffects()
    }

    @Test
    fun `CommitWordChanges with blank edited is no-op`() {
        val initial = wordLoaded(isEditMode = true, edited = "   ")
        val result = reducer.testReduce(initial, Msg.CommitWordChanges)
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `CommitWordChanges valid emits UpdateWord and sets pending`() {
        val result = reducer.testReduce(wordLoaded(value = "old", isEditMode = true, edited = "new"), Msg.CommitWordChanges)
        assertTrue(result.state().isPendingDbOp)
        val loaded = result.state().wordState as WordState.Loaded
        assertFalse(loaded.isEditMode)
        assertEquals("", loaded.edited)
        result.assertEffects(setOf(DatasourceEffect.UpdateWord(wordId = 123L, value = "new")))
    }

    @Test
    fun `CommitWordChanges guarded by pending`() {
        val initial = wordLoaded(isEditMode = true, edited = "new", isPendingDbOp = true)
        val result = reducer.testReduce(initial, Msg.CommitWordChanges)
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }
}
