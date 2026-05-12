package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.assertState
import me.apomazkin.mate.test.testReduce
import me.apomazkin.mate.test.toBuilder
import me.apomazkin.wordcard.entity.Definition
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.LexemeId
import me.apomazkin.wordcard.entity.Term
import me.apomazkin.wordcard.entity.Translation
import me.apomazkin.wordcard.entity.Word
import me.apomazkin.wordcard.entity.WordId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Test cases:
 * 1. Boundary case: WordLoaded with empty lexeme list - should update word state and create empty lexeme list
 * 2. Standard case: WordLoaded with single lexeme containing both translation and definition
 * 3. Standard case: WordLoaded with multiple lexemes with mixed translation/definition presence
 * 4. Edge case: WordLoaded with lexeme having only translation (no definition)
 * 5. Edge case: WordLoaded with lexeme having only definition (no translation)
 * 6. Edge case: WordLoaded with lexeme having neither translation nor definition
 * 7. Boundary case: WordLoaded with null dates - should handle gracefully
 */
class WordLoadedTest {

    @Test
    fun `should handle WordLoaded message with empty lexeme list`() {
        // Test case 1: Boundary case - WordLoaded with empty lexeme list should update word state and create empty lexeme list
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState()
            .toBuilder()
            .modify { it.copy(isLoading = true) }
            .modify { it.copy(topBarState = TopBarState(isMenuOpen = false)) }
            .modify { it.copy(addLexemeBottomState = AddLexemeBottomState(show = false)) }
            .modify { it.copy(wordState = WordState()) }
            .modify { it.copy(lexemeList = listOf()) }
            .modify { it.copy(snackbarState = SnackbarState()) }
            .build()
        
        val term = Term(
            wordId = WordId(123L),
            word = Word("test"),
            addedDate = Date(1000L),
            changedDate = null,
            removedDate = null,
            lexemeList = emptyList()
        )
        
        val message = Msg.WordLoaded(term)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        val expectedState = initialState
            .toBuilder()
            .modify { it.copy(isLoading = false) }
            .modify { it.copy(wordState = WordState(id = 123L, value = "test", added = Date(1000L))) }
            .modify { it.copy(lexemeList = emptyList()) }
            .build()
        
        result.assertState(expectedState, "State should be updated with word data and empty lexeme list")
        
        // Check specific state changes
        assertFalse("Loading should be disabled", result.state().isLoading)
        assertEquals("Word ID should be set", 123L, result.state().wordState.id)
        assertEquals("Word value should be set", "test", result.state().wordState.value)
        assertEquals("Word added date should be set", Date(1000L), result.state().wordState.added)
        assertTrue("Lexeme list should be empty", result.state().lexemeList.isEmpty())
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "TopBarState should remain unchanged",
            initialState.topBarState,
            result.state().topBarState
        )
        assertEquals(
            "AddLexemeBottomState should remain unchanged",
            initialState.addLexemeBottomState,
            result.state().addLexemeBottomState
        )
        assertEquals(
            "SnackbarState should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        
        // Effects check using mate helper
        result.assertNoEffects("Should have no effects for WordLoaded message")
    }

    @Test
    fun `should handle WordLoaded message with single lexeme containing both translation and definition`() {
        // Test case 2: Standard case - WordLoaded with single lexeme containing both translation and definition
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState()
        
        val lexeme = Lexeme(
            lexemeId = LexemeId(456L),
            translation = Translation("перевод"),
            definition = Definition("определение"),
            category = "noun",
            addDate = Date(2000L),
            changeDate = null
        )
        
        val term = Term(
            wordId = WordId(123L),
            word = Word("test"),
            addedDate = Date(1000L),
            changedDate = null,
            removedDate = null,
            lexemeList = listOf(lexeme)
        )
        
        val message = Msg.WordLoaded(term)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertEquals("Should have one lexeme", 1, result.state().lexemeList.size)
        
        val resultLexeme = result.state().lexemeList.first()
        assertEquals("Lexeme ID should match", 456L, resultLexeme.id)
        assertEquals("Translation should be set", "перевод", resultLexeme.translation?.origin)
        assertFalse("Translation should not be in edit mode", resultLexeme.translation?.isEdit ?: true)
        assertEquals("Definition should be set", "определение", resultLexeme.definition?.origin)
        assertFalse("Definition should not be in edit mode", resultLexeme.definition?.isEdit ?: true)
        assertFalse("Lexeme menu should be closed", resultLexeme.isMenuOpen)
        
        // Immutability checks
        assertFalse("Loading should be disabled", result.state().isLoading)
        assertEquals("Word ID should be set", 123L, result.state().wordState.id)
        assertEquals("Word value should be set", "test", result.state().wordState.value)
        assertEquals("Word added date should be set", Date(1000L), result.state().wordState.added)
    }

    @Test
    fun `should handle WordLoaded message with lexeme having only translation`() {
        // Test case 4: Edge case - WordLoaded with lexeme having only translation (no definition)
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState()
        
        val lexeme = Lexeme(
            lexemeId = LexemeId(456L),
            translation = Translation("перевод"),
            definition = null,
            category = "verb",
            addDate = Date(2000L),
            changeDate = null
        )
        
        val term = Term(
            wordId = WordId(123L),
            word = Word("test"),
            addedDate = Date(1000L),
            changedDate = null,
            removedDate = null,
            lexemeList = listOf(lexeme)
        )
        
        val message = Msg.WordLoaded(term)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        val resultLexeme = result.state().lexemeList.first()
        assertTrue("Translation should be present", resultLexeme.translation != null)
        assertTrue("Definition should be null", resultLexeme.definition == null)
        assertEquals("Translation value should match", "перевод", resultLexeme.translation?.origin)
        assertFalse("Translation should not be in edit mode", resultLexeme.translation?.isEdit ?: true)
        
        // Immutability checks
        assertEquals(
            "Loading state should be set to false",
            false,
            result.state().isLoading
        )
        assertEquals(
            "Word ID should be set from term",
            123L,
            result.state().wordState.id
        )
        assertEquals(
            "Word value should be set from term",
            "test",
            result.state().wordState.value
        )
    }

    @Test
    fun `should handle WordLoaded message with lexeme having only definition`() {
        // Test case 5: Edge case - WordLoaded with lexeme having only definition (no translation)
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState()
        
        val lexeme = Lexeme(
            lexemeId = LexemeId(456L),
            translation = null,
            definition = Definition("определение"),
            category = "adjective",
            addDate = Date(2000L),
            changeDate = null
        )
        
        val term = Term(
            wordId = WordId(123L),
            word = Word("test"),
            addedDate = Date(1000L),
            changedDate = null,
            removedDate = null,
            lexemeList = listOf(lexeme)
        )
        
        val message = Msg.WordLoaded(term)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        val resultLexeme = result.state().lexemeList.first()
        assertTrue("Translation should be null", resultLexeme.translation == null)
        assertTrue("Definition should be present", resultLexeme.definition != null)
        assertEquals("Definition value should match", "определение", resultLexeme.definition?.origin)
        assertFalse("Definition should not be in edit mode", resultLexeme.definition?.isEdit ?: true)
        
        // Immutability checks
        assertEquals(
            "Lexeme ID should be set correctly",
            456L,
            resultLexeme.id
        )
        assertFalse("Lexeme menu should be closed", resultLexeme.isMenuOpen)
    }

    @Test
    fun `should handle WordLoaded message with lexeme having neither translation nor definition`() {
        // Test case 6: Edge case - WordLoaded with lexeme having neither translation nor definition
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState()
        
        val lexeme = Lexeme(
            lexemeId = LexemeId(456L),
            translation = null,
            definition = null,
            category = "interjection",
            addDate = Date(2000L),
            changeDate = null
        )
        
        val term = Term(
            wordId = WordId(123L),
            word = Word("test"),
            addedDate = Date(1000L),
            changedDate = null,
            removedDate = null,
            lexemeList = listOf(lexeme)
        )
        
        val message = Msg.WordLoaded(term)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        val resultLexeme = result.state().lexemeList.first()
        assertTrue("Translation should be null", resultLexeme.translation == null)
        assertTrue("Definition should be null", resultLexeme.definition == null)
        assertEquals("Lexeme ID should be set correctly", 456L, resultLexeme.id)
        assertFalse("Lexeme menu should be closed", resultLexeme.isMenuOpen)
        
        // Immutability checks
        assertEquals(
            "Word state should be updated correctly",
            123L,
            result.state().wordState.id
        )
        assertEquals(
            "Word value should be updated correctly",
            "test",
            result.state().wordState.value
        )
    }

    @Test
    fun `should handle WordLoaded message with multiple lexemes with mixed properties`() {
        // Test case 3: Standard case - WordLoaded with multiple lexemes with mixed translation/definition presence
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState()
        
        val lexeme1 = Lexeme(
            lexemeId = LexemeId(1L),
            translation = Translation("первый перевод"),
            definition = Definition("первое определение"),
            category = "noun",
            addDate = Date(1000L),
            changeDate = null
        )
        
        val lexeme2 = Lexeme(
            lexemeId = LexemeId(2L),
            translation = Translation("второй перевод"),
            definition = null,
            category = "verb",
            addDate = Date(2000L),
            changeDate = null
        )
        
        val lexeme3 = Lexeme(
            lexemeId = LexemeId(3L),
            translation = null,
            definition = Definition("третье определение"),
            category = "adjective",
            addDate = Date(3000L),
            changeDate = null
        )
        
        val term = Term(
            wordId = WordId(123L),
            word = Word("test"),
            addedDate = Date(1000L),
            changedDate = null,
            removedDate = null,
            lexemeList = listOf(lexeme1, lexeme2, lexeme3)
        )
        
        val message = Msg.WordLoaded(term)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertEquals("Should have three lexemes", 3, result.state().lexemeList.size)
        
        val resultLexeme1 = result.state().lexemeList[0]
        assertEquals("First lexeme ID should match", 1L, resultLexeme1.id)
        assertEquals("First lexeme translation should be set", "первый перевод", resultLexeme1.translation?.origin)
        assertEquals("First lexeme definition should be set", "первое определение", resultLexeme1.definition?.origin)
        
        val resultLexeme2 = result.state().lexemeList[1]
        assertEquals("Second lexeme ID should match", 2L, resultLexeme2.id)
        assertEquals("Second lexeme translation should be set", "второй перевод", resultLexeme2.translation?.origin)
        assertTrue("Second lexeme definition should be null", resultLexeme2.definition == null)
        
        val resultLexeme3 = result.state().lexemeList[2]
        assertEquals("Third lexeme ID should match", 3L, resultLexeme3.id)
        assertTrue("Third lexeme translation should be null", resultLexeme3.translation == null)
        assertEquals("Third lexeme definition should be set", "третье определение", resultLexeme3.definition?.origin)
        
        // Immutability checks
        assertFalse("Loading should be disabled", result.state().isLoading)
        assertEquals("Word ID should be set", 123L, result.state().wordState.id)
        assertEquals("Word value should be set", "test", result.state().wordState.value)
        assertEquals("Word added date should be set", Date(1000L), result.state().wordState.added)
    }

    @Test
    fun `should handle WordLoaded message with null dates gracefully`() {
        // Test case 7: Boundary case - WordLoaded with null dates should handle gracefully
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState()
        
        val term = Term(
            wordId = WordId(123L),
            word = Word("test"),
            addedDate = Date(1000L),
            changedDate = null,
            removedDate = null,
            lexemeList = emptyList()
        )
        
        val message = Msg.WordLoaded(term)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertEquals("Word added date should be set", Date(1000L), result.state().wordState.added)
        assertFalse("Loading should be disabled", result.state().isLoading)
        assertEquals("Word ID should be set", 123L, result.state().wordState.id)
        assertEquals("Word value should be set", "test", result.state().wordState.value)
        
        // Immutability checks
        assertEquals(
            "TopBarState should remain unchanged",
            initialState.topBarState,
            result.state().topBarState
        )
        assertEquals(
            "AddLexemeBottomState should remain unchanged",
            initialState.addLexemeBottomState,
            result.state().addLexemeBottomState
        )
        assertEquals(
            "SnackbarState should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
    }
}
