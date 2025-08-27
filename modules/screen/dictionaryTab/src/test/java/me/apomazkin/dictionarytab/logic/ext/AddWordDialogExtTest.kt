package me.apomazkin.dictionarytab.logic.ext

import me.apomazkin.dictionarytab.logic.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for extensions related to add word dialog management
 * 
 * Test cases:
 * 1. Boundary case: showAddWordDialog opens dialog with null wordValue
 * 2. Standard case: showAddWordDialog opens dialog with empty wordValue
 * 3. Standard case: showAddWordDialog opens dialog with valid wordValue
 * 4. Boundary case: showAddWordDialog opens dialog with null wordId
 * 5. Standard case: showAddWordDialog opens dialog with valid wordId
 * 6. Boundary case: hideAddWordDialog closes dialog when already closed
 * 7. Standard case: hideAddWordDialog closes dialog and resets values
 * 8. Standard case: updateWordValue updates word value in dialog
 * 9. Boundary case: updateWordValue handles null value
 */
class AddWordDialogExtTest {

    @Test
    fun `should open dialog with empty string when showAddWordDialog called with null wordValue`() {
        // Test case 1: Boundary case - showAddWordDialog opens dialog with null wordValue
        // Given
        val initialState = DictionaryTabState(
            isLoading = false,
            addWordDialogState = AddWordDialogState(
                isOpen = false,
                wordValue = "",
                wordId = null
            ),
            snackbarState = SnackbarState(title = "test", show = true)
        )
        
        // When
        val resultState = initialState.showAddWordDialog(
            wordValue = null,
            wordId = null
        )
        
        // Then
        // Main functionality check
        assertTrue("Dialog should be opened", resultState.addWordDialogState.isOpen)
        assertEquals("Word value should be empty string", "", resultState.addWordDialogState.wordValue)
        assertEquals("Word ID should be null", null, resultState.addWordDialogState.wordId)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }

    @Test
    fun `should open dialog with provided wordValue when showAddWordDialog called with valid wordValue`() {
        // Test case 3: Standard case - showAddWordDialog opens dialog with valid wordValue
        // Given
        val testWordValue = "hello world"
        val initialState = DictionaryTabState(
            isLoading = true,
            addWordDialogState = AddWordDialogState(
                isOpen = false,
                wordValue = "",
                wordId = null
            ),
            topBarState = TopBarState(isActionMode = true)
        )
        
        // When
        val resultState = initialState.showAddWordDialog(
            wordValue = testWordValue,
            wordId = null
        )
        
        // Then
        // Main functionality check
        assertTrue("Dialog should be opened", resultState.addWordDialogState.isOpen)
        assertEquals("Word value should match provided value", testWordValue, resultState.addWordDialogState.wordValue)
        assertEquals("Word ID should be null", null, resultState.addWordDialogState.wordId)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }

    @Test
    fun `should open dialog with provided wordId when showAddWordDialog called with valid wordId`() {
        // Test case 5: Standard case - showAddWordDialog opens dialog with valid wordId
        // Given
        val testWordId = 123L
        val testWordValue = "test word"
        val initialState = DictionaryTabState(
            isLoading = false,
            addWordDialogState = AddWordDialogState(
                isOpen = false,
                wordValue = "",
                wordId = null
            ),
            snackbarState = SnackbarState(show = false)
        )
        
        // When
        val resultState = initialState.showAddWordDialog(
            wordValue = testWordValue,
            wordId = testWordId
        )
        
        // Then
        // Main functionality check
        assertTrue("Dialog should be opened", resultState.addWordDialogState.isOpen)
        assertEquals("Word value should match provided value", testWordValue, resultState.addWordDialogState.wordValue)
        assertEquals("Word ID should match provided ID", testWordId, resultState.addWordDialogState.wordId)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }

    @Test
    fun `should close dialog and reset values when hideAddWordDialog called`() {
        // Test case 7: Standard case - hideAddWordDialog closes dialog and resets values
        // Given
        val initialState = DictionaryTabState(
            isLoading = false,
            addWordDialogState = AddWordDialogState(
                isOpen = true,
                wordValue = "test word",
                wordId = 456L
            ),
            topBarState = TopBarState(isActionMode = false)
        )
        
        // When
        val resultState = initialState.hideAddWordDialog()
        
        // Then
        // Main functionality check
        assertFalse("Dialog should be closed", resultState.addWordDialogState.isOpen)
        assertEquals("Word value should be reset to empty string", "", resultState.addWordDialogState.wordValue)
        assertEquals("Word ID should be reset to null", null, resultState.addWordDialogState.wordId)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }

    @Test
    fun `should close dialog and reset values when hideAddWordDialog called on already closed dialog`() {
        // Test case 6: Boundary case - hideAddWordDialog closes dialog when already closed
        // Given
        val initialState = DictionaryTabState(
            isLoading = true,
            addWordDialogState = AddWordDialogState(
                isOpen = false,
                wordValue = "existing value",
                wordId = 789L
            ),
            snackbarState = SnackbarState(title = "existing", show = true)
        )
        
        // When
        val resultState = initialState.hideAddWordDialog()
        
        // Then
        // Main functionality check
        assertFalse("Dialog should remain closed", resultState.addWordDialogState.isOpen)
        assertEquals("Word value should be reset to empty string", "", resultState.addWordDialogState.wordValue)
        assertEquals("Word ID should be reset to null", null, resultState.addWordDialogState.wordId)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }

    @Test
    fun `should update word value when updateWordValue called with valid value`() {
        // Test case 8: Standard case - updateWordValue updates word value in dialog
        // Given
        val newValue = "updated word value"
        val initialState = DictionaryTabState(
            isLoading = false,
            addWordDialogState = AddWordDialogState(
                isOpen = true,
                wordValue = "original value",
                wordId = 123L
            ),
            topBarState = TopBarState(isActionMode = true)
        )
        
        // When
        val resultState = initialState.updateWordValue(newValue)
        
        // Then
        // Main functionality check
        assertEquals("Word value should be updated", newValue, resultState.addWordDialogState.wordValue)
        assertTrue("Dialog should remain open", resultState.addWordDialogState.isOpen)
        assertEquals("Word ID should remain unchanged", 123L, resultState.addWordDialogState.wordId)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }

    @Test
    fun `should update word value when updateWordValue called with empty string`() {
        // Test case 9: Boundary case - updateWordValue handles null value
        // Given
        val emptyValue = ""
        val initialState = DictionaryTabState(
            isLoading = true,
            addWordDialogState = AddWordDialogState(
                isOpen = false,
                wordValue = "previous value",
                wordId = null
            ),
            snackbarState = SnackbarState(show = false)
        )
        
        // When
        val resultState = initialState.updateWordValue(emptyValue)
        
        // Then
        // Main functionality check
        assertEquals("Word value should be updated to empty string", emptyValue, resultState.addWordDialogState.wordValue)
        assertFalse("Dialog should remain closed", resultState.addWordDialogState.isOpen)
        assertEquals("Word ID should remain unchanged", null, resultState.addWordDialogState.wordId)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }

    @Test
    fun `should open dialog with empty wordValue when showAddWordDialog called with empty string`() {
        // Test case 2: Standard case - showAddWordDialog opens dialog with empty wordValue
        // Given
        val initialState = DictionaryTabState(
            isLoading = false,
            addWordDialogState = AddWordDialogState(
                isOpen = false,
                wordValue = "",
                wordId = null
            ),
            topBarState = TopBarState()
        )
        
        // When
        val resultState = initialState.showAddWordDialog(
            wordValue = "",
            wordId = null
        )
        
        // Then
        // Main functionality check
        assertTrue("Dialog should be opened", resultState.addWordDialogState.isOpen)
        assertEquals("Word value should be empty string", "", resultState.addWordDialogState.wordValue)
        assertEquals("Word ID should be null", null, resultState.addWordDialogState.wordId)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }

    @Test
    fun `should open dialog with null wordId when showAddWordDialog called with null wordId`() {
        // Test case 4: Boundary case - showAddWordDialog opens dialog with null wordId
        // Given
        val testWordValue = "test"
        val initialState = DictionaryTabState(
            isLoading = false,
            addWordDialogState = AddWordDialogState(
                isOpen = false,
                wordValue = "",
                wordId = 999L
            ),
            snackbarState = SnackbarState(title = "test", show = false)
        )
        
        // When
        val resultState = initialState.showAddWordDialog(
            wordValue = testWordValue,
            wordId = null
        )
        
        // Then
        // Main functionality check
        assertTrue("Dialog should be opened", resultState.addWordDialogState.isOpen)
        assertEquals("Word value should match provided value", testWordValue, resultState.addWordDialogState.wordValue)
        assertEquals("Word ID should be null", null, resultState.addWordDialogState.wordId)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
        )
        assertEquals(
            "Confirm delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }
}
