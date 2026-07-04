package me.apomazkin.wordcard.mate.ext

import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.wordcard.mate.ComponentValueKey
import me.apomazkin.wordcard.mate.ComponentValueState
import me.apomazkin.wordcard.mate.CommitOutcome
import me.apomazkin.wordcard.mate.commitDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Truth table для `ComponentValueState.commitDecision()` — 07 §2.3 / 03 §3.2.
 *
 * Решение зависит ТОЛЬКО от `isEdit` / `edited` / `origin` (по trimmed);
 * key/typeId/ref/isMultiple на исход не влияют.
 */
class CommitDecisionTest {

    private val TR = ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)

    private fun cv(isEdit: Boolean, edited: String, origin: String) =
        ComponentValueState(
            key = ComponentValueKey.Saved(ComponentValueId(1L)),
            componentTypeId = ComponentTypeId(50L),
            componentTypeRef = TR,
            isMultiple = false,
            isEdit = isEdit,
            isCommitting = false,
            origin = origin,
            edited = edited,
        )

    @Test
    fun `commitDecision_returns_NoOp_when_not_editing`() {
        assertEquals(CommitOutcome.NoOp, cv(isEdit = false, edited = "x", origin = "y").commitDecision())
    }

    @Test
    fun `commitDecision_returns_LocalRemove_when_editing_empty_empty`() {
        assertEquals(CommitOutcome.LocalRemove, cv(isEdit = true, edited = "", origin = "").commitDecision())
    }

    @Test
    fun `commitDecision_returns_LocalRemove_when_editing_blank_empty_origin`() {
        assertEquals(CommitOutcome.LocalRemove, cv(isEdit = true, edited = "   ", origin = "").commitDecision())
    }

    @Test
    fun `commitDecision_returns_PessimisticRemove_when_editing_emptied_nonempty_origin`() {
        assertEquals(CommitOutcome.PessimisticRemove, cv(isEdit = true, edited = "", origin = "x").commitDecision())
    }

    @Test
    fun `commitDecision_returns_PessimisticRemove_when_editing_blank_nonempty_origin`() {
        assertEquals(CommitOutcome.PessimisticRemove, cv(isEdit = true, edited = "   ", origin = "x").commitDecision())
    }

    @Test
    fun `commitDecision_returns_NoOp_when_edited_equals_origin`() {
        assertEquals(CommitOutcome.NoOp, cv(isEdit = true, edited = "same", origin = "same").commitDecision())
    }

    @Test
    fun `commitDecision_returns_NoOp_when_trimmed_edited_equals_origin`() {
        assertEquals(CommitOutcome.NoOp, cv(isEdit = true, edited = "  same  ", origin = "same").commitDecision())
    }

    @Test
    fun `commitDecision_returns_Update_when_edited_differs_from_origin`() {
        val outcome = cv(isEdit = true, edited = "new", origin = "old").commitDecision()
        assertTrue("expected Update, got $outcome", outcome is CommitOutcome.Update)
        assertEquals("new", (outcome as CommitOutcome.Update).text)
    }

    @Test
    fun `commitDecision_returns_Update_trimmed_when_edited_has_whitespace`() {
        val outcome = cv(isEdit = true, edited = "  new  ", origin = "old").commitDecision()
        assertTrue("expected Update, got $outcome", outcome is CommitOutcome.Update)
        assertEquals("new", (outcome as CommitOutcome.Update).text)
    }

    @Test
    fun `commitDecision_returns_Update_for_pristine_first_commit_empty_origin`() {
        val outcome = cv(isEdit = true, edited = "new", origin = "").commitDecision()
        assertTrue("expected Update, got $outcome", outcome is CommitOutcome.Update)
        assertEquals("new", (outcome as CommitOutcome.Update).text)
    }

    /**
     * Regression (ревью IS481): `origin` сравнивался сырым, а `edited` — триммленым → ложный `Update`,
     * если `origin` приходит с краевыми пробелами. Сравнение должно быть по trimmed с обеих сторон.
     */
    @Test
    fun `commitDecision_NoOp_when_origin_has_trailing_whitespace`() {
        assertEquals(CommitOutcome.NoOp, cv(isEdit = true, edited = "abc", origin = "abc ").commitDecision())
    }

    /** Regression: whitespace-only origin + пустой edit = LocalRemove (origin = пусто после trim), не Pessimistic. */
    @Test
    fun `commitDecision_LocalRemove_when_blank_edit_and_whitespace_origin`() {
        assertEquals(CommitOutcome.LocalRemove, cv(isEdit = true, edited = "", origin = "   ").commitDecision())
    }
}
