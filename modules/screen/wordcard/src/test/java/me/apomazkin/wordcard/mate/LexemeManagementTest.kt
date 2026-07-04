package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §1.8 LexemeDraftPromoted (REWRITE) — B2 pristine-merge / F3-F4 anchor-by-key / G8 in-place.
 */
class LexemeManagementTest {

    private val reducer = WordCardReducer()
    private val EX = ComponentTypeRef.UserDefined("Example")
    private val EX2 = ComponentTypeRef.UserDefined("Example2")

    @Test
    fun `single anchor pristine yields no reemit`() {
        val initial = loaded(lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(1L, ref = TR, edited = "t")))))
        val promoted = domainLexeme(55L, listOf(domainCv(60L, 55L, "t", ref = TR)))
        val result = reducer.testReduce(initial, Msg.LexemeDraftPromoted(promoted, anchorPristineKey = 1L))
        val lex = result.state().lexemeList.single()
        assertEquals(55L, lex.id)
        assertEquals(1, lex.components.size)
        assertFalse(result.state().isPendingDbOp)
        result.assertNoEffects()
    }

    @Test
    fun `B2_two_pristines_reemit_survivor`() {
        val initial = loaded(
            lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(1L, ref = TR, edited = "t"), pristineCv(2L, typeId = 51L, ref = EX, edited = "ex")))),
        )
        val promoted = domainLexeme(55L, listOf(domainCv(60L, 55L, "t", ref = TR)))
        val result = reducer.testReduce(initial, Msg.LexemeDraftPromoted(promoted, anchorPristineKey = 1L))
        val lex = result.state().lexemeList.single()
        assertEquals(55L, lex.id)
        val survivor = lex.components.first { it.pristineKey == 2L }
        assertTrue("survivor in-flight", survivor.isCommitting)
        result.assertEffects(
            setOf(
                DatasourceEffect.UpsertComponentValue.AddValue(
                    wordId = 7L, dictionaryId = 3L, lexemeId = 55L, pristineKey = 2L,
                    componentTypeId = ComponentTypeId(51L), componentTypeRef = EX,
                    data = textValuesOf("ex"),
                ),
            ),
        )
    }

    @Test
    fun `F3_F4_excludes_anchor_by_key_not_order`() {
        val initial = loaded(
            lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(5L, typeId = 51L, ref = EX, edited = "ex"), pristineCv(1L, ref = TR, edited = "t")))),
        )
        val promoted = domainLexeme(55L, listOf(domainCv(60L, 55L, "t", ref = TR)))
        val result = reducer.testReduce(initial, Msg.LexemeDraftPromoted(promoted, anchorPristineKey = 1L))
        val effs = result.effects()
        assertEquals(1, effs.size)
        val add = effs.single() as DatasourceEffect.UpsertComponentValue.AddValue
        assertEquals("реэмит только survivor key=5", 5L, add.pristineKey)
        val trCount = result.state().lexemeList.single().components.count { it.componentTypeRef == TR }
        assertEquals("ровно один TRANSLATION", 1, trCount)
    }

    @Test
    fun `F3_F4_B5_added_pristine_after_emit_excluded_by_key`() {
        val EX2 = ComponentTypeRef.UserDefined("Example2")
        val initial = loaded(
            lexemes = listOf(
                lexeme(
                    NOT_IN_DB,
                    listOf(
                        pristineCv(1L, ref = TR, edited = "t"),
                        pristineCv(2L, typeId = 51L, ref = EX, edited = "a"),
                        pristineCv(3L, typeId = 52L, ref = EX2, edited = "b"),
                    ),
                ),
            ),
        )
        val promoted = domainLexeme(55L, listOf(domainCv(60L, 55L, "t", ref = TR)))
        val result = reducer.testReduce(initial, Msg.LexemeDraftPromoted(promoted, anchorPristineKey = 1L))
        val effs = result.effects()
        assertEquals("2 реэмита (survivors 2,3), якорь 1 исключён", 2, effs.size)
        val keys = effs.map { (it as DatasourceEffect.UpsertComponentValue.AddValue).pristineKey }.toSet()
        assertEquals(setOf(2L, 3L), keys)
        val survivors = result.state().lexemeList.single().components.filter { it.isPristine }
        assertTrue("оба survivor in-flight", survivors.all { it.isCommitting })
    }

    @Test
    fun `empty_survivor_dropped_no_reemit`() {
        val initial = loaded(
            lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(1L, ref = TR, edited = "t"), pristineCv(2L, ref = EX, edited = "")))),
        )
        val promoted = domainLexeme(55L, listOf(domainCv(60L, 55L, "t", ref = TR)))
        val result = reducer.testReduce(initial, Msg.LexemeDraftPromoted(promoted, anchorPristineKey = 1L))
        assertEquals(1, result.state().lexemeList.single().components.size)
        result.assertNoEffects()
    }

    @Test
    fun `clears pending`() {
        val initial = loaded(isPendingDbOp = true, lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(1L, ref = TR, edited = "t")))))
        val promoted = domainLexeme(55L, listOf(domainCv(60L, 55L, "t", ref = TR)))
        val result = reducer.testReduce(initial, Msg.LexemeDraftPromoted(promoted, anchorPristineKey = 1L))
        assertFalse(result.state().isPendingDbOp)
    }

    @Test
    fun `keeps_position_replaces_in_place`() {
        val initial = loaded(
            lexemes = listOf(lexeme(NOT_IN_DB, listOf(pristineCv(1L, ref = TR, edited = "t"))), lexeme(8L, listOf(savedCv(5L)))),
        )
        val promoted = domainLexeme(55L, listOf(domainCv(60L, 55L, "t", ref = TR)))
        val result = reducer.testReduce(initial, Msg.LexemeDraftPromoted(promoted, anchorPristineKey = 1L))
        val list = result.state().lexemeList
        assertEquals("промоут НА МЕСТЕ черновика (сверху)", 55L, list[0].id)
        assertEquals(8L, list[1].id)
    }
}
