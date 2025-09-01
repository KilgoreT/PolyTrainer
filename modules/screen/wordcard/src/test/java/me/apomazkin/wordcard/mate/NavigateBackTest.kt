package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Standard case: NavigateBack sets closeScreen to true
 * 2. Boundary case: NavigateBack when closeScreen already true - should remain true (idempotency)
 * 3. Standard case: NavigateBack with complex state - should only affect closeScreen
 * 4. Standard case: NavigateBack while loading - should set closeScreen regardless of loading state
 * 5. Standard case: NavigateBack with active dialogs - should set closeScreen without affecting dialogs
 */
class NavigateBackTest {

    @Test
    fun `should set closeScreen to true when NavigateBack is received`() {
        // Test case 1: Standard case - NavigateBack sets closeScreen to true
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false, // Initially false
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.NavigateBack
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "CloseScreen should be set to true",
            result.state().closeScreen
        )
        
        // Effects check
        result.assertNoEffects("NavigateBack should not produce any effects")
        
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
    fun `should remain true when closeScreen already true`() {
        // Test case 2: Boundary case - NavigateBack when closeScreen already true should remain true (idempotency)
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = true, // Already true
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.NavigateBack
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check - should remain true
        assertTrue(
            "CloseScreen should remain true",
            result.state().closeScreen
        )
        
        // Effects check
        result.assertNoEffects("NavigateBack should not produce any effects")
        
        // Immutability checks
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
    fun `should set closeScreen with complex state`() {
        // Test case 3: Standard case - NavigateBack with complex state should only affect closeScreen
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
        
        val message = Msg.NavigateBack
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "CloseScreen should be set to true",
            result.state().closeScreen
        )
        
        // Effects check
        result.assertNoEffects("NavigateBack should not produce any effects")
        
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
    fun `should set closeScreen while loading`() {
        // Test case 4: Standard case - NavigateBack while loading should set closeScreen regardless of loading state
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
        
        val message = Msg.NavigateBack
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "CloseScreen should be set to true even while loading",
            result.state().closeScreen
        )
        
        // Loading state should remain unchanged
        assertTrue(
            "Loading state should remain true",
            result.state().isLoading
        )
        
        // Effects check
        result.assertNoEffects("NavigateBack should not produce any effects")
    }

    @Test
    fun `should set closeScreen with active dialogs`() {
        // Test case 5: Standard case - NavigateBack with active dialogs should set closeScreen without affecting dialogs
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true),
            addLexemeBottomState = AddLexemeBottomState(
                show = true, // Active add lexeme dialog
                isTranslationCheck = true,
                isDefinitionCheck = false
            ),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(
                id = 123L,
                value = "test",
                showWarningDialog = true, // Active delete dialog
            ),
            lexemeList = listOf(),
            snackbarState = SnackbarState(title = "Active snackbar", show = true)
        )
        
        val message = Msg.NavigateBack
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "CloseScreen should be set to true",
            result.state().closeScreen
        )
        
        // Dialog states should remain unchanged
        assertTrue(
            "Top bar menu should remain open",
            result.state().topBarState.isMenuOpen
        )
        assertTrue(
            "Add lexeme dialog should remain active",
            result.state().addLexemeBottomState.show
        )
        assertTrue(
            "Delete dialog should remain active",
            result.state().wordState.showWarningDialog
        )
        assertTrue(
            "Snackbar should remain active",
            result.state().snackbarState.show
        )
        
        // Effects check
        result.assertNoEffects("NavigateBack should not produce any effects")
    }
}
