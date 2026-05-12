package me.apomazkin.wordcard.mate.ext

import me.apomazkin.wordcard.entity.Term
import me.apomazkin.wordcard.entity.Word
import me.apomazkin.wordcard.entity.WordId
import me.apomazkin.wordcard.mate.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Tests for extensions related to word state management
 * 
 * Test cases:
 * 1. Standard case: setWordId sets word ID
 * 2. Standard case: setWordAdded sets word added date
 * 3. Standard case: setWordValue sets word value
 * 4. Boundary case: enableWordEdit enables edit mode when not editing
 * 5. Boundary case: enableWordEdit enables edit mode when already editing
 * 6. Boundary case: disableWordEdit disables edit mode when editing
 * 7. Boundary case: disableWordEdit disables edit mode when not editing
 * 8. Standard case: updateWordEdited updates edited text
 * 9. Boundary case: showWordWarningDialog shows dialog when hidden
 * 10. Boundary case: showWordWarningDialog shows dialog when already shown
 * 11. Boundary case: hideWordWarningDialog hides dialog when shown
 * 12. Boundary case: hideWordWarningDialog hides dialog when already hidden
 * 13. Standard case: setTerm sets complete term data
 */
class WordExtTest {

    @Test
    fun `should set word ID when setWordId is called`() {
        // Test case 1: Standard case - setWordId sets word ID
        // Given
        val newWordId = 456L
        val initialState = WordCardState(
            wordState = WordState(id = 123L, value = "test")
        )
        
        // When
        val resultState = initialState.setWordId(newWordId)
        
        // Then
        // Main functionality check
        assertEquals("Word ID should be updated", newWordId, resultState.wordState.id)
        
        // Immutability checks - other word state properties should remain unchanged
        assertEquals(
            "Word value should remain unchanged",
            initialState.wordState.value,
            resultState.wordState.value
        )
        assertEquals(
            "Word added date should remain unchanged",
            initialState.wordState.added,
            resultState.wordState.added
        )
        assertEquals(
            "Word edit mode should remain unchanged",
            initialState.wordState.isEditMode,
            resultState.wordState.isEditMode
        )
        assertEquals(
            "Word edited text should remain unchanged",
            initialState.wordState.edited,
            resultState.wordState.edited
        )
        assertEquals(
            "Word warning dialog should remain unchanged",
            initialState.wordState.showWarningDialog,
            resultState.wordState.showWarningDialog
        )
        
        // Other state properties should remain unchanged
        assertEquals(
            "topBarState should not mutate on setWordId() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on setWordId() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "isLoading should not mutate on setWordId() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "lexemeList should not mutate on setWordId() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on setWordId() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should set word added date when setWordAdded is called`() {
        // Test case 2: Standard case - setWordAdded sets word added date
        // Given
        val newDate = Date(5000L)
        val initialState = WordCardState(
            wordState = WordState(id = 123L, value = "test", added = Date(1000L))
        )
        
        // When
        val resultState = initialState.setWordAdded(newDate)
        
        // Then
        // Main functionality check
        assertEquals("Word added date should be updated", newDate, resultState.wordState.added)
        
        // Immutability checks - other word state properties should remain unchanged
        assertEquals(
            "Word ID should remain unchanged",
            initialState.wordState.id,
            resultState.wordState.id
        )
        assertEquals(
            "Word value should remain unchanged",
            initialState.wordState.value,
            resultState.wordState.value
        )
    }

    @Test
    fun `should enable word edit mode when enableWordEdit is called`() {
        // Test case 4: Boundary case - enableWordEdit enables edit mode when not editing
        // Given
        val initialState = WordCardState(
            wordState = WordState(
                id = 123L,
                value = "original text",
                isEditMode = false,
                edited = ""
            )
        )
        
        // When
        val resultState = initialState.enableWordEdit()
        
        // Then
        // Main functionality check
        assertTrue("Edit mode should be enabled", resultState.wordState.isEditMode)
        assertEquals(
            "Edited text should be set to current value",
            initialState.wordState.value,
            resultState.wordState.edited
        )
        
        // Immutability checks - other word state properties should remain unchanged
        assertEquals(
            "Word ID should remain unchanged",
            initialState.wordState.id,
            resultState.wordState.id
        )
        assertEquals(
            "Word value should remain unchanged",
            initialState.wordState.value,
            resultState.wordState.value
        )
        assertEquals(
            "Word added date should remain unchanged",
            initialState.wordState.added,
            resultState.wordState.added
        )
    }

    @Test
    fun `should enable word edit mode when already editing`() {
        // Test case 5: Boundary case - enableWordEdit enables edit mode when already editing
        // Given
        val initialState = WordCardState(
            wordState = WordState(
                id = 123L,
                value = "original text",
                isEditMode = true,
                edited = "previously edited"
            )
        )
        
        // When
        val resultState = initialState.enableWordEdit()
        
        // Then
        // Main functionality check
        assertTrue("Edit mode should remain enabled", resultState.wordState.isEditMode)
        assertEquals(
            "Edited text should be reset to current value",
            initialState.wordState.value,
            resultState.wordState.edited
        )
    }

    @Test
    fun `should disable word edit mode when disableWordEdit is called`() {
        // Test case 6: Boundary case - disableWordEdit disables edit mode when editing
        // Given
        val initialState = WordCardState(
            wordState = WordState(
                id = 123L,
                value = "original text",
                isEditMode = true,
                edited = "edited text"
            )
        )
        
        // When
        val resultState = initialState.disableWordEdit()
        
        // Then
        // Main functionality check
        assertFalse("Edit mode should be disabled", resultState.wordState.isEditMode)
        assertEquals("Edited text should be cleared", "", resultState.wordState.edited)
        
        // Immutability checks - other word state properties should remain unchanged
        assertEquals(
            "Word ID should remain unchanged",
            initialState.wordState.id,
            resultState.wordState.id
        )
        assertEquals(
            "Word value should remain unchanged",
            initialState.wordState.value,
            resultState.wordState.value
        )
    }

    @Test
    fun `should update word edited text when updateWordEdited is called`() {
        // Test case 8: Standard case - updateWordEdited updates edited text
        // Given
        val newEditedText = "newly edited text"
        val initialState = WordCardState(
            wordState = WordState(
                id = 123L,
                value = "original text",
                isEditMode = true,
                edited = "old edited text"
            )
        )
        
        // When
        val resultState = initialState.updateWordEdited(newEditedText)
        
        // Then
        // Main functionality check
        assertEquals("Edited text should be updated", newEditedText, resultState.wordState.edited)
        
        // Immutability checks - other word state properties should remain unchanged
        assertEquals(
            "Word ID should remain unchanged",
            initialState.wordState.id,
            resultState.wordState.id
        )
        assertEquals(
            "Word value should remain unchanged",
            initialState.wordState.value,
            resultState.wordState.value
        )
        assertEquals(
            "Edit mode should remain unchanged",
            initialState.wordState.isEditMode,
            resultState.wordState.isEditMode
        )
    }

    @Test
    fun `should show warning dialog when showWordWarningDialog is called`() {
        // Test case 9: Boundary case - showWordWarningDialog shows dialog when hidden
        // Given
        val initialState = WordCardState(
            wordState = WordState(
                id = 123L,
                value = "test",
                showWarningDialog = false
            )
        )
        
        // When
        val resultState = initialState.showWordWarningDialog()
        
        // Then
        // Main functionality check
        assertTrue("Warning dialog should be shown", resultState.wordState.showWarningDialog)
        
        // Immutability checks - other word state properties should remain unchanged
        assertEquals(
            "Word ID should remain unchanged",
            initialState.wordState.id,
            resultState.wordState.id
        )
        assertEquals(
            "Word value should remain unchanged",
            initialState.wordState.value,
            resultState.wordState.value
        )
    }

    @Test
    fun `should hide warning dialog when hideWordWarningDialog is called`() {
        // Test case 11: Boundary case - hideWordWarningDialog hides dialog when shown
        // Given
        val initialState = WordCardState(
            wordState = WordState(
                id = 123L,
                value = "test",
                showWarningDialog = true
            )
        )
        
        // When
        val resultState = initialState.hideWordWarningDialog()
        
        // Then
        // Main functionality check
        assertFalse("Warning dialog should be hidden", resultState.wordState.showWarningDialog)
        
        // Immutability checks - other word state properties should remain unchanged
        assertEquals(
            "Word ID should remain unchanged",
            initialState.wordState.id,
            resultState.wordState.id
        )
        assertEquals(
            "Word value should remain unchanged",
            initialState.wordState.value,
            resultState.wordState.value
        )
    }


    @Test
    fun `should set complete term data when setTerm is called`() {
        // Test case 13: Standard case - setTerm sets complete term data
        // Given
        val term = Term(
            wordId = WordId(456L),
            word = Word("new word"),
            addedDate = Date(3000L),
            changedDate = null,
            removedDate = null,
            lexemeList = emptyList()
        )
        
        val initialState = WordCardState(
            wordState = WordState(
                id = 123L,
                value = "old word",
                added = Date(1000L)
            )
        )
        
        // When
        val resultState = initialState.setTerm(term)
        
        // Then
        // Main functionality check
        assertEquals("Word ID should be updated from term", 456L, resultState.wordState.id)
        assertEquals("Word value should be updated from term", "new word", resultState.wordState.value)
        assertEquals("Word added date should be updated from term", Date(3000L), resultState.wordState.added)
        
        // Immutability checks - other word state properties should remain unchanged
        assertEquals(
            "Edit mode should remain unchanged",
            initialState.wordState.isEditMode,
            resultState.wordState.isEditMode
        )
        assertEquals(
            "Edited text should remain unchanged",
            initialState.wordState.edited,
            resultState.wordState.edited
        )
        assertEquals(
            "Warning dialog should remain unchanged",
            initialState.wordState.showWarningDialog,
            resultState.wordState.showWarningDialog
        )
        
        // Other state properties should remain unchanged
        assertEquals(
            "topBarState should not mutate on setTerm() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on setTerm() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "isLoading should not mutate on setTerm() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "lexemeList should not mutate on setTerm() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on setTerm() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }
}
