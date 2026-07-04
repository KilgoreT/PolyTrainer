package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §2.4 commitAndCloseAllEdits(): Pair<State, Set<Effect>> (G6).
 */
class CommitAndCloseAllEditsTest {

    private val EX = ComponentTypeRef.UserDefined("Example")
    private val EX2 = ComponentTypeRef.UserDefined("Example2")

    @Test
    fun `mix_of_outcomes_emits_correct_effects`() {
        val initial = loaded(
            lexemes = listOf(
                lexeme(
                    7L,
                    listOf(
                        savedCv(5L, origin = "same", isEdit = true, edited = "same"), // NoOp
                        savedCv(6L, origin = "old", isEdit = true, edited = "new"),    // Update
                        savedCv(8L, origin = "x", isEdit = true, edited = ""),         // PessimisticRemove
                        pristineCv(9L, edited = ""),                                   // LocalRemove
                    ),
                ),
            ),
        )
        val (state, effects) = initial.commitAndCloseAllEdits()
        val comps = state.lexemeList.single().components
        assertFalse("#5 NoOp closed", comps.first { it.key == ComponentValueKey.Saved(ComponentValueId(5L)) }.isEdit)
        val six = comps.first { it.key == ComponentValueKey.Saved(ComponentValueId(6L)) }
        assertTrue("#6 A10 holds edit", six.isEdit)
        assertTrue(six.isCommitting)
        assertTrue("#9 pristine dropped", comps.none { it.pristineKey == 9L })
        assertTrue(state.isPendingDbOp)
        assertEquals(
            setOf(
                DatasourceEffect.UpsertComponentValue.UpdateValue(
                    wordId = 7L, dictionaryId = 3L, lexemeId = 7L,
                    componentValueId = ComponentValueId(6L),
                    componentTypeId = ComponentTypeId(50L), componentTypeRef = TR,
                    data = textValuesOf("new"),
                ),
                DatasourceEffect.RemoveComponentValue(ComponentValueId(8L), 7L),
            ),
            effects,
        )
    }

    @Test
    fun `B6_only_Update_PessimisticRemove_set_pending`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "old", isEdit = true, edited = "new")))))
        val (state, effects) = initial.commitAndCloseAllEdits()
        assertTrue(state.isPendingDbOp)
        assertEquals(1, effects.size)
    }

    @Test
    fun `B6_NoOp_LocalRemove_do_not_set_pending`() {
        val initial = loaded(
            lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "s", isEdit = true, edited = "s"), pristineCv(9L, edited = "")))),
        )
        val (state, effects) = initial.commitAndCloseAllEdits()
        assertFalse(state.isPendingDbOp)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `NOT_IN_DB_3_pristine_emits_exactly_one_create_anchor_first`() {
        val initial = loaded(
            lexemes = listOf(
                lexeme(
                    NOT_IN_DB,
                    listOf(
                        pristineCv(1L, ref = TR, edited = "t"),
                        pristineCv(2L, ref = EX, edited = "e"),
                        pristineCv(3L, ref = EX2, edited = "x"),
                    ),
                ),
            ),
        )
        val (state, effects) = initial.commitAndCloseAllEdits()
        assertTrue(state.isPendingDbOp)
        assertEquals(1, effects.size)
        val eff = effects.single() as DatasourceEffect.UpsertComponentValue.CreateLexeme
        assertEquals(1L, eff.pristineKey)
    }

    @Test
    fun `NOT_IN_DB_anchor_first_by_order_NOT_translation`() {
        val initial = loaded(
            lexemes = listOf(
                lexeme(NOT_IN_DB, listOf(pristineCv(5L, ref = EX, edited = "e"), pristineCv(1L, ref = TR, edited = "t"))),
            ),
        )
        val (_, effects) = initial.commitAndCloseAllEdits()
        val eff = effects.single() as DatasourceEffect.UpsertComponentValue.CreateLexeme
        assertEquals("первый по порядку (Example), НЕ TR", 5L, eff.pristineKey)
    }

    @Test
    fun `B3_NOT_IN_DB_single_empty_pristine_removed`() {
        val initial = loaded(lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(1L, ref = TR, edited = "")))))
        val (state, effects) = initial.commitAndCloseAllEdits()
        assertTrue(state.lexemeList.isEmpty())
        assertFalse(state.isPendingDbOp)
        assertFalse(state.isCreatingLexeme)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `multi_lexeme_commits_each_no_early_return`() {
        val initial = loaded(
            lexemes = listOf(
                lexeme(7L, listOf(savedCv(6L, origin = "o", isEdit = true, edited = "n6"))),
                lexeme(8L, listOf(savedCv(9L, origin = "o", isEdit = true, edited = "n9"))),
            ),
        )
        val (_, effects) = initial.commitAndCloseAllEdits()
        assertEquals(2, effects.size)
    }

    @Test
    fun `real_lexeme_two_pristine_updates_emit_BOTH_no_anchor_rule`() {
        val initial = loaded(
            lexemes = listOf(lexeme(7L, listOf(pristineCv(1L, edited = "a"), pristineCv(2L, edited = "b")))),
        )
        val (_, effects) = initial.commitAndCloseAllEdits()
        assertEquals("anchor-правило НЕ применяется к real лексеме", 2, effects.size)
    }

    @Test
    fun `B3_NOT_IN_DB_empty_plus_filled_skips_empty_anchor`() {
        val initial = loaded(
            lexemes = listOf(
                lexeme(NOT_IN_DB, listOf(pristineCv(1L, ref = TR, edited = ""), pristineCv(2L, typeId = 51L, ref = EX, edited = "e"))),
            ),
        )
        val (_, effects) = initial.commitAndCloseAllEdits()
        assertEquals(1, effects.size)
        val eff = effects.single() as DatasourceEffect.UpsertComponentValue.CreateLexeme
        assertEquals("anchor пропускает пустой #1, берёт первый НЕпустой #2", 2L, eff.pristineKey)
    }

    @Test
    fun `word_edit_committed_alongside_components`() {
        val base = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "old", isEdit = true, edited = "new")))))
        val initial = base.copy(wordState = (base.wordState as WordState.Loaded).copy(isEditMode = true, edited = "ed", value = "w"))
        val (state, effects) = initial.commitAndCloseAllEdits()
        val loaded = state.wordState as WordState.Loaded
        assertEquals("ed", loaded.value)
        assertFalse(loaded.isEditMode)
        assertTrue(effects.any { it is DatasourceEffect.UpdateWord && it.wordId == 7L && it.value == "ed" })
    }

    @Test
    fun `word_edit_NoOp_when_unchanged`() {
        val base = loaded()
        val initial = base.copy(wordState = (base.wordState as WordState.Loaded).copy(isEditMode = true, edited = "w", value = "w"))
        val (_, effects) = initial.commitAndCloseAllEdits()
        assertFalse(effects.any { it is DatasourceEffect.UpdateWord })
    }
}
