package me.apomazkin.wordcard.mate.ext

import me.apomazkin.wordcard.mate.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Tests for extensions related to snackbar state management
 * 
 * Test cases:
 * 1. Standard case: showSnackbar displays snackbar with message
 * 2. Standard case: showSnackbar displays snackbar with empty message
 * 3. Standard case: showSnackbar displays snackbar when already showing
 * 4. Standard case: showSnackbar displays snackbar with complex data
 * 5. Boundary case: hideSnackbar hides snackbar when already hidden
 * 6. Standard case: hideSnackbar hides snackbar when showing
 * 7. Standard case: hideSnackbar hides snackbar with complex data
 */
class SnackbarExtTest {

    @Test
    fun `should show snackbar with message when showSnackbar is called`() {
        // Test case 1: Standard case - showSnackbar displays snackbar with message
        // Given
        val testMessage = "Test snackbar message"
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState(title = "", show = false)
        )
        
        // When
        val resultState = initialState.showSnackbar(testMessage)
        
        // Then
        // Main functionality check
        assertTrue("Snackbar should be shown", resultState.snackbarState.show)
        assertEquals("Snackbar title should be set", testMessage, resultState.snackbarState.title)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "topBarState should not mutate on showSnackbar() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on showSnackbar() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "isLoading should not mutate on showSnackbar() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on showSnackbar() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on showSnackbar() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
    }

    @Test
    fun `should show snackbar with empty message when showSnackbar is called`() {
        // Test case 2: Standard case - showSnackbar displays snackbar with empty message
        // Given
        val emptyMessage = ""
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState(title = "Previous message", show = false)
        )
        
        // When
        val resultState = initialState.showSnackbar(emptyMessage)
        
        // Then
        // Main functionality check
        assertTrue("Snackbar should be shown", resultState.snackbarState.show)
        assertEquals("Snackbar title should be empty", emptyMessage, resultState.snackbarState.title)
        
        // Immutability checks
        assertEquals(
            "topBarState should not mutate on showSnackbar() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on showSnackbar() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "isLoading should not mutate on showSnackbar() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on showSnackbar() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on showSnackbar() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
    }

    @Test
    fun `should show snackbar when already showing`() {
        // Test case 3: Standard case - showSnackbar displays snackbar when already showing
        // Given
        val newMessage = "New snackbar message"
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState(title = "Old message", show = true) // Already showing
        )
        
        // When
        val resultState = initialState.showSnackbar(newMessage)
        
        // Then
        // Main functionality check
        assertTrue("Snackbar should be shown", resultState.snackbarState.show)
        assertEquals("Snackbar title should be updated", newMessage, resultState.snackbarState.title)
        
        // Immutability checks
        assertEquals(
            "topBarState should not mutate on showSnackbar() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on showSnackbar() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "isLoading should not mutate on showSnackbar() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on showSnackbar() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on showSnackbar() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
    }

    @Test
    fun `should show snackbar with complex data`() {
        // Test case 4: Standard case - showSnackbar displays snackbar with complex data
        // Given
        val testMessage = "Complex snackbar message"
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
            isLoading = true,
            wordState = WordState(
                id = 456L,
                value = "complex word",
                added = Date(1000L),
                isEditMode = true,
                edited = "edited complex word",
                showWarningDialog = true,
            ),
            lexemeList = lexemeList,
            snackbarState = SnackbarState(title = "", show = false)
        )
        
        // When
        val resultState = initialState.showSnackbar(testMessage)
        
        // Then
        // Main functionality check
        assertTrue("Snackbar should be shown", resultState.snackbarState.show)
        assertEquals("Snackbar title should be set", testMessage, resultState.snackbarState.title)
        
        // Immutability checks - ALL complex state must remain unchanged
        assertEquals(
            "topBarState should not mutate on showSnackbar() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on showSnackbar() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "isLoading should not mutate on showSnackbar() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on showSnackbar() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on showSnackbar() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
    }

    @Test
    fun `should hide snackbar when hideSnackbar is called`() {
        // Test case 5: Boundary case - hideSnackbar hides snackbar when already hidden
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState(title = "Some message", show = false) // Already hidden
        )
        
        // When
        val resultState = initialState.hideSnackbar()
        
        // Then
        // Main functionality check
        assertFalse("Snackbar should be hidden", resultState.snackbarState.show)
        assertEquals("Snackbar title should be empty", "", resultState.snackbarState.title)
        
        // Immutability checks
        assertEquals(
            "topBarState should not mutate on hideSnackbar() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on hideSnackbar() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "isLoading should not mutate on hideSnackbar() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on hideSnackbar() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on hideSnackbar() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
    }

    @Test
    fun `should hide snackbar when showing`() {
        // Test case 6: Standard case - hideSnackbar hides snackbar when showing
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState(title = "Visible message", show = true) // Currently showing
        )
        
        // When
        val resultState = initialState.hideSnackbar()
        
        // Then
        // Main functionality check
        assertFalse("Snackbar should be hidden", resultState.snackbarState.show)
        assertEquals("Snackbar title should be empty", "", resultState.snackbarState.title)
        
        // Immutability checks
        assertEquals(
            "topBarState should not mutate on hideSnackbar() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on hideSnackbar() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "isLoading should not mutate on hideSnackbar() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on hideSnackbar() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on hideSnackbar() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
    }

    @Test
    fun `should hide snackbar with complex data`() {
        // Test case 7: Standard case - hideSnackbar hides snackbar with complex data
        // Given
        val lexemeList = listOf(
            LexemeState(
                id = 1L,
                translation = TextValueState(origin = "translation", isEdit = true),
                definition = null,
                isMenuOpen = false
            ),
            LexemeState(
                id = 2L,
                translation = TextValueState(origin = "trans2", isEdit = false),
                definition = TextValueState(origin = "def2", isEdit = true),
                isMenuOpen = true
            )
        )
        
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true),
            addLexemeBottomState = AddLexemeBottomState(
                show = false,
                isTranslationCheck = false,
                isDefinitionCheck = true
            ),
            isLoading = false,
            wordState = WordState(
                id = 789L,
                value = "complex test",
                added = Date(2000L),
                isEditMode = false,
                edited = "",
                showWarningDialog = false,
            ),
            lexemeList = lexemeList,
            snackbarState = SnackbarState(title = "Complex message", show = true)
        )
        
        // When
        val resultState = initialState.hideSnackbar()
        
        // Then
        // Main functionality check
        assertFalse("Snackbar should be hidden", resultState.snackbarState.show)
        assertEquals("Snackbar title should be empty", "", resultState.snackbarState.title)
        
        // Immutability checks - ALL complex state must remain unchanged
        assertEquals(
            "topBarState should not mutate on hideSnackbar() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on hideSnackbar() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "isLoading should not mutate on hideSnackbar() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on hideSnackbar() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on hideSnackbar() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
    }
}
