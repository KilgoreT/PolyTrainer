package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Standard case: LoadingWord enables loading and triggers LoadWord effect
 * 2. Standard case: LoadingWord with existing word ID - should trigger LoadWord with correct ID
 * 3. Boundary case: LoadingWord when already loading - should enable loading and trigger effect
 * 4. Standard case: LoadingWord with complex state - should only affect loading and trigger effect
 * 5. Edge case: LoadingWord with word ID -1 (NOT_IN_DB) - should trigger LoadWord effect
 */
class LoadingWordTest {

    @Test
    fun `should enable loading and trigger LoadWord effect when LoadingWord is received`() {
        // Test case 1: Standard case - LoadingWord enables loading and triggers LoadWord effect
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false, // Not loading initially
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.LoadingWord
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Loading should be enabled",
            result.state().isLoading
        )
        
        // Effects check
        result.assertEffects(
            setOf(DatasourceEffect.LoadWord(wordId = 123L)),
            "Should trigger LoadWord effect with correct word ID"
        )
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "topBarState should remain unchanged",
            initialState.topBarState,
            result.state().topBarState
        )
        assertEquals(
            "addLexemeBottomState should remain unchanged",
            initialState.addLexemeBottomState,
            result.state().addLexemeBottomState
        )
        assertEquals(
            "wordState should remain unchanged",
            initialState.wordState,
            result.state().wordState
        )
        assertEquals(
            "lexemeList should remain unchanged",
            initialState.lexemeList,
            result.state().lexemeList
        )
        assertEquals(
            "snackbarState should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
    }

    @Test
    fun `should trigger LoadWord with correct word ID when LoadingWord is received`() {
        // Test case 2: Standard case - LoadingWord with existing word ID should trigger LoadWord with correct ID
        // Given
        val reducer = WordCardReducer()
        val wordId = 456L
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = wordId, value = "another test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.LoadingWord
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Loading should be enabled",
            result.state().isLoading
        )
        
        // Effects check - should use the word ID from state
        result.assertEffects(
            setOf(DatasourceEffect.LoadWord(wordId = wordId)),
            "Should trigger LoadWord effect with word ID from state"
        )
        
        // Word state should remain unchanged except for loading
        assertEquals(
            "Word ID should remain unchanged",
            wordId,
            result.state().wordState.id
        )
        assertEquals(
            "Word value should remain unchanged",
            "another test",
            result.state().wordState.value
        )
    }

    @Test
    fun `should enable loading when already loading and trigger effect`() {
        // Test case 3: Boundary case - LoadingWord when already loading should enable loading and trigger effect
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = true, // Already loading
            wordState = WordState(id = 789L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.LoadingWord
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Loading should remain enabled",
            result.state().isLoading
        )
        
        // Effects check - should still trigger LoadWord effect
        result.assertEffects(
            setOf(DatasourceEffect.LoadWord(wordId = 789L)),
            "Should trigger LoadWord effect even when already loading"
        )
        
        // Immutability checks
        assertEquals(
            "topBarState should remain unchanged",
            initialState.topBarState,
            result.state().topBarState
        )
        assertEquals(
            "wordState should remain unchanged",
            initialState.wordState,
            result.state().wordState
        )
    }

    @Test
    fun `should handle LoadingWord with complex state`() {
        // Test case 4: Standard case - LoadingWord with complex state should only affect loading and trigger effect
        // Given
        val reducer = WordCardReducer()
        val lexemeList = listOf(
            LexemeState(
                id = 1L,
                translation = TextValueState(origin = "translation", isEdit = false),
                definition = TextValueState(origin = "definition", isEdit = true),
                isMenuOpen = true
            ),
            LexemeState(
                id = 2L,
                translation = null,
                definition = TextValueState(origin = "def2", isEdit = false),
                isMenuOpen = false
            )
        )
        
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true),
            addLexemeBottomState = AddLexemeBottomState(
                show = true,
                isTranslationCheck = true,
                isDefinitionCheck = false
            ),
            isLoading = false,
            wordState = WordState(
                id = 456L,
                value = "complex word",
                isEditMode = true,
                edited = "edited complex word",
                showWarningDialog = true,
            ),
            lexemeList = lexemeList,
            snackbarState = SnackbarState(title = "Test message", show = true)
        )
        
        val message = Msg.LoadingWord
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Loading should be enabled",
            result.state().isLoading
        )
        
        // Effects check
        result.assertEffects(
            setOf(DatasourceEffect.LoadWord(wordId = 456L)),
            "Should trigger LoadWord effect with correct word ID"
        )
        
        // Immutability checks - ALL complex state must remain unchanged
        assertEquals(
            "topBarState should remain unchanged",
            initialState.topBarState,
            result.state().topBarState
        )
        assertEquals(
            "addLexemeBottomState should remain unchanged",
            initialState.addLexemeBottomState,
            result.state().addLexemeBottomState
        )
        assertEquals(
            "wordState should remain unchanged",
            initialState.wordState,
            result.state().wordState
        )
        assertEquals(
            "lexemeList should remain unchanged",
            initialState.lexemeList,
            result.state().lexemeList
        )
        assertEquals(
            "snackbarState should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
    }

    @Test
    fun `should trigger LoadWord effect with NOT_IN_DB word ID`() {
        // Test case 5: Edge case - LoadingWord with word ID -1 (NOT_IN_DB) should trigger LoadWord effect
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = -1L, value = ""), // NOT_IN_DB
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.LoadingWord
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Loading should be enabled",
            result.state().isLoading
        )
        
        // Effects check - should trigger LoadWord even with NOT_IN_DB ID
        result.assertEffects(
            setOf(DatasourceEffect.LoadWord(wordId = -1L)),
            "Should trigger LoadWord effect even with NOT_IN_DB word ID"
        )
        
        // State should remain unchanged except for loading
        assertEquals(
            "Word ID should remain NOT_IN_DB",
            -1L,
            result.state().wordState.id
        )
        assertEquals(
            "Word value should remain empty",
            "",
            result.state().wordState.value
        )
    }
}
