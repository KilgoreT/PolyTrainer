package me.apomazkin.wordcard.mate

import me.apomazkin.core_resources.R
import me.apomazkin.mate.NavigationEffect
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §1.9 Undo/Delete (REWRITE) — LexemeCascadeRemoved / LexemeRemoved / UndoRestoreLexeme /
 * RemoveLexeme (вкл. NOT_IN_DB local).
 */
class UndoDeleteTest {

    private val reducer = WordCardReducer()

    @Test
    fun `LexemeCascadeRemoved removes and emits undo`() {
        val removed = domainLexeme(8L, listOf(domainCv(60L, 8L, "hi", ref = TR)))
        val initial = loaded(lexemes = listOf(lexeme(8L, listOf(savedCv(60L))), lexeme(9L, listOf(savedCv(61L)))))
        val result = reducer.testReduce(initial, Msg.LexemeCascadeRemoved(removed))
        assertEquals(listOf(9L), result.state().lexemeList.map { it.id })
        assertFalse(result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                UiEffect.ShowSnackbarWithUndo(
                    messageRes = R.string.word_card_snackbar_lexeme_deleted,
                    actionLabelRes = R.string.word_card_snackbar_undo,
                    undoMsg = Msg.UndoRestoreLexeme(removed),
                ),
            ),
        )
    }

    @Test
    fun `LexemeRemoved removes and emits undo`() {
        val removed = domainLexeme(9L, listOf(domainCv(61L, 9L, "x", ref = TR)))
        val initial = loaded(lexemes = listOf(lexeme(8L, listOf(savedCv(60L))), lexeme(9L, listOf(savedCv(61L)))))
        val result = reducer.testReduce(initial, Msg.LexemeRemoved(removed))
        assertEquals(listOf(8L), result.state().lexemeList.map { it.id })
        result.assertEffects(
            setOf(
                UiEffect.ShowSnackbarWithUndo(
                    messageRes = R.string.word_card_snackbar_lexeme_deleted,
                    actionLabelRes = R.string.word_card_snackbar_undo,
                    undoMsg = Msg.UndoRestoreLexeme(removed),
                ),
            ),
        )
    }

    @Test
    fun `cascade clears pending and is not guarded`() {
        val removed = domainLexeme(8L, listOf(domainCv(60L, 8L, "hi", ref = TR)))
        val initial = loaded(isPendingDbOp = true, lexemes = listOf(lexeme(8L, listOf(savedCv(60L)))))
        val result = reducer.testReduce(initial, Msg.LexemeCascadeRemoved(removed))
        assertFalse(result.state().isPendingDbOp)
        assertTrue(result.state().lexemeList.isEmpty())
    }

    @Test
    fun `UndoRestoreLexeme_sets_pending_and_emits_restore`() {
        val snapshot = domainLexeme(8L, listOf(domainCv(60L, 8L, "hi", ref = TR), domainCv(61L, 8L, "e", typeId = 51L, ref = me.apomazkin.lexeme.ComponentTypeRef.UserDefined("Example"))))
        val initial = loaded(wordId = 7L, dictionaryId = 3L)
        val result = reducer.testReduce(initial, Msg.UndoRestoreLexeme(snapshot))
        assertTrue(result.state().isPendingDbOp)
        result.assertEffects(setOf(DatasourceEffect.RestoreLexemeWithComponents(7L, 3L, snapshot)))
    }

    @Test
    fun `RemoveLexeme emits effect`() {
        val initial = loaded(wordId = 7L, lexemes = listOf(lexeme(8L, listOf(savedCv(60L)))))
        val result = reducer.testReduce(initial, Msg.RemoveLexeme(8L))
        result.assertEffects(setOf(DatasourceEffect.RemoveLexeme(7L, 8L)))
    }

    @Test
    fun `RemoveLexeme_NOT_IN_DB_local_no_effect_no_undo`() {
        val initial = loaded(wordId = 7L, lexemes = listOf(lexeme(NOT_IN_DB, emptyList()), lexeme(8L, listOf(savedCv(60L)))))
        val result = reducer.testReduce(initial, Msg.RemoveLexeme(NOT_IN_DB))
        assertEquals(listOf(8L), result.state().lexemeList.map { it.id })
        assertFalse(result.state().isPendingDbOp)
        assertFalse(result.state().isCreatingLexeme)
        result.assertNoEffects()
    }

    /**
     * Regression (BUG-2, docs/features/IS481_bugs/bugs.md, сценарий L3.1): подтверждение
     * в confirm-диалоге (`RemoveLexeme`) не сбрасывало `lexemeIdPendingDelete` — лексема
     * удалялась ПОД модалкой, а сама модалка оставалась висеть (`ConfirmDeleteLexemeWidget`
     * показывается, пока поле != null). Закрывался диалог только по «Отмена»
     * (`CloseDeleteLexemeDialog`); путь подтверждения был не покрыт тестами.
     */
    @Test
    fun `RemoveLexeme closes confirm dialog`() {
        val initial = loaded(wordId = 7L, lexemes = listOf(lexeme(8L, listOf(savedCv(60L)))))
            .copy(lexemeIdPendingDelete = 8L)
        val result = reducer.testReduce(initial, Msg.RemoveLexeme(8L))
        assertNull("модалка закрыта", result.state().lexemeIdPendingDelete)
        result.assertEffects(setOf(DatasourceEffect.RemoveLexeme(7L, 8L)))
    }

    /** Regression (BUG-2): локальный путь (NOT_IN_DB черновик) тоже обязан закрывать модалку. */
    @Test
    fun `RemoveLexeme NOT_IN_DB closes confirm dialog`() {
        val initial = loaded(wordId = 7L, lexemes = listOf(lexeme(NOT_IN_DB, emptyList())))
            .copy(lexemeIdPendingDelete = NOT_IN_DB)
        val result = reducer.testReduce(initial, Msg.RemoveLexeme(NOT_IN_DB))
        assertNull("модалка закрыта", result.state().lexemeIdPendingDelete)
        result.assertNoEffects()
    }

    /**
     * Regression (ревью IS481): cascade-remove во время flush-on-back (`isExiting`) не должен
     * слать undo-снек — экран тут же закрывается пост-шагом (`Back`), снек «Отменить» бесполезен.
     */
    @Test
    fun `cascade during exit suppresses undo snackbar`() {
        val removed = domainLexeme(8L, listOf(domainCv(60L, 8L, "x", ref = TR)))
        val initial = loaded(
            isExiting = true,
            lexemes = listOf(lexeme(8L, listOf(savedCv(60L, isCommitting = true)))),
        )
        val result = reducer.testReduce(initial, Msg.LexemeCascadeRemoved(removed))
        assertTrue("лексема удалена", result.state().lexemeList.isEmpty())
        result.assertEffects(setOf(NavigationEffect.Back))
    }

    /**
     * Regression (ревью IS481, MAJOR / сценарий A17): провал восстановления (undo) должен дать
     * retry-снек с `UndoRestoreLexeme(snapshot)`. Баг: эмитился `ShowErrorSnackbar` без action,
     * snapshot отбрасывался → повтор восстановления невозможен. Reducer-ветка не была покрыта.
     */
    @Test
    fun `RestoreLexemeFailed emits retry snackbar carrying snapshot`() {
        val snapshot = domainLexeme(8L, listOf(domainCv(60L, 8L, "x", ref = TR)))
        val result = reducer.testReduce(loaded(isPendingDbOp = true), Msg.RestoreLexemeFailed(snapshot))
        assertFalse("pending снят", result.state().isPendingDbOp)
        result.assertEffects(
            setOf(
                UiEffect.ShowSnackbarWithRetry(
                    messageRes = R.string.word_card_error_restore_lexeme,
                    actionLabelRes = R.string.word_card_action_retry,
                    retryMsg = Msg.UndoRestoreLexeme(snapshot),
                ),
            ),
        )
    }
}
