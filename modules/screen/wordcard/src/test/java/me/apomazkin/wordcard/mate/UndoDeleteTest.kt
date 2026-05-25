package me.apomazkin.wordcard.mate

import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Reducer tests for IS479: snackbar c undo при удалении translation/definition/лексемы.
 *
 * Покрытие:
 * - TranslationDeleted / DefinitionDeleted → state update + ShowSnackbarWithUndo Effect
 * - LexemeCascadeRemovedWithUndo → NOT_IN_DB-черновик + ShowSnackbarWithUndo с правильным текстом
 * - LexemeRemoved (full-delete с snapshot) → remove + ShowSnackbarWithUndo
 * - LexemeRemoved без snapshot (snapshot==null/null) → remove без Effect (нет смысла в undo)
 * - UndoRemoveTranslation real id → UpdateLexemeTranslation(realId)
 * - UndoRemoveTranslation NOT_IN_DB → UpdateLexemeTranslation(null) = re-INSERT
 * - UndoRemoveLexeme c обеими субсущностями → RestoreLexeme(wordId, t, d)
 */
class UndoDeleteTest {

    private fun loaded(
        wordId: Long = 7L,
        lexemes: List<LexemeState> = emptyList(),
    ): WordCardState = WordCardState(
        isLoading = false,
        isPendingDbOp = true, // обычно delete завершается — pending уже true
        wordState = WordState.Loaded(id = wordId, added = Date(0L), value = "w"),
        lexemeList = lexemes,
    )

    @Test
    fun `TranslationDeleted nullifies translation and emits ShowSnackbarWithUndo`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(
                id = 42L,
                translation = TextValueState(origin = "old"),
                definition = TextValueState(origin = "d"),
            ),
        ))

        val result = reducer.testReduce(
            initial,
            Msg.TranslationDeleted(lexemeId = 42L, removedValue = "old"),
        )

        val lex = result.state().lexemeList.first()
        assertNull("translation nullified", lex.translation)
        assertNotNull("definition preserved", lex.definition)
        assertFalse("pending cleared", result.state().isPendingDbOp)
        val effects = result.effects()
        assertEquals(1, effects.size)
        val effect = effects.first() as UiEffect.ShowSnackbarWithUndo
        assertEquals(
            Msg.UndoRemoveTranslation(lexemeId = 42L, value = "old"),
            effect.undoMsg,
        )
    }

    @Test
    fun `DefinitionDeleted nullifies definition and emits ShowSnackbarWithUndo`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(
                id = 42L,
                translation = TextValueState(origin = "t"),
                definition = TextValueState(origin = "old"),
            ),
        ))

        val result = reducer.testReduce(
            initial,
            Msg.DefinitionDeleted(lexemeId = 42L, removedValue = "old"),
        )

        val lex = result.state().lexemeList.first()
        assertNull(lex.definition)
        assertNotNull(lex.translation)
        val effect = result.effects().first() as UiEffect.ShowSnackbarWithUndo
        assertEquals(
            Msg.UndoRemoveDefinition(lexemeId = 42L, value = "old"),
            effect.undoMsg,
        )
    }

    @Test
    fun `LexemeCascadeRemovedWithUndo (translation deleted) turns lexeme into NOT_IN_DB draft + snackbar with translation text`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(
                id = 42L,
                translation = TextValueState(origin = "hello"),
                definition = null,
            ),
        ))

        val result = reducer.testReduce(
            initial,
            Msg.LexemeCascadeRemovedWithUndo(
                lexemeId = 42L,
                removedTranslation = "hello",
                removedDefinition = null,
            ),
        )

        val state = result.state()
        val draft = state.lexemeList.first()
        assertEquals(NOT_IN_DB, draft.id)
        assertNull(draft.translation)
        assertNull(draft.definition)
        assertFalse(state.isPendingDbOp)
        assertTrue("isCreatingLexeme set (because NOT_IN_DB)", state.isCreatingLexeme)
        val effect = result.effects().first() as UiEffect.ShowSnackbarWithUndo
        // undoMsg = UndoRemoveTranslation с NOT_IN_DB → re-INSERT
        assertEquals(
            Msg.UndoRemoveTranslation(lexemeId = NOT_IN_DB, value = "hello"),
            effect.undoMsg,
        )
    }

    @Test
    fun `LexemeRemoved with snapshot emits ShowSnackbarWithUndo and removes lexeme`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(
            LexemeState(
                id = 42L,
                translation = TextValueState(origin = "t"),
                definition = TextValueState(origin = "d"),
            ),
        ))

        val result = reducer.testReduce(
            initial,
            Msg.LexemeRemoved(lexemeId = 42L, translation = "t", definition = "d"),
        )

        val state = result.state()
        assertTrue("lexeme removed", state.lexemeList.isEmpty())
        assertFalse(state.isPendingDbOp)
        val effect = result.effects().first() as UiEffect.ShowSnackbarWithUndo
        assertEquals(
            Msg.UndoRemoveLexeme(translation = "t", definition = "d"),
            effect.undoMsg,
        )
    }

    @Test
    fun `LexemeRemoved without snapshot removes lexeme without Effect`() {
        val reducer = WordCardReducer()
        val initial = loaded(lexemes = listOf(LexemeState(id = 42L)))

        val result = reducer.testReduce(
            initial,
            Msg.LexemeRemoved(lexemeId = 42L, translation = null, definition = null),
        )

        assertTrue(result.state().lexemeList.isEmpty())
        result.assertNoEffects()
    }

    @Test
    fun `UndoRemoveTranslation with real id emits UpdateLexemeTranslation with real lexemeId`() {
        val reducer = WordCardReducer()
        val initial = loaded(
            wordId = 100L,
            lexemes = listOf(LexemeState(id = 42L)),
        ).copy(isPendingDbOp = false)

        val result = reducer.testReduce(
            initial,
            Msg.UndoRemoveTranslation(lexemeId = 42L, value = "restored"),
        )

        assertTrue("pending set on undo", result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpdateLexemeTranslation(
                    wordId = 100L,
                    lexemeId = 42L,
                    translation = "restored",
                ),
            ),
        )
    }

    @Test
    fun `UndoRemoveTranslation with NOT_IN_DB emits UpdateLexemeTranslation with null lexemeId (re-INSERT)`() {
        val reducer = WordCardReducer()
        val initial = loaded(
            wordId = 100L,
            lexemes = listOf(LexemeState(id = NOT_IN_DB)),
        ).copy(isPendingDbOp = false)

        val result = reducer.testReduce(
            initial,
            Msg.UndoRemoveTranslation(lexemeId = NOT_IN_DB, value = "restored"),
        )

        result.assertEffects(
            setOf(
                DatasourceEffect.UpdateLexemeTranslation(
                    wordId = 100L,
                    lexemeId = null,
                    translation = "restored",
                ),
            ),
        )
    }

    @Test
    fun `UndoRemoveLexeme with both translation and definition emits RestoreLexeme`() {
        val reducer = WordCardReducer()
        val initial = loaded(wordId = 100L).copy(isPendingDbOp = false)

        val result = reducer.testReduce(
            initial,
            Msg.UndoRemoveLexeme(translation = "t", definition = "d"),
        )

        assertTrue(result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                DatasourceEffect.RestoreLexeme(
                    wordId = 100L,
                    translation = "t",
                    definition = "d",
                ),
            ),
        )
    }
}
