package me.apomazkin.wordcard.mate.ext

import me.apomazkin.wordcard.mate.AddLexemeBottomState
import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.SnackbarState
import me.apomazkin.wordcard.mate.TextValueState
import me.apomazkin.wordcard.mate.TopBarState
import me.apomazkin.wordcard.mate.WordCardState
import me.apomazkin.wordcard.mate.WordState
import me.apomazkin.wordcard.mate.createLexemeDefinition
import me.apomazkin.wordcard.mate.createLexemeTranslation
import me.apomazkin.wordcard.mate.enableLexemeDefinitionEdit
import me.apomazkin.wordcard.mate.enableLexemeTranslationEdit
import me.apomazkin.wordcard.mate.refreshLexemeDefinition
import me.apomazkin.wordcard.mate.refreshLexemeTranslation
import me.apomazkin.wordcard.mate.setLexemeMenuOpen
import me.apomazkin.wordcard.mate.updateLexemeDefinitionText
import me.apomazkin.wordcard.mate.updateLexemeTranslationText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for specialized lexeme extensions
 * 
 * Test cases:
 * 1. Standard case: setLexemeMenuOpen opens lexeme menu
 * 2. Standard case: setLexemeMenuOpen closes lexeme menu
 * 3. Edge case: setLexemeMenuOpen with non-existent lexeme ID
 * 4. Standard case: createLexemeTranslation creates translation in edit mode
 * 5. Edge case: createLexemeTranslation with non-existent lexeme ID
 * 6. Standard case: updateLexemeTranslationText updates translation text
 * 7. Standard case: updateLexemeTranslationText with empty text
 * 8. Edge case: updateLexemeTranslationText with non-existent lexeme ID
 * 9. Standard case: enableLexemeTranslationEdit enables translation edit mode
 * 10. Edge case: enableLexemeTranslationEdit with non-existent lexeme ID
 * 11. Standard case: refreshLexemeTranslation updates origin and disables edit
 * 12. Standard case: createLexemeDefinition creates definition in edit mode
 * 13. Standard case: updateLexemeDefinitionText updates definition text
 * 14. Standard case: enableLexemeDefinitionEdit enables definition edit mode
 * 15. Standard case: refreshLexemeDefinition updates origin and disables edit
 */
class SpecializedLexemeExtTest {

    @Test
    fun `should open lexeme menu when setLexemeMenuOpen with true is called`() {
        // Test case 1: Standard case - setLexemeMenuOpen opens lexeme menu
        // Given
        val lexemeId = 123L
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 456L, value = "test"),
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(origin = "translation", isEdit = false),
                    isMenuOpen = false // Initially closed
                )
            ),
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.setLexemeMenuOpen(lexemeId, true)
        
        // Then
        // Main functionality check
        val updatedLexeme = resultState.lexemeList.first { it.id == lexemeId }
        assertTrue("Lexeme menu should be opened", updatedLexeme.isMenuOpen)
        
        // Other lexeme properties should remain unchanged
        assertEquals(
            "Lexeme ID should remain unchanged",
            lexemeId,
            updatedLexeme.id
        )
        assertEquals(
            "Translation should remain unchanged",
            initialState.lexemeList.first().translation,
            updatedLexeme.translation
        )
        assertEquals(
            "Definition should remain unchanged",
            initialState.lexemeList.first().definition,
            updatedLexeme.definition
        )
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "topBarState should not mutate on setLexemeMenuOpen() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on setLexemeMenuOpen() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "closeScreen should not mutate on setLexemeMenuOpen() - violates immutability",
            initialState.closeScreen,
            resultState.closeScreen
        )
        assertEquals(
            "isLoading should not mutate on setLexemeMenuOpen() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on setLexemeMenuOpen() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "snackbarState should not mutate on setLexemeMenuOpen() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should close lexeme menu when setLexemeMenuOpen with false is called`() {
        // Test case 2: Standard case - setLexemeMenuOpen closes lexeme menu
        // Given
        val lexemeId = 123L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(origin = "translation", isEdit = false),
                    isMenuOpen = true // Initially open
                )
            )
        )
        
        // When
        val resultState = initialState.setLexemeMenuOpen(lexemeId, false)
        
        // Then
        // Main functionality check
        val updatedLexeme = resultState.lexemeList.first { it.id == lexemeId }
        assertFalse("Lexeme menu should be closed", updatedLexeme.isMenuOpen)
        
        // Other lexeme properties should remain unchanged
        assertEquals(
            "Translation should remain unchanged",
            initialState.lexemeList.first().translation,
            updatedLexeme.translation
        )
    }

    @Test
    fun `should not change state when setLexemeMenuOpen with non-existent lexeme ID is called`() {
        // Test case 3: Edge case - setLexemeMenuOpen with non-existent lexeme ID
        // Given
        val nonExistentLexemeId = 999L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = 123L,
                    translation = TextValueState(origin = "translation", isEdit = false),
                    isMenuOpen = false
                )
            )
        )
        
        // When
        val resultState = initialState.setLexemeMenuOpen(nonExistentLexemeId, true)
        
        // Then
        // State should remain unchanged
        assertEquals(
            "State should remain unchanged",
            initialState,
            resultState
        )
    }

    @Test
    fun `should create translation in edit mode when createLexemeTranslation is called`() {
        // Test case 4: Standard case - createLexemeTranslation creates translation in edit mode
        // Given
        val lexemeId = 123L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = null, // No translation initially
                    definition = TextValueState(origin = "definition", isEdit = false),
                    isMenuOpen = false
                )
            )
        )
        
        // When
        val resultState = initialState.createLexemeTranslation(lexemeId)
        
        // Then
        // Main functionality check
        val updatedLexeme = resultState.lexemeList.first { it.id == lexemeId }
        assertNotNull("Translation should be created", updatedLexeme.translation)
        assertEquals("Translation origin should be empty", "", updatedLexeme.translation?.origin)
        assertTrue("Translation should be in edit mode", updatedLexeme.translation?.isEdit ?: false)
        
        // Other lexeme properties should remain unchanged
        assertEquals(
            "Definition should remain unchanged",
            initialState.lexemeList.first().definition,
            updatedLexeme.definition
        )
        assertEquals(
            "Menu open state should remain unchanged",
            initialState.lexemeList.first().isMenuOpen,
            updatedLexeme.isMenuOpen
        )
    }

    @Test
    fun `should update translation text when updateLexemeTranslationText is called`() {
        // Test case 6: Standard case - updateLexemeTranslationText updates translation text
        // Given
        val lexemeId = 123L
        val newText = "updated translation"
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(
                        origin = "original",
                        edited = "old text",
                        isEdit = true
                    )
                )
            )
        )
        
        // When
        val resultState = initialState.updateLexemeTranslationText(lexemeId, newText)
        
        // Then
        // Main functionality check
        val updatedLexeme = resultState.lexemeList.first { it.id == lexemeId }
        assertEquals(
            "Translation edited text should be updated",
            newText,
            updatedLexeme.translation?.edited
        )
        
        // Other translation properties should remain unchanged
        assertEquals(
            "Translation origin should remain unchanged",
            "original",
            updatedLexeme.translation?.origin
        )
        assertTrue(
            "Translation should remain in edit mode",
            updatedLexeme.translation?.isEdit ?: false
        )
    }

    @Test
    fun `should update translation text with empty text when updateLexemeTranslationText is called`() {
        // Test case 7: Standard case - updateLexemeTranslationText with empty text
        // Given
        val lexemeId = 123L
        val emptyText = ""
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(
                        origin = "original",
                        edited = "previous text",
                        isEdit = true
                    )
                )
            )
        )
        
        // When
        val resultState = initialState.updateLexemeTranslationText(lexemeId, emptyText)
        
        // Then
        // Main functionality check
        val updatedLexeme = resultState.lexemeList.first { it.id == lexemeId }
        assertEquals(
            "Translation edited text should be empty",
            emptyText,
            updatedLexeme.translation?.edited
        )
    }

    @Test
    fun `should enable translation edit mode when enableLexemeTranslationEdit is called`() {
        // Test case 9: Standard case - enableLexemeTranslationEdit enables translation edit mode
        // Given
        val lexemeId = 123L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(
                        origin = "original translation",
                        edited = "",
                        isEdit = false // Not in edit mode
                    )
                )
            )
        )
        
        // When
        val resultState = initialState.enableLexemeTranslationEdit(lexemeId)
        
        // Then
        // Main functionality check
        val updatedLexeme = resultState.lexemeList.first { it.id == lexemeId }
        assertTrue(
            "Translation should be in edit mode",
            updatedLexeme.translation?.isEdit ?: false
        )
        
        // Other translation properties should remain unchanged
        assertEquals(
            "Translation origin should remain unchanged",
            "original translation",
            updatedLexeme.translation?.origin
        )
        assertEquals(
            "Translation edited should remain unchanged",
            "",
            updatedLexeme.translation?.edited
        )
    }

    @Test
    fun `should refresh translation origin and disable edit when refreshLexemeTranslation is called`() {
        // Test case 11: Standard case - refreshLexemeTranslation updates origin and disables edit
        // Given
        val lexemeId = 123L
        val newOrigin = "refreshed translation"
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(
                        origin = "old translation",
                        edited = "edited translation",
                        isEdit = true // In edit mode
                    )
                )
            )
        )
        
        // When
        val resultState = initialState.refreshLexemeTranslation(lexemeId, newOrigin)
        
        // Then
        // Main functionality check
        val updatedLexeme = resultState.lexemeList.first { it.id == lexemeId }
        assertEquals(
            "Translation origin should be updated",
            newOrigin,
            updatedLexeme.translation?.origin
        )
        assertFalse(
            "Translation should exit edit mode",
            updatedLexeme.translation?.isEdit ?: true
        )
        
        // Translation edited should remain unchanged (not reset to origin)
        assertEquals(
            "Translation edited should remain unchanged",
            "edited translation",
            updatedLexeme.translation?.edited
        )
    }

    @Test
    fun `should create definition in edit mode when createLexemeDefinition is called`() {
        // Test case 12: Standard case - createLexemeDefinition creates definition in edit mode
        // Given
        val lexemeId = 123L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(origin = "translation", isEdit = false),
                    definition = null, // No definition initially
                    isMenuOpen = false
                )
            )
        )
        
        // When
        val resultState = initialState.createLexemeDefinition(lexemeId)
        
        // Then
        // Main functionality check
        val updatedLexeme = resultState.lexemeList.first { it.id == lexemeId }
        assertNotNull("Definition should be created", updatedLexeme.definition)
        assertEquals("Definition origin should be empty", "", updatedLexeme.definition?.origin)
        assertTrue("Definition should be in edit mode", updatedLexeme.definition?.isEdit ?: false)
        
        // Other lexeme properties should remain unchanged
        assertEquals(
            "Translation should remain unchanged",
            initialState.lexemeList.first().translation,
            updatedLexeme.translation
        )
        assertEquals(
            "Menu open state should remain unchanged",
            initialState.lexemeList.first().isMenuOpen,
            updatedLexeme.isMenuOpen
        )
    }

    @Test
    fun `should update definition text when updateLexemeDefinitionText is called`() {
        // Test case 13: Standard case - updateLexemeDefinitionText updates definition text
        // Given
        val lexemeId = 123L
        val newText = "updated definition"
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    definition = TextValueState(
                        origin = "original",
                        edited = "old text",
                        isEdit = true
                    )
                )
            )
        )
        
        // When
        val resultState = initialState.updateLexemeDefinitionText(lexemeId, newText)
        
        // Then
        // Main functionality check
        val updatedLexeme = resultState.lexemeList.first { it.id == lexemeId }
        assertEquals(
            "Definition edited text should be updated",
            newText,
            updatedLexeme.definition?.edited
        )
        
        // Other definition properties should remain unchanged
        assertEquals(
            "Definition origin should remain unchanged",
            "original",
            updatedLexeme.definition?.origin
        )
        assertTrue(
            "Definition should remain in edit mode",
            updatedLexeme.definition?.isEdit ?: false
        )
    }

    @Test
    fun `should enable definition edit mode when enableLexemeDefinitionEdit is called`() {
        // Test case 14: Standard case - enableLexemeDefinitionEdit enables definition edit mode
        // Given
        val lexemeId = 123L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    definition = TextValueState(
                        origin = "original definition",
                        edited = "",
                        isEdit = false // Not in edit mode
                    )
                )
            )
        )
        
        // When
        val resultState = initialState.enableLexemeDefinitionEdit(lexemeId)
        
        // Then
        // Main functionality check
        val updatedLexeme = resultState.lexemeList.first { it.id == lexemeId }
        assertTrue(
            "Definition should be in edit mode",
            updatedLexeme.definition?.isEdit ?: false
        )
        
        // Other definition properties should remain unchanged
        assertEquals(
            "Definition origin should remain unchanged",
            "original definition",
            updatedLexeme.definition?.origin
        )
        assertEquals(
            "Definition edited should remain unchanged",
            "",
            updatedLexeme.definition?.edited
        )
    }

    @Test
    fun `should refresh definition origin and disable edit when refreshLexemeDefinition is called`() {
        // Test case 15: Standard case - refreshLexemeDefinition updates origin and disables edit
        // Given
        val lexemeId = 123L
        val newOrigin = "refreshed definition"
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    definition = TextValueState(
                        origin = "old definition",
                        edited = "edited definition",
                        isEdit = true // In edit mode
                    )
                )
            )
        )
        
        // When
        val resultState = initialState.refreshLexemeDefinition(lexemeId, newOrigin)
        
        // Then
        // Main functionality check
        val updatedLexeme = resultState.lexemeList.first { it.id == lexemeId }
        assertEquals(
            "Definition origin should be updated",
            newOrigin,
            updatedLexeme.definition?.origin
        )
        assertFalse(
            "Definition should exit edit mode",
            updatedLexeme.definition?.isEdit ?: true
        )
        
        // Definition edited should remain unchanged (not reset to origin)
        assertEquals(
            "Definition edited should remain unchanged",
            "edited definition",
            updatedLexeme.definition?.edited
        )
    }
}
