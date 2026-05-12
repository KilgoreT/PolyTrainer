package me.apomazkin.wordcard.mate.ext

import me.apomazkin.wordcard.mate.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Tests for extensions related to loading state management
 * 
 * Test cases:
 * 1. Boundary case: showLoading enables loading state when already loading
 * 2. Standard case: showLoading enables loading state when not loading
 * 3. Standard case: showLoading enables loading state with complex data
 * 4. Boundary case: hideLoading disables loading state when already not loading
 * 5. Standard case: hideLoading disables loading state when loading
 * 6. Standard case: hideLoading disables loading state with complex data
 */
class LoadingExtTest {

    @Test
    fun `should enable loading state when showLoading is called`() {
        // Test case 1: Boundary case - showLoading enables loading state when already loading
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = true, // Already loading
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.showLoading()
        
        // Then
        // Main functionality check
        assertTrue("Loading should be enabled", resultState.isLoading)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "topBarState should not mutate on showLoading() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on showLoading() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "wordState should not mutate on showLoading() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on showLoading() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on showLoading() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should enable loading state when not loading`() {
        // Test case 2: Standard case - showLoading enables loading state when not loading
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false, // Not loading
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.showLoading()
        
        // Then
        // Main functionality check
        assertTrue("Loading should be enabled", resultState.isLoading)
        
        // Immutability checks
        assertEquals(
            "topBarState should not mutate on showLoading() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on showLoading() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "wordState should not mutate on showLoading() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on showLoading() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on showLoading() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should enable loading state with complex data`() {
        // Test case 3: Standard case - showLoading enables loading state with complex data
        // Given
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
            isLoading = false,
            wordState = WordState(
                id = 456L,
                value = "complex word",
                added = Date(1000L),
                isEditMode = true,
                edited = "edited complex word",
                showWarningDialog = true,
            ),
            lexemeList = lexemeList,
            snackbarState = SnackbarState(title = "Test message", show = true)
        )
        
        // When
        val resultState = initialState.showLoading()
        
        // Then
        // Main functionality check
        assertTrue("Loading should be enabled", resultState.isLoading)
        
        // Immutability checks - ALL complex state must remain unchanged
        assertEquals(
            "topBarState should not mutate on showLoading() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on showLoading() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "wordState should not mutate on showLoading() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on showLoading() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on showLoading() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should disable loading state when hideLoading is called`() {
        // Test case 4: Boundary case - hideLoading disables loading state when already not loading
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false, // Already not loading
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.hideLoading()
        
        // Then
        // Main functionality check
        assertFalse("Loading should be disabled", resultState.isLoading)
        
        // Immutability checks
        assertEquals(
            "topBarState should not mutate on hideLoading() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on hideLoading() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "wordState should not mutate on hideLoading() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on hideLoading() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on hideLoading() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should disable loading state when loading`() {
        // Test case 5: Standard case - hideLoading disables loading state when loading
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = true, // Currently loading
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.hideLoading()
        
        // Then
        // Main functionality check
        assertFalse("Loading should be disabled", resultState.isLoading)
        
        // Immutability checks
        assertEquals(
            "topBarState should not mutate on hideLoading() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on hideLoading() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "wordState should not mutate on hideLoading() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on hideLoading() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on hideLoading() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should disable loading state with complex data`() {
        // Test case 6: Standard case - hideLoading disables loading state with complex data
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
            isLoading = true,
            wordState = WordState(
                id = 789L,
                value = "complex test",
                added = Date(2000L),
                isEditMode = false,
                edited = "",
                showWarningDialog = false,
            ),
            lexemeList = lexemeList,
            snackbarState = SnackbarState(title = "Complex message", show = false)
        )
        
        // When
        val resultState = initialState.hideLoading()
        
        // Then
        // Main functionality check
        assertFalse("Loading should be disabled", resultState.isLoading)
        
        // Immutability checks - ALL complex state must remain unchanged
        assertEquals(
            "topBarState should not mutate on hideLoading() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on hideLoading() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "wordState should not mutate on hideLoading() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on hideLoading() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on hideLoading() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }
}
