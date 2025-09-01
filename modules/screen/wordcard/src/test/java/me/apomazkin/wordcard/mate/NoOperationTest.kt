package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Standard case: NoOperation does nothing - state remains unchanged
 * 2. Standard case: NoOperation with complex state - all state remains unchanged
 * 3. Standard case: NoOperation while loading - loading state remains unchanged
 * 4. Standard case: NoOperation with active dialogs - dialog states remain unchanged
 */
class NoOperationTest {

    @Test
    fun `should not change state when NoOperation is received`() {
        // Test case 1: Standard case - NoOperation does nothing - state remains unchanged
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.NoOperation
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check - state should remain completely unchanged
        assertEquals(
            "State should remain completely unchanged",
            initialState,
            result.state()
        )
        
        // Effects check
        result.assertNoEffects("NoOperation should not produce any effects")
        
        // Individual property checks for clarity
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
            "closeScreen should remain unchanged",
            initialState.closeScreen,
            result.state().closeScreen
        )
        assertEquals(
            "isLoading should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
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
    fun `should not change complex state when NoOperation is received`() {
        // Test case 2: Standard case - NoOperation with complex state - all state remains unchanged
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
            closeScreen = false,
            isLoading = true,
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
        
        val message = Msg.NoOperation
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check - entire complex state should remain unchanged
        assertEquals(
            "Complex state should remain completely unchanged",
            initialState,
            result.state()
        )
        
        // Effects check
        result.assertNoEffects("NoOperation should not produce any effects")
        
        // Detailed property checks for complex state
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
            "closeScreen should remain unchanged",
            initialState.closeScreen,
            result.state().closeScreen
        )
        assertEquals(
            "isLoading should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
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
    fun `should not change loading state when NoOperation is received while loading`() {
        // Test case 3: Standard case - NoOperation while loading - loading state remains unchanged
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = true, // Currently loading
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.NoOperation
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertEquals(
            "State should remain completely unchanged",
            initialState,
            result.state()
        )
        
        // Specific loading state check
        assertTrue(
            "Loading state should remain true",
            result.state().isLoading
        )
        
        // Effects check
        result.assertNoEffects("NoOperation should not produce any effects")
    }

    @Test
    fun `should not change dialog states when NoOperation is received with active dialogs`() {
        // Test case 4: Standard case - NoOperation with active dialogs - dialog states remain unchanged
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true), // Menu open
            addLexemeBottomState = AddLexemeBottomState(
                show = true, // Dialog open
                isTranslationCheck = true,
                isDefinitionCheck = false
            ),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(
                id = 123L,
                value = "test",
                showWarningDialog = true, // Warning dialog open
            ),
            lexemeList = listOf(),
            snackbarState = SnackbarState(title = "Active snackbar", show = true) // Snackbar active
        )
        
        val message = Msg.NoOperation
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertEquals(
            "State should remain completely unchanged",
            initialState,
            result.state()
        )
        
        // Specific dialog state checks
        assertTrue(
            "Top bar menu should remain open",
            result.state().topBarState.isMenuOpen
        )
        assertTrue(
            "Add lexeme dialog should remain open",
            result.state().addLexemeBottomState.show
        )
        assertTrue(
            "Warning dialog should remain open",
            result.state().wordState.showWarningDialog
        )
        assertTrue(
            "Snackbar should remain active",
            result.state().snackbarState.show
        )
        
        // Effects check
        result.assertNoEffects("NoOperation should not produce any effects")
    }
}
