package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import me.apomazkin.wordcard.entity.Term
import me.apomazkin.wordcard.entity.Word
import me.apomazkin.wordcard.entity.WordId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Reducer tests for Word inline edit under sealed WordState.
 *
 * Test cases:
 * - EnterWordEditMode: guard wordState !is Loaded.
 * - EnterWordEditMode under Loaded: isEditMode=true, edited=value.
 * - EnterWordEditMode with active lexeme edit: closeAllEditModes closes chip first.
 * - UpdateWordInput: локальный edited.
 * - CommitWordChanges blank guard (инв. 10): edited="   " → state unchanged.
 * - CommitWordChanges valid: emits UpdateWord, isPendingDbOp=true.
 * - CommitWordChanges under isPendingDbOp=true → state unchanged.
 * - RefreshWord: updates value, clears pending, no Effect.
 */
class WordEditTest {

    private fun loaded(
        id: Long = 123L,
        value: String = "word",
        isEditMode: Boolean = false,
        edited: String = "",
        isPendingDbOp: Boolean = false,
        lexemes: List<LexemeState> = emptyList(),
    ): WordCardState = WordCardState(
        isLoading = false,
        isPendingDbOp = isPendingDbOp,
        wordState = WordState.Loaded(
            id = id,
            added = Date(0L),
            value = value,
            isEditMode = isEditMode,
            edited = edited,
        ),
        lexemeList = lexemes,
    )

    @Test
    fun `given NotLoaded when EnterWordEditMode then state unchanged`() {
        val reducer = WordCardReducer()
        val initial = WordCardState(isLoading = false, wordState = WordState.NotLoaded)

        val result = reducer.testReduce(initial, Msg.EnterWordEditMode)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `given Loaded when EnterWordEditMode then isEditMode true edited equals value`() {
        val reducer = WordCardReducer()
        val initial = loaded(value = "hello")

        val result = reducer.testReduce(initial, Msg.EnterWordEditMode)

        val loaded = result.state().wordState as WordState.Loaded
        assertTrue(loaded.isEditMode)
        assertEquals("hello", loaded.edited)
        result.assertNoEffects()
    }

    @Test
    fun `EnterWordEditMode commits pending lexeme edit (Bug 2 IS479)`() {
        // Bug 2 (IS479): переключение в word-edit с активным lexeme-edit
        // commit'ит pending translation, не отбрасывает его.
        val reducer = WordCardReducer()
        val initial = loaded(
            id = 123L,
            value = "word",
            lexemes = listOf(
                LexemeState(id = 1L, translation = TextValueState(origin = "a", edited = "edit", isEdit = true)),
            ),
        )

        val result = reducer.testReduce(initial, Msg.EnterWordEditMode)

        val state = result.state()
        val loaded = state.wordState as WordState.Loaded
        assertTrue("word edit enabled", loaded.isEditMode)
        val lex = state.lexemeList.first()
        assertFalse("chip edit closed", lex.translation?.isEdit ?: true)
        assertEquals("chip edited reset", "", lex.translation?.edited)
        assertEquals("origin advanced to commit value", "edit", lex.translation?.origin)
        assertTrue("pending DB op set due to commit effect", state.isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpdateLexemeTranslation(
                    wordId = 123L,
                    lexemeId = 1L,
                    translation = "edit",
                ),
            ),
        )
    }

    @Test
    fun `UpdateWordInput updates edited locally`() {
        val reducer = WordCardReducer()
        val initial = loaded(isEditMode = true, edited = "x")

        val result = reducer.testReduce(initial, Msg.UpdateWordInput(value = "abc"))

        val loaded = result.state().wordState as WordState.Loaded
        assertEquals("abc", loaded.edited)
        result.assertNoEffects()
    }

    @Test
    fun `CommitWordChanges with blank edited is no-op (инв 10)`() {
        val reducer = WordCardReducer()
        val initial = loaded(isEditMode = true, edited = "   ")

        val result = reducer.testReduce(initial, Msg.CommitWordChanges)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `CommitWordChanges valid emits UpdateWord effect and sets pending`() {
        val reducer = WordCardReducer()
        val initial = loaded(id = 123L, value = "old", isEditMode = true, edited = "new")

        val result = reducer.testReduce(initial, Msg.CommitWordChanges)

        assertTrue(result.state().isPendingDbOp)
        val loaded = result.state().wordState as WordState.Loaded
        assertFalse("edit mode reset", loaded.isEditMode)
        assertEquals("edited reset", "", loaded.edited)
        result.assertEffects(setOf(DatasourceEffect.UpdateWord(wordId = 123L, value = "new")))
    }

    @Test
    fun `given isPendingDbOp true CommitWordChanges is no-op (global guard)`() {
        val reducer = WordCardReducer()
        val initial = loaded(isEditMode = true, edited = "new", isPendingDbOp = true)

        val result = reducer.testReduce(initial, Msg.CommitWordChanges)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `RefreshWord updates value and clears pending`() {
        val reducer = WordCardReducer()
        val initial = loaded(isPendingDbOp = true)
        val newTerm = Term(
            wordId = WordId(123L),
            word = Word("renamed"),
            addedDate = Date(0L),
            changedDate = null,
            removedDate = null,
            lexemeList = emptyList(),
        )

        val result = reducer.testReduce(initial, Msg.RefreshWord(word = newTerm))

        val loaded = result.state().wordState as WordState.Loaded
        assertEquals("renamed", loaded.value)
        assertFalse(result.state().isPendingDbOp)
        result.assertNoEffects()
    }
}
