package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import me.apomazkin.lexeme.Definition
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.LexemeId
import me.apomazkin.lexeme.Translation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Reducer tests for Lexeme lifecycle per contract_spec § "Тестовые сценарии":
 * - CreateLexeme локально (append NOT_IN_DB, без Effect)
 * - CreateLexeme guard (повторный → state to emptySet())
 * - RefreshLexemeList — заменяет lexemeList, сохраняет NOT_IN_DB, isPendingDbOp=false
 * - LexemeCascadeRemoved — лексема удалена из списка, isPendingDbOp=false
 * - RemoveLexeme — реальный id даёт Effect + isPendingDbOp=true; NOT_IN_DB — локально
 * - Глобальный guard isPendingDbOp на RemoveLexeme.
 */
class LexemeManagementTest {

    private fun loadedState(
        wordId: Long = 1L,
        lexemes: List<LexemeState> = emptyList(),
        isPendingDbOp: Boolean = false,
    ): WordCardState = WordCardState(
        isLoading = false,
        isPendingDbOp = isPendingDbOp,
        wordState = WordState.Loaded(
            id = wordId,
            added = Date(0L),
            value = "word",
        ),
        lexemeList = lexemes,
    )

    @Test
    fun `given Loaded when CreateLexeme then prepends NOT_IN_DB lexeme without Effect`() {
        val reducer = WordCardReducer()
        val initial = loadedState(lexemes = listOf(
            LexemeState(id = 5L, translation = TextValueState(origin = "a", isEdit = false)),
        ))

        val result = reducer.testReduce(initial, Msg.CreateLexeme)

        val list = result.state().lexemeList
        assertEquals("list size +1", 2, list.size)
        val created = list.first()
        assertEquals("created id is NOT_IN_DB at the top", NOT_IN_DB, created.id)
        assertNull("translation null on create", created.translation)
        assertNull("definition null on create", created.definition)
        result.assertNoEffects("CreateLexeme is purely local")
    }

    @Test
    fun `given existing NOT_IN_DB lexeme when CreateLexeme then state unchanged`() {
        val reducer = WordCardReducer()
        val initial = loadedState(lexemes = listOf(
            LexemeState(id = NOT_IN_DB, translation = null, definition = null),
        ))

        val result = reducer.testReduce(initial, Msg.CreateLexeme)

        assertEquals("state should be unchanged (guard)", initial, result.state())
        result.assertNoEffects("CreateLexeme guard yields no effects")
    }

    @Test
    fun `given NotLoaded when CreateLexeme then state unchanged`() {
        val reducer = WordCardReducer()
        val initial = WordCardState(isLoading = false, wordState = WordState.NotLoaded)

        val result = reducer.testReduce(initial, Msg.CreateLexeme)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `given Loaded when RemoveLexeme for real id then emits RemoveLexeme effect and sets pending`() {
        val reducer = WordCardReducer()
        val initial = loadedState(
            wordId = 7L,
            lexemes = listOf(
                LexemeState(id = 42L, translation = TextValueState(origin = "x", isEdit = false)),
            ),
        )

        val result = reducer.testReduce(initial, Msg.RemoveLexeme(lexemeId = 42L))

        assertTrue("isPendingDbOp set", result.state().isPendingDbOp)
        result.assertEffects(
            setOf(DatasourceEffect.RemoveLexeme(wordId = 7L, lexemeId = 42L)),
            "RemoveLexeme effect expected",
        )
    }

    @Test
    fun `given Loaded when RemoveLexeme for NOT_IN_DB then locally removes without Effect`() {
        val reducer = WordCardReducer()
        val initial = loadedState(lexemes = listOf(
            LexemeState(id = NOT_IN_DB, translation = null, definition = null),
        ))

        val result = reducer.testReduce(initial, Msg.RemoveLexeme(lexemeId = NOT_IN_DB))

        assertTrue("lexeme removed", result.state().lexemeList.isEmpty())
        assertFalse("isPendingDbOp not set", result.state().isPendingDbOp)
        result.assertNoEffects("local cascade has no Effect")
    }

    @Test
    fun `given isPendingDbOp true when RemoveLexeme then state unchanged (global guard)`() {
        val reducer = WordCardReducer()
        val initial = loadedState(
            lexemes = listOf(LexemeState(id = 1L, translation = TextValueState(origin = "x"))),
            isPendingDbOp = true,
        )

        val result = reducer.testReduce(initial, Msg.RemoveLexeme(lexemeId = 1L))

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `when RefreshLexemeList then replaces lexemeList and clears pending`() {
        val reducer = WordCardReducer()
        val initial = loadedState(isPendingDbOp = true, lexemes = listOf(
            LexemeState(id = 1L, translation = TextValueState(origin = "a")),
        ))
        val newLexemes = listOf(
            Lexeme(LexemeId(2L), Translation("b"), null, Date(0)),
            Lexeme(LexemeId(3L), null, Definition("c"), Date(0)),
        )

        val result = reducer.testReduce(initial, Msg.RefreshLexemeList(lexemes = newLexemes))

        val state = result.state()
        assertFalse("isPendingDbOp cleared", state.isPendingDbOp)
        assertEquals("list replaced", 2, state.lexemeList.size)
        assertEquals(2L, state.lexemeList[0].id)
        assertEquals("b", state.lexemeList[0].translation?.origin)
        assertEquals(3L, state.lexemeList[1].id)
        assertEquals("c", state.lexemeList[1].definition?.origin)
        result.assertNoEffects()
    }

}
