package me.apomazkin.wordcard.mate.ext

import me.apomazkin.wordcard.mate.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Tests for extensions related to top bar state management
 * 
 * Test cases:
 * 1. Boundary case: showMenu enables menu when already open
 * 2. Standard case: showMenu enables menu when closed
 * 3. Standard case: showMenu enables menu with complex data
 * 4. Boundary case: hideMenu disables menu when already closed
 * 5. Standard case: hideMenu disables menu when open
 * 6. Standard case: hideMenu disables menu with complex data
 */
class TopBarExtTest {

    @Test
    fun `should enable menu when showMenu is called`() {
        // Test case 1: Boundary case - showMenu enables menu when already open
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true), // Already open
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.showMenu()
        
        // Then
        // Main functionality check
        assertTrue("Menu should be enabled", resultState.topBarState.isMenuOpen)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "addLexemeBottomState should not mutate on showMenu() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "closeScreen should not mutate on showMenu() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on showMenu() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on showMenu() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on showMenu() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on showMenu() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should enable menu when closed`() {
        // Test case 2: Standard case - showMenu enables menu when closed
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false), // Closed
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.showMenu()
        
        // Then
        // Main functionality check
        assertTrue("Menu should be enabled", resultState.topBarState.isMenuOpen)
        
        // Immutability checks
        assertEquals(
            "addLexemeBottomState should not mutate on showMenu() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "closeScreen should not mutate on showMenu() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on showMenu() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on showMenu() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on showMenu() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on showMenu() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should enable menu with complex data`() {
        // Test case 3: Standard case - showMenu enables menu with complex data
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
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(
                show = true,
                isTranslationCheck = true,
                isDefinitionCheck = false
            ),
            closeScreen = true,
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
            snackbarState = SnackbarState(title = "Test message", show = true)
        )
        
        // When
        val resultState = initialState.showMenu()
        
        // Then
        // Main functionality check
        assertTrue("Menu should be enabled", resultState.topBarState.isMenuOpen)
        
        // Immutability checks - ALL complex state must remain unchanged
        assertEquals(
            "addLexemeBottomState should not mutate on showMenu() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "closeScreen should not mutate on showMenu() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on showMenu() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on showMenu() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on showMenu() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on showMenu() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should disable menu when hideMenu is called`() {
        // Test case 4: Boundary case - hideMenu disables menu when already closed
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false), // Already closed
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.hideMenu()
        
        // Then
        // Main functionality check
        assertFalse("Menu should be disabled", resultState.topBarState.isMenuOpen)
        
        // Immutability checks
        assertEquals(
            "addLexemeBottomState should not mutate on hideMenu() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "closeScreen should not mutate on hideMenu() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on hideMenu() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on hideMenu() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on hideMenu() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on hideMenu() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should disable menu when open`() {
        // Test case 5: Standard case - hideMenu disables menu when open
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true), // Currently open
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.hideMenu()
        
        // Then
        // Main functionality check
        assertFalse("Menu should be disabled", resultState.topBarState.isMenuOpen)
        
        // Immutability checks
        assertEquals(
            "addLexemeBottomState should not mutate on hideMenu() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "closeScreen should not mutate on hideMenu() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on hideMenu() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on hideMenu() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on hideMenu() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on hideMenu() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should disable menu with complex data`() {
        // Test case 6: Standard case - hideMenu disables menu with complex data
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
            closeScreen = true,
            isLoading = false,
            wordState = WordState(
                id = 789L,
                value = "complex test",
                added = Date(2000L),
                isEditMode = false,
                edited = "",
                showWarningDialog = false
            ),
            lexemeList = lexemeList,
            snackbarState = SnackbarState(title = "Complex message", show = false)
        )
        
        // When
        val resultState = initialState.hideMenu()
        
        // Then
        // Main functionality check
        assertFalse("Menu should be disabled", resultState.topBarState.isMenuOpen)
        
        // Immutability checks - ALL complex state must remain unchanged
        assertEquals(
            "addLexemeBottomState should not mutate on hideMenu() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "closeScreen should not mutate on hideMenu() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on hideMenu() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on hideMenu() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on hideMenu() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on hideMenu() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }
}
