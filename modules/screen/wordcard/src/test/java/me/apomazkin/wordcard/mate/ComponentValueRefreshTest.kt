package me.apomazkin.wordcard.mate

import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §1.6 RefreshLexemeComponents (B4 union-by-id, порядок G7) + §1.7 ComponentValueInserted.
 */
class ComponentValueRefreshTest {

    private val reducer = WordCardReducer()
    private fun savedK(id: Long) = ComponentValueKey.Saved(ComponentValueId(id))

    // ---------- RefreshLexemeComponents ----------

    @Test
    fun `overwrites saved origin by id`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L, origin = "old")))))
        val result = reducer.testReduce(initial, Msg.RefreshLexemeComponents(7L, listOf(domainCv(5L, 7L, "fresh"))))
        val cv = result.state().lexemeList.single().components.single()
        assertEquals("fresh", cv.origin)
        assertFalse(cv.isEdit)
        assertFalse(result.state().isPendingDbOp)
    }

    @Test
    fun `A10 refresh closes ONLY committing component`() {
        val initial = loaded(
            lexemes = listOf(
                lexeme(
                    7L,
                    listOf(
                        savedCv(5L, origin = "o5", isEdit = true, isCommitting = true, edited = "t5"),
                        savedCv(6L, origin = "o6", isEdit = true, isCommitting = false, edited = "t6"),
                    ),
                ),
            ),
        )
        val result = reducer.testReduce(
            initial,
            Msg.RefreshLexemeComponents(7L, listOf(domainCv(5L, 7L, "db5"), domainCv(6L, 7L, "db6"))),
        )
        val comps = result.state().lexemeList.single().components
        val five = comps.first { it.key == savedK(5L) }
        val six = comps.first { it.key == savedK(6L) }
        assertFalse("#5 закрыт", five.isEdit)
        assertFalse(five.isCommitting)
        assertEquals("db5", five.origin)
        assertTrue("#6 редактируемый сохранён", six.isEdit)
        assertEquals("t6", six.edited)
        assertEquals("db6", six.origin)
    }

    @Test
    fun `B4_keeps_in_flight_pristine_in_tail`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L), pristineCv(10L, edited = "draft")))))
        val result = reducer.testReduce(initial, Msg.RefreshLexemeComponents(7L, listOf(domainCv(5L, 7L, "x"))))
        val comps = result.state().lexemeList.single().components
        assertEquals("pristine в хвосте", ComponentValueKey.Pristine(10L), comps.last().key)
        assertEquals("draft", comps.last().edited)
    }

    @Test
    fun `removes saved absent from payload`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L), savedCv(6L)))))
        val result = reducer.testReduce(initial, Msg.RefreshLexemeComponents(7L, listOf(domainCv(5L, 7L, "x"))))
        val comps = result.state().lexemeList.single().components
        assertEquals(1, comps.size)
        assertEquals(savedK(5L), comps.single().key)
    }

    @Test
    fun `adds new saved from payload`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L)))))
        val result = reducer.testReduce(initial, Msg.RefreshLexemeComponents(7L, listOf(domainCv(5L, 7L, "a"), domainCv(7L, 7L, "b"))))
        assertEquals(2, result.state().lexemeList.single().components.size)
    }

    @Test
    fun `B4_multi_race_keeps_pristine_adds_saved_tail_order`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L), pristineCv(10L, edited = "x")))))
        val result = reducer.testReduce(initial, Msg.RefreshLexemeComponents(7L, listOf(domainCv(5L, 7L, "a"), domainCv(7L, 7L, "b"))))
        val comps = result.state().lexemeList.single().components
        assertEquals("saved сначала (5,7), pristine 10 в хвосте — G7", listOf(savedK(5L), savedK(7L), ComponentValueKey.Pristine(10L)), comps.map { it.key })
    }

    @Test
    fun `clears pending`() {
        val initial = loaded(isPendingDbOp = true, lexemes = listOf(lexeme(7L, listOf(savedCv(5L)))))
        val result = reducer.testReduce(initial, Msg.RefreshLexemeComponents(7L, listOf(domainCv(5L, 7L, "x"))))
        assertFalse(result.state().isPendingDbOp)
    }

    @Test
    fun `unknown lexemeId clears pending list untouched`() {
        val initial = loaded(isPendingDbOp = true, lexemes = listOf(lexeme(7L, listOf(savedCv(5L)))))
        val result = reducer.testReduce(initial, Msg.RefreshLexemeComponents(999L, listOf(domainCv(5L, 7L, "x"))))
        assertEquals(initial.lexemeList, result.state().lexemeList)
        assertFalse(result.state().isPendingDbOp)
    }

    @Test
    fun `B1_keeps_saved_value_when_types_not_yet_loaded`() {
        val initial = loaded(availableTypes = emptyList(), lexemes = listOf(lexeme(7L, listOf(savedCv(5L, typeId = 99L)))))
        val result = reducer.testReduce(initial, Msg.RefreshLexemeComponents(7L, listOf(domainCv(5L, 7L, "x", typeId = 99L))))
        assertTrue(result.state().lexemeList.single().components.any { it.key == savedK(5L) })
    }

    // ---------- ComponentValueInserted ----------

    @Test
    fun `flips pristine to saved`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(pristineCv(10L, typeId = 50L)))))
        val result = reducer.testReduce(initial, Msg.ComponentValueInserted(7L, 10L, ComponentValueId(77L)))
        val cv = result.state().lexemeList.single().components.single()
        assertEquals(savedK(77L), cv.key)
        assertFalse(cv.isPristine)
        result.assertNoEffects()
    }

    @Test
    fun `idempotent for nonexistent pristineKey`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(5L)))))
        val result = reducer.testReduce(initial, Msg.ComponentValueInserted(7L, 999L, ComponentValueId(77L)))
        assertEquals(initial, result.state())
    }

    @Test
    fun `flip closes edit keeps edited text`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(pristineCv(10L, edited = "x")))))
        val result = reducer.testReduce(initial, Msg.ComponentValueInserted(7L, 10L, ComponentValueId(77L)))
        val cv = result.state().lexemeList.single().components.single()
        assertEquals(savedK(77L), cv.key)
        assertEquals("x", cv.edited)
        assertFalse(cv.isEdit)
        assertFalse(cv.isCommitting)
    }

    @Test
    fun `inserted dedup removes pristine when saved already present`() {
        val initial = loaded(lexemes = listOf(lexeme(7L, listOf(savedCv(77L), pristineCv(10L, edited = "x")))))
        val result = reducer.testReduce(initial, Msg.ComponentValueInserted(7L, 10L, ComponentValueId(77L)))
        val comps = result.state().lexemeList.single().components
        assertEquals("дубль не создан", 1, comps.size)
        assertEquals(savedK(77L), comps.single().key)
    }

    @Test
    fun `inserted not guarded by pending`() {
        val initial = loaded(isPendingDbOp = true, lexemes = listOf(lexeme(7L, listOf(pristineCv(10L)))))
        val result = reducer.testReduce(initial, Msg.ComponentValueInserted(7L, 10L, ComponentValueId(77L)))
        assertEquals(savedK(77L), result.state().lexemeList.single().components.single().key)
    }
}
