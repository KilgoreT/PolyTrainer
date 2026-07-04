package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * §8.4 CreateLexeme — prepend NOT_IN_DB черновик, single-draft guard, pending guard,
 * commit открытых правок перед созданием.
 */
class CreateLexemeTest {

    private val reducer = WordCardReducer()

    @Test
    fun `CreateLexeme prepends NOT_IN_DB draft on top`() {
        val initial = loaded(lexemes = listOf(lexeme(8L, listOf(savedCv(5L)))))
        val result = reducer.testReduce(initial, Msg.CreateLexeme)
        val list = result.state().lexemeList
        assertEquals("черновик сверху", NOT_IN_DB, list.first().id)
        assertEquals("существующая на месте", 8L, list[1].id)
    }

    @Test
    fun `CreateLexeme no-op when draft already exists (single-draft)`() {
        val initial = loaded(lexemes = listOf(lexeme(NOT_IN_DB, emptyList())))
        val result = reducer.testReduce(initial, Msg.CreateLexeme)
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `CreateLexeme guarded by pending`() {
        val initial = loaded(isPendingDbOp = true, lexemes = listOf(lexeme(8L, emptyList())))
        val result = reducer.testReduce(initial, Msg.CreateLexeme)
        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `CreateLexeme commits open edits then adds draft`() {
        val initial = loaded(
            lexemes = listOf(lexeme(8L, listOf(savedCv(5L, origin = "old", isEdit = true, edited = "new")))),
        )
        val result = reducer.testReduce(initial, Msg.CreateLexeme)
        assertTrue("черновик добавлен", result.state().lexemeList.any { it.id == NOT_IN_DB })
        assertTrue("commit-эффект эмитнут", result.state().isPendingDbOp)
    }
}
