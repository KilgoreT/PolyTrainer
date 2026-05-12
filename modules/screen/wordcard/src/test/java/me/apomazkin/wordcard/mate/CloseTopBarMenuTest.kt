package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Boundary case: CloseTopBarMenu when menu is open - should close menu
 * 2. Boundary case: CloseTopBarMenu when menu is already closed - should remain closed (idempotency)
 * 3. Standard case: CloseTopBarMenu while loading - should close menu regardless of loading state
 * 4. Standard case: CloseTopBarMenu while not loading - should close menu
 * 5. Edge case: CloseTopBarMenu with active delete dialog - should close menu without affecting dialog
 * 6. Edge case: CloseTopBarMenu with active add lexeme dialog - should close menu without affecting dialog
 * 7. Edge case: CloseTopBarMenu with empty lexeme list - should close menu
 * 8. Edge case: CloseTopBarMenu with multiple lexemes - should close menu without affecting lexemes
 * 9. Edge case: CloseTopBarMenu with word in edit mode - should close menu without affecting edit state
 * 10. Edge case: CloseTopBarMenu with active snackbar - should close menu without affecting snackbar
 */
class CloseTopBarMenuTest {

    @Test
    fun `should close top bar menu when menu is open`() {
        // Test case 1: Boundary case - CloseTopBarMenu when menu is open should close menu
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true), // Currently open
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.CloseTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertFalse(
            "Top bar menu should be closed",
            result.state().topBarState.isMenuOpen
        )
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "AddLexemeBottomState should remain unchanged",
            initialState.addLexemeBottomState,
            result.state().addLexemeBottomState
        )
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "WordState should remain unchanged",
            initialState.wordState,
            result.state().wordState
        )
        assertEquals(
            "Lexeme list should remain unchanged",
            initialState.lexemeList,
            result.state().lexemeList
        )
        assertEquals(
            "SnackbarState should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        
        // Effects check
        result.assertNoEffects("CloseTopBarMenu should not produce any effects")
    }

    @Test
    fun `should remain closed when menu is already closed`() {
        // Test case 2: Boundary case - CloseTopBarMenu when menu is already closed should remain closed (idempotency)
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false), // Already closed
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.CloseTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check - should remain closed
        assertFalse(
            "Top bar menu should remain closed",
            result.state().topBarState.isMenuOpen
        )
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "AddLexemeBottomState should remain unchanged",
            initialState.addLexemeBottomState,
            result.state().addLexemeBottomState
        )
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            result.state().isLoading
        )
        assertEquals(
            "WordState should remain unchanged",
            initialState.wordState,
            result.state().wordState
        )
        assertEquals(
            "Lexeme list should remain unchanged",
            initialState.lexemeList,
            result.state().lexemeList
        )
        assertEquals(
            "SnackbarState should remain unchanged",
            initialState.snackbarState,
            result.state().snackbarState
        )
        
        // Effects check
        result.assertNoEffects("CloseTopBarMenu should not produce any effects")
    }

    @Test
    fun `should close menu while loading`() {
        // Test case 3: Standard case - CloseTopBarMenu while loading should close menu regardless of loading state
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = true, // Loading state
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.CloseTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertFalse(
            "Top bar menu should be closed even while loading",
            result.state().topBarState.isMenuOpen
        )
        
        // Immutability checks
        assertTrue(
            "Loading state should remain true",
            result.state().isLoading
        )
        assertEquals(
            "WordState should remain unchanged",
            initialState.wordState,
            result.state().wordState
        )
        
        // Effects check
        result.assertNoEffects("CloseTopBarMenu should not produce any effects")
    }

    @Test
    fun `should close menu with active delete dialog`() {
        // Test case 5: Edge case - CloseTopBarMenu with active delete dialog should close menu without affecting dialog
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(
                id = 123L, 
                value = "test",
                showWarningDialog = true, // Active delete dialog
            ),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.CloseTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertFalse(
            "Top bar menu should be closed",
            result.state().topBarState.isMenuOpen
        )
        
        // Dialog state should remain unchanged
        assertTrue(
            "Delete dialog should remain active",
            result.state().wordState.showWarningDialog
        )
        
        // Effects check
        result.assertNoEffects("CloseTopBarMenu should not produce any effects")
    }

    @Test
    fun `should close menu with multiple lexemes`() {
        // Test case 8: Edge case - CloseTopBarMenu with multiple lexemes should close menu without affecting lexemes
        // Given
        val reducer = WordCardReducer()
        val lexemeList = listOf(
            LexemeState(id = 1L, translation = TextValueState(origin = "translation1", isEdit = false)),
            LexemeState(id = 2L, definition = TextValueState(origin = "definition2", isEdit = false)),
            LexemeState(id = 3L, isMenuOpen = true) // One lexeme has open menu
        )
        
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = lexemeList,
            snackbarState = SnackbarState()
        )
        
        val message = Msg.CloseTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertFalse(
            "Top bar menu should be closed",
            result.state().topBarState.isMenuOpen
        )
        
        // Lexeme list should remain unchanged
        assertEquals(
            "Lexeme list size should remain unchanged",
            3,
            result.state().lexemeList.size
        )
        assertEquals(
            "First lexeme should remain unchanged",
            lexemeList[0],
            result.state().lexemeList[0]
        )
        assertEquals(
            "Second lexeme should remain unchanged",
            lexemeList[1],
            result.state().lexemeList[1]
        )
        assertEquals(
            "Third lexeme should remain unchanged",
            lexemeList[2],
            result.state().lexemeList[2]
        )
        
        // Effects check
        result.assertNoEffects("CloseTopBarMenu should not produce any effects")
    }

    @Test
    fun `should close menu with word in edit mode`() {
        // Test case 9: Edge case - CloseTopBarMenu with word in edit mode should close menu without affecting edit state
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(
                id = 123L, 
                value = "test",
                isEditMode = true, // Word is in edit mode
                edited = "edited_test"
            ),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.CloseTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertFalse(
            "Top bar menu should be closed",
            result.state().topBarState.isMenuOpen
        )
        
        // Word edit state should remain unchanged
        assertTrue(
            "Word should remain in edit mode",
            result.state().wordState.isEditMode
        )
        assertEquals(
            "Edited text should remain unchanged",
            "edited_test",
            result.state().wordState.edited
        )
        assertEquals(
            "Word value should remain unchanged",
            "test",
            result.state().wordState.value
        )
        
        // Effects check
        result.assertNoEffects("CloseTopBarMenu should not produce any effects")
    }

    @Test
    fun `should close menu with active snackbar`() {
        // Test case 10: Edge case - CloseTopBarMenu with active snackbar should close menu without affecting snackbar
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState(
                title = "Test snackbar message",
                show = true // Active snackbar
            )
        )
        
        val message = Msg.CloseTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertFalse(
            "Top bar menu should be closed",
            result.state().topBarState.isMenuOpen
        )
        
        // Snackbar state should remain unchanged
        assertTrue(
            "Snackbar should remain active",
            result.state().snackbarState.show
        )
        assertEquals(
            "Snackbar message should remain unchanged",
            "Test snackbar message",
            result.state().snackbarState.title
        )
        
        // Effects check
        result.assertNoEffects("CloseTopBarMenu should not produce any effects")
    }
}
