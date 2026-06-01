package me.apomazkin.wordcard.mate

import me.apomazkin.mate.NavigationEffect
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.assertSingleEffect
import me.apomazkin.mate.test.testReduce
import me.apomazkin.lexeme.Definition
import me.apomazkin.lexeme.Lexeme
import me.apomazkin.lexeme.LexemeId
import me.apomazkin.lexeme.Translation
import me.apomazkin.wordcard.entity.Term
import me.apomazkin.wordcard.entity.Word
import me.apomazkin.wordcard.entity.WordId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Reducer tests for WordLoaded / WordNotFound — first scenarios in contract_spec § "Тестовые сценарии".
 *
 * Test cases:
 * 1. WordLoaded атомарность: (NotLoaded, isLoading=true) → WordLoaded(term) → Loaded(...), isLoading=false,
 *    isPendingDbOp=false, lexemeList = term.lexemeList.map { toLexemeState() }.
 * 2. WordLoaded — empty lexeme list.
 * 3. WordLoaded — multiple lexemes mapped через toLexemeState (translation/definition origin сохранены, edited="", isEdit=false).
 * 4. WordNotFound silent exit: (NotLoaded, isLoading=true) → NavigationEffect.Back; snackbar НЕ устанавливается.
 */
class WordLoadedTest {

    @Test
    fun `given NotLoaded with isLoading when WordLoaded then atomically transitions to Loaded with mapped lexemes and clears pending`() {
        // Test case 1: атомарность WordLoaded.
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            isLoading = true,
            isPendingDbOp = false,
            wordState = WordState.NotLoaded,
            lexemeList = emptyList(),
        )
        val lexeme = Lexeme(
            lexemeId = LexemeId(10L),
            translation = Translation("hello"),
            definition = Definition("greeting"),
            addDate = Date(2000L),
            changeDate = null,
        )
        val term = Term(
            wordId = WordId(123L),
            word = Word("test"),
            addedDate = Date(1000L),
            changedDate = null,
            removedDate = null,
            lexemeList = listOf(lexeme),
        )

        val result = reducer.testReduce(initialState, Msg.WordLoaded(term))

        val state = result.state()
        assertFalse("isLoading should be false", state.isLoading)
        assertFalse("isPendingDbOp should be false", state.isPendingDbOp)
        assertTrue("wordState should be Loaded", state.wordState is WordState.Loaded)
        val loaded = state.wordState as WordState.Loaded
        assertEquals("id should match term", 123L, loaded.id)
        assertEquals("value should match term", "test", loaded.value)
        assertEquals("added should match term", Date(1000L), loaded.added)
        assertFalse("isEditMode default false", loaded.isEditMode)
        assertEquals("edited default empty", "", loaded.edited)
        assertFalse("showWarningDialog default false", loaded.showWarningDialog)

        assertEquals("lexemeList size", 1, state.lexemeList.size)
        val lex = state.lexemeList.first()
        assertEquals("lexeme id", 10L, lex.id)
        assertEquals("translation origin", "hello", lex.translation?.origin)
        assertEquals("translation edited empty (per mapper)", "", lex.translation?.edited)
        assertFalse("translation isEdit false", lex.translation?.isEdit ?: true)
        assertEquals("definition origin", "greeting", lex.definition?.origin)
        assertEquals("definition edited empty (per mapper)", "", lex.definition?.edited)
        assertFalse("definition isEdit false", lex.definition?.isEdit ?: true)

        result.assertNoEffects("WordLoaded should not produce effects")
    }

    @Test
    fun `given NotLoaded when WordLoaded with empty lexeme list then lexemeList is empty`() {
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            isLoading = true,
            wordState = WordState.NotLoaded,
        )
        val term = Term(
            wordId = WordId(1L),
            word = Word("w"),
            addedDate = Date(1L),
            changedDate = null,
            removedDate = null,
            lexemeList = emptyList(),
        )

        val result = reducer.testReduce(initialState, Msg.WordLoaded(term))

        assertTrue(result.state().lexemeList.isEmpty())
        assertTrue(result.state().wordState is WordState.Loaded)
    }

    @Test
    fun `given multiple lexemes when WordLoaded then all are mapped to LexemeState with empty edited`() {
        val reducer = WordCardReducer()
        val initialState = WordCardState(isLoading = true, wordState = WordState.NotLoaded)
        val lexemes = listOf(
            Lexeme(LexemeId(1L), Translation("a"), null, Date(0)),
            Lexeme(LexemeId(2L), null, Definition("b"), Date(0)),
            Lexeme(LexemeId(3L), Translation("c"), Definition("d"), Date(0)),
        )
        val term = Term(
            wordId = WordId(99L),
            word = Word("w"),
            addedDate = Date(0L),
            changedDate = null,
            removedDate = null,
            lexemeList = lexemes,
        )

        val result = reducer.testReduce(initialState, Msg.WordLoaded(term))

        val list = result.state().lexemeList
        assertEquals(3, list.size)
        assertEquals("", list[0].translation?.edited)
        assertEquals("a", list[0].translation?.origin)
        assertEquals(null, list[1].translation)
        assertEquals("b", list[1].definition?.origin)
        assertEquals("c", list[2].translation?.origin)
        assertEquals("d", list[2].definition?.origin)
    }

    @Test
    fun `given NotLoaded when WordNotFound then emits NavigationEffect Back and clears pending`() {
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            isLoading = true,
            isPendingDbOp = true,
            wordState = WordState.NotLoaded,
        )

        val result = reducer.testReduce(initialState, Msg.WordNotFound)

        val state = result.state()
        assertFalse("isLoading should be false", state.isLoading)
        assertFalse("isPendingDbOp should be false", state.isPendingDbOp)
        result.assertSingleEffect<NavigationEffect.Back>()
    }
}
