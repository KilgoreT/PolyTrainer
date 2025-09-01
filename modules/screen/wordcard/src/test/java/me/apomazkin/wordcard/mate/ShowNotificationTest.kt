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
 * 1. Standard case: ShowNotification with show=true displays notification
 * 2. Standard case: ShowNotification with show=false hides notification
 * 3. Standard case: ShowNotification with empty text and show=true
 * 4. Standard case: ShowNotification with long text and show=true
 * 5. Standard case: ShowNotification replaces existing notification
 * 6. Boundary case: ShowNotification with show=true when notification already shown
 * 7. Boundary case: ShowNotification with show=false when notification already hidden
 * 8. Standard case: ShowNotification with complex state - should only affect snackbar
 */
class ShowNotificationTest {

    @Test
    fun `should display notification when ShowNotification with show true is received`() {
        // Test case 1: Standard case - ShowNotification with show=true displays notification
        // Given
        val reducer = WordCardReducer()
        val testText = "Test notification message"
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState(title = "", show = false) // Initially hidden
        )
        
        val message = UiMsg.ShowNotification(text = testText, show = true)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Notification should be shown",
            result.state().snackbarState.show
        )
        assertEquals(
            "Notification text should be set",
            testText,
            result.state().snackbarState.title
        )
        
        // Effects check
        result.assertNoEffects("ShowNotification should not produce any effects")
        
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
    }

    @Test
    fun `should hide notification when ShowNotification with show false is received`() {
        // Test case 2: Standard case - ShowNotification with show=false hides notification
        // Given
        val reducer = WordCardReducer()
        val testText = "Test notification message"
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState(title = "Previous message", show = true) // Initially shown
        )
        
        val message = UiMsg.ShowNotification(text = testText, show = false)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertFalse(
            "Notification should be hidden",
            result.state().snackbarState.show
        )
        assertEquals(
            "Notification text should be updated",
            testText,
            result.state().snackbarState.title
        )
        
        // Effects check
        result.assertNoEffects("ShowNotification should not produce any effects")
        
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
    fun `should display notification with empty text when ShowNotification is received`() {
        // Test case 3: Standard case - ShowNotification with empty text and show=true
        // Given
        val reducer = WordCardReducer()
        val emptyText = ""
        val initialState = WordCardState(
            snackbarState = SnackbarState(title = "Previous message", show = false)
        )
        
        val message = UiMsg.ShowNotification(text = emptyText, show = true)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Notification should be shown",
            result.state().snackbarState.show
        )
        assertEquals(
            "Notification text should be empty",
            emptyText,
            result.state().snackbarState.title
        )
        
        // Effects check
        result.assertNoEffects("ShowNotification should not produce any effects")
    }

    @Test
    fun `should display notification with long text when ShowNotification is received`() {
        // Test case 4: Standard case - ShowNotification with long text and show=true
        // Given
        val reducer = WordCardReducer()
        val longText = "This is a very long notification message that contains multiple words and should be handled properly by the snackbar system without any issues or truncation problems"
        val initialState = WordCardState(
            snackbarState = SnackbarState(title = "", show = false)
        )
        
        val message = UiMsg.ShowNotification(text = longText, show = true)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Notification should be shown",
            result.state().snackbarState.show
        )
        assertEquals(
            "Notification text should be set to long text",
            longText,
            result.state().snackbarState.title
        )
        
        // Effects check
        result.assertNoEffects("ShowNotification should not produce any effects")
    }

    @Test
    fun `should replace existing notification when ShowNotification is received`() {
        // Test case 5: Standard case - ShowNotification replaces existing notification
        // Given
        val reducer = WordCardReducer()
        val oldText = "Old notification message"
        val newText = "New notification message"
        val initialState = WordCardState(
            snackbarState = SnackbarState(title = oldText, show = true) // Existing notification
        )
        
        val message = UiMsg.ShowNotification(text = newText, show = true)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Notification should remain shown",
            result.state().snackbarState.show
        )
        assertEquals(
            "Notification text should be replaced",
            newText,
            result.state().snackbarState.title
        )
        
        // Effects check
        result.assertNoEffects("ShowNotification should not produce any effects")
    }

    @Test
    fun `should update notification when already shown with show true`() {
        // Test case 6: Boundary case - ShowNotification with show=true when notification already shown
        // Given
        val reducer = WordCardReducer()
        val testText = "Updated notification message"
        val initialState = WordCardState(
            snackbarState = SnackbarState(title = "Previous message", show = true) // Already shown
        )
        
        val message = UiMsg.ShowNotification(text = testText, show = true)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Notification should remain shown",
            result.state().snackbarState.show
        )
        assertEquals(
            "Notification text should be updated",
            testText,
            result.state().snackbarState.title
        )
        
        // Effects check
        result.assertNoEffects("ShowNotification should not produce any effects")
    }

    @Test
    fun `should update notification when already hidden with show false`() {
        // Test case 7: Boundary case - ShowNotification with show=false when notification already hidden
        // Given
        val reducer = WordCardReducer()
        val testText = "Hidden notification message"
        val initialState = WordCardState(
            snackbarState = SnackbarState(title = "Previous message", show = false) // Already hidden
        )
        
        val message = UiMsg.ShowNotification(text = testText, show = false)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertFalse(
            "Notification should remain hidden",
            result.state().snackbarState.show
        )
        assertEquals(
            "Notification text should be updated",
            testText,
            result.state().snackbarState.title
        )
        
        // Effects check
        result.assertNoEffects("ShowNotification should not produce any effects")
    }

    @Test
    fun `should only affect snackbar with complex state when ShowNotification is received`() {
        // Test case 8: Standard case - ShowNotification with complex state should only affect snackbar
        // Given
        val reducer = WordCardReducer()
        val testText = "Complex state notification"
        val lexemeList = listOf(
            LexemeState(
                id = 1L,
                translation = TextValueState(origin = "translation", isEdit = false),
                definition = TextValueState(origin = "definition", isEdit = true),
                isMenuOpen = true
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
            snackbarState = SnackbarState(title = "Old message", show = false)
        )
        
        val message = UiMsg.ShowNotification(text = testText, show = true)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Notification should be shown",
            result.state().snackbarState.show
        )
        assertEquals(
            "Notification text should be updated",
            testText,
            result.state().snackbarState.title
        )
        
        // Effects check
        result.assertNoEffects("ShowNotification should not produce any effects")
        
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
    }
}
