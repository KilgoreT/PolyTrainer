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
 * 1. Standard case: EnterWordEditMode enables edit mode and sets edited text
 * 2. Boundary case: EnterWordEditMode when already in edit mode
 * 3. Standard case: UpdateWordInput updates edited text
 * 4. Standard case: UpdateWordInput with empty text
 * 5. Standard case: UpdateWordInput with long text
 * 6. Standard case: ExitWordEditMode disables edit mode
 * 7. Boundary case: ExitWordEditMode when not in edit mode
 * 8. Standard case: CommitWordChanges saves edited text and triggers UpdateWord effect
 * 9. Standard case: CommitWordChanges with empty edited text
 * 10. Standard case: CommitWordChanges with same text as original
 */
class WordEditTest {

    @Test
    fun `should enable edit mode and set edited text when EnterWordEditMode is received`() {
        // Test case 1: Standard case - EnterWordEditMode enables edit mode and sets edited text
        // Given
        val reducer = WordCardReducer()
        val originalValue = "original word"
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(
                id = 123L,
                value = originalValue,
                isEditMode = false, // Not in edit mode
                edited = ""
            ),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.EnterWordEditMode
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Edit mode should be enabled",
            result.state().wordState.isEditMode
        )
        assertEquals(
            "Edited text should be set to current value",
            originalValue,
            result.state().wordState.edited
        )
        
        // Effects check
        result.assertNoEffects("EnterWordEditMode should not produce any effects")
        
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
            "Warning dialog should remain unchanged",
            initialState.wordState.showWarningDialog,
            result.state().wordState.showWarningDialog
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
    fun `should enable edit mode when already in edit mode`() {
        // Test case 2: Boundary case - EnterWordEditMode when already in edit mode
        // Given
        val reducer = WordCardReducer()
        val originalValue = "original word"
        val previouslyEdited = "previously edited"
        val initialState = WordCardState(
            wordState = WordState(
                id = 123L,
                value = originalValue,
                isEditMode = true, // Already in edit mode
                edited = previouslyEdited
            )
        )
        
        val message = Msg.EnterWordEditMode
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Edit mode should remain enabled",
            result.state().wordState.isEditMode
        )
        assertEquals(
            "Edited text should be reset to current value",
            originalValue,
            result.state().wordState.edited
        )
        
        // Effects check
        result.assertNoEffects("EnterWordEditMode should not produce any effects")
    }

    @Test
    fun `should update edited text when UpdateWordInput is received`() {
        // Test case 3: Standard case - UpdateWordInput updates edited text
        // Given
        val reducer = WordCardReducer()
        val newValue = "updated word"
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(
                id = 123L,
                value = "original word",
                isEditMode = true,
                edited = "old edited text"
            ),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.UpdateWordInput(value = newValue)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertEquals(
            "Edited text should be updated",
            newValue,
            result.state().wordState.edited
        )
        
        // Effects check
        result.assertNoEffects("UpdateWordInput should not produce any effects")
        
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
            "Edit mode should remain unchanged",
            initialState.wordState.isEditMode,
            result.state().wordState.isEditMode
        )
        assertEquals(
            "Word added date should remain unchanged",
            initialState.wordState.added,
            result.state().wordState.added
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
    fun `should update edited text with empty text when UpdateWordInput is received`() {
        // Test case 4: Standard case - UpdateWordInput with empty text
        // Given
        val reducer = WordCardReducer()
        val emptyValue = ""
        val initialState = WordCardState(
            wordState = WordState(
                id = 123L,
                value = "original word",
                isEditMode = true,
                edited = "previous text"
            )
        )
        
        val message = Msg.UpdateWordInput(value = emptyValue)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertEquals(
            "Edited text should be empty",
            emptyValue,
            result.state().wordState.edited
        )
        
        // Effects check
        result.assertNoEffects("UpdateWordInput should not produce any effects")
    }

    @Test
    fun `should disable edit mode when ExitWordEditMode is received`() {
        // Test case 6: Standard case - ExitWordEditMode disables edit mode
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            wordState = WordState(
                id = 123L,
                value = "original word",
                isEditMode = true, // Currently in edit mode
                edited = "edited text"
            )
        )
        
        val message = Msg.ExitWordEditMode
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertFalse(
            "Edit mode should be disabled",
            result.state().wordState.isEditMode
        )
        
        // Effects check
        result.assertNoEffects("ExitWordEditMode should not produce any effects")
        
        // Other word state properties should remain unchanged
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
    }

    @Test
    fun `should disable edit mode when not in edit mode`() {
        // Test case 7: Boundary case - ExitWordEditMode when not in edit mode
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            wordState = WordState(
                id = 123L,
                value = "original word",
                isEditMode = false, // Not in edit mode
                edited = ""
            )
        )
        
        val message = Msg.ExitWordEditMode
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertFalse(
            "Edit mode should remain disabled",
            result.state().wordState.isEditMode
        )
        
        // Effects check
        result.assertNoEffects("ExitWordEditMode should not produce any effects")
    }

    @Test
    fun `should save edited text and trigger UpdateWord effect when CommitWordChanges is received`() {
        // Test case 8: Standard case - CommitWordChanges saves edited text and triggers UpdateWord effect
        // Given
        val reducer = WordCardReducer()
        val wordId = 123L
        val editedText = "edited word"
        val initialState = WordCardState(
            wordState = WordState(
                id = wordId,
                value = "original word",
                isEditMode = true,
                edited = editedText
            )
        )
        
        val message = Msg.CommitWordChanges
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertEquals(
            "Word value should be updated to edited text",
            editedText,
            result.state().wordState.value
        )
        
        // Effects check - should trigger UpdateWord effect
        result.assertEffects(
            setOf(DatasourceEffect.UpdateWord(wordId = wordId, value = editedText)),
            "Should trigger UpdateWord effect with correct word ID and value"
        )
        
        // Other word state properties should remain unchanged
        assertEquals(
            "Word ID should remain unchanged",
            initialState.wordState.id,
            result.state().wordState.id
        )
        assertEquals(
            "Edit mode should remain unchanged",
            initialState.wordState.isEditMode,
            result.state().wordState.isEditMode
        )
        assertEquals(
            "Edited text should remain unchanged",
            initialState.wordState.edited,
            result.state().wordState.edited
        )
    }

    @Test
    fun `should save empty edited text when CommitWordChanges is received`() {
        // Test case 9: Standard case - CommitWordChanges with empty edited text
        // Given
        val reducer = WordCardReducer()
        val wordId = 456L
        val emptyEditedText = ""
        val initialState = WordCardState(
            wordState = WordState(
                id = wordId,
                value = "original word",
                isEditMode = true,
                edited = emptyEditedText
            )
        )
        
        val message = Msg.CommitWordChanges
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertEquals(
            "Word value should be updated to empty text",
            emptyEditedText,
            result.state().wordState.value
        )
        
        // Effects check - should trigger UpdateWord effect with empty value
        result.assertEffects(
            setOf(DatasourceEffect.UpdateWord(wordId = wordId, value = emptyEditedText)),
            "Should trigger UpdateWord effect with empty value"
        )
    }

    @Test
    fun `should save edited text when same as original when CommitWordChanges is received`() {
        // Test case 10: Standard case - CommitWordChanges with same text as original
        // Given
        val reducer = WordCardReducer()
        val wordId = 789L
        val sameText = "same word"
        val initialState = WordCardState(
            wordState = WordState(
                id = wordId,
                value = sameText,
                isEditMode = true,
                edited = sameText // Same as original
            )
        )
        
        val message = Msg.CommitWordChanges
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertEquals(
            "Word value should remain the same",
            sameText,
            result.state().wordState.value
        )
        
        // Effects check - should still trigger UpdateWord effect
        result.assertEffects(
            setOf(DatasourceEffect.UpdateWord(wordId = wordId, value = sameText)),
            "Should trigger UpdateWord effect even with same value"
        )
    }
}
