package me.apomazkin.wordcard.mate.ext

import me.apomazkin.wordcard.mate.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Tests for extensions related to add lexeme bottom state management
 * 
 * Test cases:
 * 1. Boundary case: showAddLexemeBottom shows dialog when already shown
 * 2. Standard case: showAddLexemeBottom shows dialog when hidden
 * 3. Standard case: showAddLexemeBottom shows dialog with complex data
 * 4. Boundary case: hideAddLexemeBottom hides dialog when already hidden
 * 5. Standard case: hideAddLexemeBottom hides dialog when shown
 * 6. Standard case: hideAddLexemeBottom hides dialog with complex data
 * 7. Standard case: setTranslationCheck enables translation check
 * 8. Standard case: setTranslationCheck disables translation check
 * 9. Standard case: setDefinitionCheck enables definition check
 * 10. Standard case: setDefinitionCheck disables definition check
 */
class AddLexemeExtTest {

    @Test
    fun `should show add lexeme dialog when showAddLexemeBottom is called`() {
        // Test case 1: Boundary case - showAddLexemeBottom shows dialog when already shown
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(
                show = true, // Already shown
                isTranslationCheck = true,
                isDefinitionCheck = false
            ),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.showAddLexemeBottom()
        
        // Then
        // Main functionality check
        assertTrue("Add lexeme dialog should be shown", resultState.addLexemeBottomState.show)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "topBarState should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "closeScreen should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
        
        // AddLexemeBottomState other properties should remain unchanged
        assertEquals(
            "Translation check should remain unchanged",
            initialState.addLexemeBottomState.isTranslationCheck,
            resultState.addLexemeBottomState.isTranslationCheck
        )
        assertEquals(
            "Definition check should remain unchanged",
            initialState.addLexemeBottomState.isDefinitionCheck,
            resultState.addLexemeBottomState.isDefinitionCheck
        )
    }

    @Test
    fun `should show add lexeme dialog when hidden`() {
        // Test case 2: Standard case - showAddLexemeBottom shows dialog when hidden
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(
                show = false, // Hidden
                isTranslationCheck = false,
                isDefinitionCheck = true
            ),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.showAddLexemeBottom()
        
        // Then
        // Main functionality check
        assertTrue("Add lexeme dialog should be shown", resultState.addLexemeBottomState.show)
        
        // Immutability checks
        assertEquals(
            "topBarState should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "closeScreen should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should show add lexeme dialog with complex data`() {
        // Test case 3: Standard case - showAddLexemeBottom shows dialog with complex data
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
                show = false,
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
        val resultState = initialState.showAddLexemeBottom()
        
        // Then
        // Main functionality check
        assertTrue("Add lexeme dialog should be shown", resultState.addLexemeBottomState.show)
        
        // Immutability checks - ALL complex state must remain unchanged
        assertEquals(
            "topBarState should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "closeScreen should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on showAddLexemeBottom() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should hide add lexeme dialog when hideAddLexemeBottom is called`() {
        // Test case 4: Boundary case - hideAddLexemeBottom hides dialog when already hidden
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(
                show = false, // Already hidden
                isTranslationCheck = true,
                isDefinitionCheck = false
            ),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.hideAddLexemeBottom()
        
        // Then
        // Main functionality check
        assertFalse("Add lexeme dialog should be hidden", resultState.addLexemeBottomState.show)
        
        // Immutability checks
        assertEquals(
            "topBarState should not mutate on hideAddLexemeBottom() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "closeScreen should not mutate on hideAddLexemeBottom() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on hideAddLexemeBottom() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on hideAddLexemeBottom() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on hideAddLexemeBottom() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on hideAddLexemeBottom() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should hide add lexeme dialog when shown`() {
        // Test case 5: Standard case - hideAddLexemeBottom hides dialog when shown
        // Given
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(
                show = true, // Currently shown
                isTranslationCheck = false,
                isDefinitionCheck = true
            ),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.hideAddLexemeBottom()
        
        // Then
        // Main functionality check
        assertFalse("Add lexeme dialog should be hidden", resultState.addLexemeBottomState.show)
        
        // Immutability checks
        assertEquals(
            "topBarState should not mutate on hideAddLexemeBottom() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "closeScreen should not mutate on hideAddLexemeBottom() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on hideAddLexemeBottom() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on hideAddLexemeBottom() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on hideAddLexemeBottom() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on hideAddLexemeBottom() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should enable translation check when setTranslationCheck is called`() {
        // Test case 7: Standard case - setTranslationCheck enables translation check
        // Given
        val initialState = WordCardState(
            addLexemeBottomState = AddLexemeBottomState(
                show = true,
                isTranslationCheck = false,
                isDefinitionCheck = true
            )
        )
        
        // When
        val resultState = initialState.setTranslationCheck(true)
        
        // Then
        // Main functionality check
        assertTrue("Translation check should be enabled", resultState.addLexemeBottomState.isTranslationCheck)
        
        // Immutability checks - other addLexemeBottomState properties should remain unchanged
        assertEquals(
            "Show state should remain unchanged",
            initialState.addLexemeBottomState.show,
            resultState.addLexemeBottomState.show
        )
        assertEquals(
            "Definition check should remain unchanged",
            initialState.addLexemeBottomState.isDefinitionCheck,
            resultState.addLexemeBottomState.isDefinitionCheck
        )
        
        // Other state properties should remain unchanged
        assertEquals(
            "topBarState should not mutate on setTranslationCheck() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "closeScreen should not mutate on setTranslationCheck() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on setTranslationCheck() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on setTranslationCheck() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on setTranslationCheck() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on setTranslationCheck() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should disable translation check when setTranslationCheck is called`() {
        // Test case 8: Standard case - setTranslationCheck disables translation check
        // Given
        val initialState = WordCardState(
            addLexemeBottomState = AddLexemeBottomState(
                show = true,
                isTranslationCheck = true,
                isDefinitionCheck = false
            )
        )
        
        // When
        val resultState = initialState.setTranslationCheck(false)
        
        // Then
        // Main functionality check
        assertFalse("Translation check should be disabled", resultState.addLexemeBottomState.isTranslationCheck)
        
        // Immutability checks - other addLexemeBottomState properties should remain unchanged
        assertEquals(
            "Show state should remain unchanged",
            initialState.addLexemeBottomState.show,
            resultState.addLexemeBottomState.show
        )
        assertEquals(
            "Definition check should remain unchanged",
            initialState.addLexemeBottomState.isDefinitionCheck,
            resultState.addLexemeBottomState.isDefinitionCheck
        )
    }

    @Test
    fun `should enable definition check when setDefinitionCheck is called`() {
        // Test case 9: Standard case - setDefinitionCheck enables definition check
        // Given
        val initialState = WordCardState(
            addLexemeBottomState = AddLexemeBottomState(
                show = false,
                isTranslationCheck = true,
                isDefinitionCheck = false
            )
        )
        
        // When
        val resultState = initialState.setDefinitionCheck(true)
        
        // Then
        // Main functionality check
        assertTrue("Definition check should be enabled", resultState.addLexemeBottomState.isDefinitionCheck)
        
        // Immutability checks - other addLexemeBottomState properties should remain unchanged
        assertEquals(
            "Show state should remain unchanged",
            initialState.addLexemeBottomState.show,
            resultState.addLexemeBottomState.show
        )
        assertEquals(
            "Translation check should remain unchanged",
            initialState.addLexemeBottomState.isTranslationCheck,
            resultState.addLexemeBottomState.isTranslationCheck
        )
        
        // Other state properties should remain unchanged
        assertEquals(
            "topBarState should not mutate on setDefinitionCheck() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "closeScreen should not mutate on setDefinitionCheck() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on setDefinitionCheck() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on setDefinitionCheck() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "lexemeList should not mutate on setDefinitionCheck() - violates immutability",
            initialState.lexemeList,
            resultState.lexemeList
        )
        assertEquals(
            "snackbarState should not mutate on setDefinitionCheck() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should disable definition check when setDefinitionCheck is called`() {
        // Test case 10: Standard case - setDefinitionCheck disables definition check
        // Given
        val initialState = WordCardState(
            addLexemeBottomState = AddLexemeBottomState(
                show = false,
                isTranslationCheck = false,
                isDefinitionCheck = true
            )
        )
        
        // When
        val resultState = initialState.setDefinitionCheck(false)
        
        // Then
        // Main functionality check
        assertFalse("Definition check should be disabled", resultState.addLexemeBottomState.isDefinitionCheck)
        
        // Immutability checks - other addLexemeBottomState properties should remain unchanged
        assertEquals(
            "Show state should remain unchanged",
            initialState.addLexemeBottomState.show,
            resultState.addLexemeBottomState.show
        )
        assertEquals(
            "Translation check should remain unchanged",
            initialState.addLexemeBottomState.isTranslationCheck,
            resultState.addLexemeBottomState.isTranslationCheck
        )
    }
}
