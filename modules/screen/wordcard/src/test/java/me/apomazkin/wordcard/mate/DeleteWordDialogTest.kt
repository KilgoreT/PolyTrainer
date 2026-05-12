package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Standard case: OpenDeleteWordDialog shows dialog
 * 2. Boundary case: OpenDeleteWordDialog when dialog already open
 * 3. Standard case: CloseDeleteWordDialog hides dialog
 * 4. Boundary case: CloseDeleteWordDialog when dialog already closed
 * 5. Standard case: RemoveWord triggers RemoveWord effect
 * 6. Standard case: RemoveWord with different word IDs
 * 7. Edge case: RemoveWord with NOT_IN_DB word ID
 */
class DeleteWordDialogTest {

    @Test
    fun `should show dialog when OpenDeleteWordDialog is received`() {
        // Test case 1: Standard case - OpenDeleteWordDialog shows dialog
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(
                id = 123L,
                value = "test",
                showWarningDialog = false // Initially hidden
            ),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.OpenDeleteWordDialog
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Warning dialog should be shown",
            result.state().wordState.showWarningDialog
        )
        
        // Effects check
        result.assertNoEffects("OpenDeleteWordDialog should not produce any effects")
        
        // Immutability checks - other word state properties should remain unchanged
        assertEquals(
            "Word ID should remain unchanged",
            initialState.wordState.id,
            result.state().wordState.id
        )
        assertEquals(
            "Word value should remain unchanged",
            initialState.wordState.value,
            result.state().wordState.value
        )
        assertEquals(
            "Word added date should remain unchanged",
            initialState.wordState.added,
            result.state().wordState.added
        )
        assertEquals(
            "Word edit mode should remain unchanged",
            initialState.wordState.isEditMode,
            result.state().wordState.isEditMode
        )
        assertEquals(
            "Word edited text should remain unchanged",
            initialState.wordState.edited,
            result.state().wordState.edited
        )
        
        // Other state properties should remain unchanged
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
    fun `should show dialog when already open`() {
        // Test case 2: Boundary case - OpenDeleteWordDialog when dialog already open
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            wordState = WordState(
                id = 123L,
                value = "test",
                showWarningDialog = true // Already open
            )
        )
        
        val message = Msg.OpenDeleteWordDialog
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check - should remain in same state
        assertTrue(
            "Warning dialog should remain shown",
            result.state().wordState.showWarningDialog
        )
        
        // Effects check
        result.assertNoEffects("OpenDeleteWordDialog should not produce any effects")
    }

    @Test
    fun `should hide dialog when CloseDeleteWordDialog is received`() {
        // Test case 3: Standard case - CloseDeleteWordDialog hides dialog
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(
                id = 123L,
                value = "test",
                showWarningDialog = true // Currently shown
            ),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.CloseDeleteWordDialog
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertFalse(
            "Warning dialog should be hidden",
            result.state().wordState.showWarningDialog
        )
        
        // Effects check
        result.assertNoEffects("CloseDeleteWordDialog should not produce any effects")
        
        // Immutability checks - other word state properties should remain unchanged
        assertEquals(
            "Word ID should remain unchanged",
            initialState.wordState.id,
            result.state().wordState.id
        )
        assertEquals(
            "Word value should remain unchanged",
            initialState.wordState.value,
            result.state().wordState.value
        )
        assertEquals(
            "Word added date should remain unchanged",
            initialState.wordState.added,
            result.state().wordState.added
        )
        assertEquals(
            "Word edit mode should remain unchanged",
            initialState.wordState.isEditMode,
            result.state().wordState.isEditMode
        )
        assertEquals(
            "Word edited text should remain unchanged",
            initialState.wordState.edited,
            result.state().wordState.edited
        )
        
        // Other state properties should remain unchanged
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
    fun `should hide dialog when already closed`() {
        // Test case 4: Boundary case - CloseDeleteWordDialog when dialog already closed
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            wordState = WordState(
                id = 123L,
                value = "test",
                showWarningDialog = false // Already closed
            )
        )
        
        val message = Msg.CloseDeleteWordDialog
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check - should remain in same state
        assertFalse(
            "Warning dialog should remain hidden",
            result.state().wordState.showWarningDialog
        )
        
        // Effects check
        result.assertNoEffects("CloseDeleteWordDialog should not produce any effects")
    }

    @Test
    fun `should trigger RemoveWord effect when RemoveWord is received`() {
        // Test case 5: Standard case - RemoveWord triggers RemoveWord effect
        // Given
        val reducer = WordCardReducer()
        val wordId = 123L
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = wordId, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.RemoveWord(wordId = wordId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // State should remain unchanged
        assertEquals(
            "State should remain unchanged",
            initialState,
            result.state()
        )
        
        // Effects check - should trigger RemoveWord effect
        result.assertEffects(
            setOf(DatasourceEffect.RemoveWord(wordId)),
            "Should trigger RemoveWord effect with correct word ID"
        )
    }

    @Test
    fun `should trigger RemoveWord effect with different word IDs`() {
        // Test case 6: Standard case - RemoveWord with different word IDs
        // Given
        val reducer = WordCardReducer()
        val wordId = 456L
        val initialState = WordCardState(
            wordState = WordState(id = 123L, value = "test") // Different ID in state
        )
        
        val message = Msg.RemoveWord(wordId = wordId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // State should remain unchanged
        assertEquals(
            "State should remain unchanged",
            initialState,
            result.state()
        )
        
        // Effects check - should use word ID from message, not from state
        result.assertEffects(
            setOf(DatasourceEffect.RemoveWord(wordId)),
            "Should trigger RemoveWord effect with word ID from message"
        )
    }

    @Test
    fun `should trigger RemoveWord effect with NOT_IN_DB word ID`() {
        // Test case 7: Edge case - RemoveWord with NOT_IN_DB word ID
        // Given
        val reducer = WordCardReducer()
        val wordId = -1L // NOT_IN_DB
        val initialState = WordCardState(
            wordState = WordState(id = wordId, value = "")
        )
        
        val message = Msg.RemoveWord(wordId = wordId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // State should remain unchanged
        assertEquals(
            "State should remain unchanged",
            initialState,
            result.state()
        )
        
        // Effects check - should trigger RemoveWord even with NOT_IN_DB ID
        result.assertEffects(
            setOf(DatasourceEffect.RemoveWord(wordId)),
            "Should trigger RemoveWord effect even with NOT_IN_DB word ID"
        )
    }
}
