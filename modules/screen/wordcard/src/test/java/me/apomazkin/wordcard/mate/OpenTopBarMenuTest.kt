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
 * 1. Boundary case: OpenTopBarMenu when menu is closed - should open menu
 * 2. Boundary case: OpenTopBarMenu when menu is already open - should remain open (idempotency)
 * 3. Standard case: OpenTopBarMenu while loading - should open menu regardless of loading state
 * 4. Standard case: OpenTopBarMenu while not loading - should open menu
 * 5. Edge case: OpenTopBarMenu with active delete dialog - should open menu without affecting dialog
 * 6. Edge case: OpenTopBarMenu with active add lexeme dialog - should open menu without affecting dialog
 * 7. Edge case: OpenTopBarMenu with empty lexeme list - should open menu
 * 8. Edge case: OpenTopBarMenu with multiple lexemes - should open menu without affecting lexemes
 * 9. Edge case: OpenTopBarMenu with word in edit mode - should open menu without affecting edit state
 * 10. Edge case: OpenTopBarMenu with active snackbar - should open menu without affecting snackbar
 */
class OpenTopBarMenuTest {

    @Test
    fun `should open top bar menu when menu is closed`() {
        // Test case 1: Boundary case - OpenTopBarMenu when menu is closed should open menu
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = true,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.OpenTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Top bar menu should be opened",
            result.state().topBarState.isMenuOpen
        )
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "AddLexemeBottomState should remain unchanged",
            initialState.addLexemeBottomState,
            result.state().addLexemeBottomState
        )
        assertEquals(
            "CloseScreen should remain unchanged",
            initialState.closeScreen,
            result.state().closeScreen
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
        result.assertNoEffects("OpenTopBarMenu should not produce any effects")
    }

    @Test
    fun `should remain open when menu is already open`() {
        // Test case 2: Boundary case - OpenTopBarMenu when menu is already open should remain open (idempotency)
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true), // Already open
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.OpenTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check - should remain open
        assertTrue(
            "Top bar menu should remain open",
            result.state().topBarState.isMenuOpen
        )
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "AddLexemeBottomState should remain unchanged",
            initialState.addLexemeBottomState,
            result.state().addLexemeBottomState
        )
        assertEquals(
            "CloseScreen should remain unchanged",
            initialState.closeScreen,
            result.state().closeScreen
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
        result.assertNoEffects("OpenTopBarMenu should not produce any effects")
    }

    @Test
    fun `should open menu while loading`() {
        // Test case 3: Standard case - OpenTopBarMenu while loading should open menu regardless of loading state
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = true, // Loading state
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.OpenTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Top bar menu should be opened even while loading",
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
        result.assertNoEffects("OpenTopBarMenu should not produce any effects")
    }

    @Test
    fun `should open menu while not loading`() {
        // Test case 4: Standard case - OpenTopBarMenu while not loading should open menu
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false, // Not loading
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.OpenTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Top bar menu should be opened",
            result.state().topBarState.isMenuOpen
        )
        
        // Immutability checks
        assertFalse(
            "Loading state should remain false",
            result.state().isLoading
        )
        
        // Effects check
        result.assertNoEffects("OpenTopBarMenu should not produce any effects")
    }

    @Test
    fun `should open menu with active delete dialog`() {
        // Test case 5: Edge case - OpenTopBarMenu with active delete dialog should open menu without affecting dialog
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(
                id = 123L, 
                value = "test",
                showWarningDialog = true, // Active delete dialog
            ),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.OpenTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Top bar menu should be opened",
            result.state().topBarState.isMenuOpen
        )
        
        // Dialog state should remain unchanged
        assertTrue(
            "Delete dialog should remain active",
            result.state().wordState.showWarningDialog
        )
        
        // Effects check
        result.assertNoEffects("OpenTopBarMenu should not produce any effects")
    }

    @Test
    fun `should open menu with active add lexeme dialog`() {
        // Test case 6: Edge case - OpenTopBarMenu with active add lexeme dialog should open menu without affecting dialog
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(
                show = true, // Active add lexeme dialog
                isTranslationCheck = true,
                isDefinitionCheck = false
            ),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.OpenTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Top bar menu should be opened",
            result.state().topBarState.isMenuOpen
        )
        
        // Add lexeme dialog state should remain unchanged
        assertTrue(
            "Add lexeme dialog should remain active",
            result.state().addLexemeBottomState.show
        )
        assertTrue(
            "Translation check should remain true",
            result.state().addLexemeBottomState.isTranslationCheck
        )
        assertFalse(
            "Definition check should remain false",
            result.state().addLexemeBottomState.isDefinitionCheck
        )
        
        // Effects check
        result.assertNoEffects("OpenTopBarMenu should not produce any effects")
    }

    @Test
    fun `should open menu with empty lexeme list`() {
        // Test case 7: Edge case - OpenTopBarMenu with empty lexeme list should open menu
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = emptyList(), // Empty lexeme list
            snackbarState = SnackbarState()
        )
        
        val message = Msg.OpenTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Top bar menu should be opened",
            result.state().topBarState.isMenuOpen
        )
        
        // Lexeme list should remain empty
        assertTrue(
            "Lexeme list should remain empty",
            result.state().lexemeList.isEmpty()
        )
        
        // Effects check
        result.assertNoEffects("OpenTopBarMenu should not produce any effects")
    }

    @Test
    fun `should open menu with multiple lexemes`() {
        // Test case 8: Edge case - OpenTopBarMenu with multiple lexemes should open menu without affecting lexemes
        // Given
        val reducer = WordCardReducer()
        val lexemeList = listOf(
            LexemeState(id = 1L, translation = TextValueState(origin = "translation1", isEdit = false)),
            LexemeState(id = 2L, definition = TextValueState(origin = "definition2", isEdit = false)),
            LexemeState(id = 3L, isMenuOpen = true) // One lexeme has open menu
        )
        
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = lexemeList,
            snackbarState = SnackbarState()
        )
        
        val message = Msg.OpenTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Top bar menu should be opened",
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
        result.assertNoEffects("OpenTopBarMenu should not produce any effects")
    }

    @Test
    fun `should open menu with word in edit mode`() {
        // Test case 9: Edge case - OpenTopBarMenu with word in edit mode should open menu without affecting edit state
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
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
        
        val message = Msg.OpenTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Top bar menu should be opened",
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
        result.assertNoEffects("OpenTopBarMenu should not produce any effects")
    }

    @Test
    fun `should open menu with active snackbar`() {
        // Test case 10: Edge case - OpenTopBarMenu with active snackbar should open menu without affecting snackbar
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState(
                title = "Test snackbar message",
                show = true // Active snackbar
            )
        )
        
        val message = Msg.OpenTopBarMenu
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Top bar menu should be opened",
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
        result.assertNoEffects("OpenTopBarMenu should not produce any effects")
    }
}
