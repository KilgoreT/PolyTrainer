package me.apomazkin.wordcard.mate.ext

import me.apomazkin.wordcard.mate.AddLexemeBottomState
import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.SnackbarState
import me.apomazkin.wordcard.mate.TextValueState
import me.apomazkin.wordcard.mate.TopBarState
import me.apomazkin.wordcard.mate.WordCardState
import me.apomazkin.wordcard.mate.WordState
import me.apomazkin.wordcard.mate.addLexeme
import me.apomazkin.wordcard.mate.removeLexeme
import me.apomazkin.wordcard.mate.setLexemeList
import me.apomazkin.wordcard.mate.toggleLexemeMenu
import me.apomazkin.wordcard.mate.updateLexeme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for extensions related to lexeme list management
 * 
 * Test cases:
 * 1. Standard case: setLexemeList sets new lexeme list
 * 2. Standard case: setLexemeList sets empty lexeme list
 * 3. Standard case: addLexeme adds lexeme to empty list
 * 4. Standard case: addLexeme adds lexeme to existing list
 * 5. Standard case: updateLexeme updates existing lexeme
 * 6. Edge case: updateLexeme with non-existent lexeme ID
 * 7. Standard case: removeLexeme removes existing lexeme
 * 8. Edge case: removeLexeme with non-existent lexeme ID
 * 9. Standard case: toggleLexemeMenu opens closed menu
 * 10. Standard case: toggleLexemeMenu closes open menu
 * 11. Edge case: toggleLexemeMenu with non-existent lexeme ID
 */
class LexemeExtTest {

    @Test
    fun `should set lexeme list when setLexemeList is called`() {
        // Test case 1: Standard case - setLexemeList sets new lexeme list
        // Given
        val newLexemeList = listOf(
            LexemeState(
                id = 1L,
                translation = TextValueState(origin = "translation1", isEdit = false),
                definition = TextValueState(origin = "definition1", isEdit = true),
                isMenuOpen = false
            ),
            LexemeState(
                id = 2L,
                translation = null,
                definition = TextValueState(origin = "definition2", isEdit = false),
                isMenuOpen = true
            )
        )
        
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(), // Empty list initially
            snackbarState = SnackbarState()
        )
        
        // When
        val resultState = initialState.setLexemeList(newLexemeList)
        
        // Then
        // Main functionality check
        assertEquals("Lexeme list should be set", newLexemeList, resultState.lexemeList)
        assertEquals("Lexeme list size should be 2", 2, resultState.lexemeList.size)
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "topBarState should not mutate on setLexemeList() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on setLexemeList() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "isLoading should not mutate on setLexemeList() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on setLexemeList() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "snackbarState should not mutate on setLexemeList() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should set empty lexeme list when setLexemeList is called`() {
        // Test case 2: Standard case - setLexemeList sets empty lexeme list
        // Given
        val existingLexemeList = listOf(
            LexemeState(id = 1L, translation = TextValueState(origin = "existing", isEdit = false))
        )
        
        val initialState = WordCardState(
            lexemeList = existingLexemeList
        )
        
        // When
        val resultState = initialState.setLexemeList(emptyList())
        
        // Then
        // Main functionality check
        assertTrue("Lexeme list should be empty", resultState.lexemeList.isEmpty())
        assertEquals("Lexeme list size should be 0", 0, resultState.lexemeList.size)
    }

    @Test
    fun `should add lexeme to empty list when addLexeme is called`() {
        // Test case 3: Standard case - addLexeme adds lexeme to empty list
        // Given
        val newLexeme = LexemeState(
            id = 1L,
            translation = TextValueState(origin = "new translation", isEdit = false),
            definition = null,
            isMenuOpen = false
        )
        
        val initialState = WordCardState(
            lexemeList = emptyList()
        )
        
        // When
        val resultState = initialState.addLexeme(newLexeme)
        
        // Then
        // Main functionality check
        assertEquals("Lexeme list size should be 1", 1, resultState.lexemeList.size)
        assertEquals("Added lexeme should match", newLexeme, resultState.lexemeList.first())
        
        // Immutability checks
        assertEquals(
            "topBarState should not mutate on addLexeme() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on addLexeme() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "isLoading should not mutate on addLexeme() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on addLexeme() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "snackbarState should not mutate on addLexeme() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should add lexeme to existing list when addLexeme is called`() {
        // Test case 4: Standard case - addLexeme adds lexeme to existing list
        // Given
        val existingLexeme = LexemeState(
            id = 1L,
            translation = TextValueState(origin = "existing", isEdit = false)
        )
        
        val newLexeme = LexemeState(
            id = 2L,
            definition = TextValueState(origin = "new definition", isEdit = true)
        )
        
        val initialState = WordCardState(
            lexemeList = listOf(existingLexeme)
        )
        
        // When
        val resultState = initialState.addLexeme(newLexeme)
        
        // Then
        // Main functionality check
        assertEquals("Lexeme list size should be 2", 2, resultState.lexemeList.size)
        assertEquals("First lexeme should remain unchanged", existingLexeme, resultState.lexemeList[0])
        assertEquals("Second lexeme should be added", newLexeme, resultState.lexemeList[1])
    }

    @Test
    fun `should update existing lexeme when updateLexeme is called`() {
        // Test case 5: Standard case - updateLexeme updates existing lexeme
        // Given
        val originalLexeme = LexemeState(
            id = 1L,
            translation = TextValueState(origin = "original", isEdit = false),
            definition = null,
            isMenuOpen = false
        )
        
        val anotherLexeme = LexemeState(
            id = 2L,
            translation = TextValueState(origin = "another", isEdit = false)
        )
        
        val initialState = WordCardState(
            lexemeList = listOf(originalLexeme, anotherLexeme)
        )
        
        val updateFunction: (LexemeState) -> LexemeState = { lexeme ->
            lexeme.copy(
                translation = TextValueState(origin = "updated", isEdit = true),
                isMenuOpen = true
            )
        }
        
        // When
        val resultState = initialState.updateLexeme(1L, updateFunction)
        
        // Then
        // Main functionality check
        assertEquals("Lexeme list size should remain 2", 2, resultState.lexemeList.size)
        
        val updatedLexeme = resultState.lexemeList[0]
        assertEquals("Lexeme ID should remain unchanged", 1L, updatedLexeme.id)
        assertEquals("Translation should be updated", "updated", updatedLexeme.translation?.origin)
        assertTrue("Translation should be in edit mode", updatedLexeme.translation?.isEdit ?: false)
        assertTrue("Menu should be open", updatedLexeme.isMenuOpen)
        
        // Second lexeme should remain unchanged
        assertEquals("Second lexeme should remain unchanged", anotherLexeme, resultState.lexemeList[1])
    }

    @Test
    fun `should not change list when updateLexeme is called with non-existent ID`() {
        // Test case 6: Edge case - updateLexeme with non-existent lexeme ID
        // Given
        val existingLexeme = LexemeState(
            id = 1L,
            translation = TextValueState(origin = "existing", isEdit = false)
        )
        
        val initialState = WordCardState(
            lexemeList = listOf(existingLexeme)
        )
        
        val updateFunction: (LexemeState) -> LexemeState = { lexeme ->
            lexeme.copy(translation = TextValueState(origin = "should not be applied", isEdit = true))
        }
        
        // When
        val resultState = initialState.updateLexeme(999L, updateFunction) // Non-existent ID
        
        // Then
        // Main functionality check
        assertEquals("Lexeme list should remain unchanged", initialState.lexemeList, resultState.lexemeList)
        assertEquals("Existing lexeme should remain unchanged", existingLexeme, resultState.lexemeList[0])
    }

    @Test
    fun `should remove existing lexeme when removeLexeme is called`() {
        // Test case 7: Standard case - removeLexeme removes existing lexeme
        // Given
        val lexemeToRemove = LexemeState(
            id = 1L,
            translation = TextValueState(origin = "to remove", isEdit = false)
        )
        
        val lexemeToKeep = LexemeState(
            id = 2L,
            definition = TextValueState(origin = "to keep", isEdit = false)
        )
        
        val initialState = WordCardState(
            lexemeList = listOf(lexemeToRemove, lexemeToKeep)
        )
        
        // When
        val resultState = initialState.removeLexeme(1L)
        
        // Then
        // Main functionality check
        assertEquals("Lexeme list size should be 1", 1, resultState.lexemeList.size)
        assertEquals("Remaining lexeme should be the one to keep", lexemeToKeep, resultState.lexemeList[0])
        
        // Immutability checks
        assertEquals(
            "topBarState should not mutate on removeLexeme() - violates immutability",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "addLexemeBottomState should not mutate on removeLexeme() - violates immutability",
            initialState.addLexemeBottomState,
            resultState.addLexemeBottomState
        )
        assertEquals(
            "isLoading should not mutate on removeLexeme() - violates immutability",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "wordState should not mutate on removeLexeme() - violates immutability",
            initialState.wordState,
            resultState.wordState
        )
        assertEquals(
            "snackbarState should not mutate on removeLexeme() - violates immutability",
            initialState.snackbarState,
            resultState.snackbarState
        )
    }

    @Test
    fun `should not change list when removeLexeme is called with non-existent ID`() {
        // Test case 8: Edge case - removeLexeme with non-existent lexeme ID
        // Given
        val existingLexeme = LexemeState(
            id = 1L,
            translation = TextValueState(origin = "existing", isEdit = false)
        )
        
        val initialState = WordCardState(
            lexemeList = listOf(existingLexeme)
        )
        
        // When
        val resultState = initialState.removeLexeme(999L) // Non-existent ID
        
        // Then
        // Main functionality check
        assertEquals("Lexeme list should remain unchanged", initialState.lexemeList, resultState.lexemeList)
        assertEquals("Existing lexeme should remain unchanged", existingLexeme, resultState.lexemeList[0])
    }

    @Test
    fun `should toggle lexeme menu from closed to open when toggleLexemeMenu is called`() {
        // Test case 9: Standard case - toggleLexemeMenu opens closed menu
        // Given
        val lexemeWithClosedMenu = LexemeState(
            id = 1L,
            translation = TextValueState(origin = "test", isEdit = false),
            isMenuOpen = false
        )
        
        val anotherLexeme = LexemeState(
            id = 2L,
            definition = TextValueState(origin = "another", isEdit = false),
            isMenuOpen = true
        )
        
        val initialState = WordCardState(
            lexemeList = listOf(lexemeWithClosedMenu, anotherLexeme)
        )
        
        // When
        val resultState = initialState.toggleLexemeMenu(1L)
        
        // Then
        // Main functionality check
        assertEquals("Lexeme list size should remain 2", 2, resultState.lexemeList.size)
        
        val toggledLexeme = resultState.lexemeList[0]
        assertTrue("Target lexeme menu should be opened", toggledLexeme.isMenuOpen)
        assertEquals("Target lexeme ID should remain unchanged", 1L, toggledLexeme.id)
        assertEquals("Target lexeme translation should remain unchanged", "test", toggledLexeme.translation?.origin)
        
        // Other lexeme should remain unchanged
        assertEquals("Other lexeme should remain unchanged", anotherLexeme, resultState.lexemeList[1])
    }

    @Test
    fun `should toggle lexeme menu from open to closed when toggleLexemeMenu is called`() {
        // Test case 10: Standard case - toggleLexemeMenu closes open menu
        // Given
        val lexemeWithOpenMenu = LexemeState(
            id = 1L,
            translation = TextValueState(origin = "test", isEdit = false),
            isMenuOpen = true
        )
        
        val initialState = WordCardState(
            lexemeList = listOf(lexemeWithOpenMenu)
        )
        
        // When
        val resultState = initialState.toggleLexemeMenu(1L)
        
        // Then
        // Main functionality check
        val toggledLexeme = resultState.lexemeList[0]
        assertFalse("Target lexeme menu should be closed", toggledLexeme.isMenuOpen)
        assertEquals("Target lexeme ID should remain unchanged", 1L, toggledLexeme.id)
        assertEquals("Target lexeme translation should remain unchanged", "test", toggledLexeme.translation?.origin)
    }

    @Test
    fun `should not change list when toggleLexemeMenu is called with non-existent ID`() {
        // Test case 11: Edge case - toggleLexemeMenu with non-existent lexeme ID
        // Given
        val existingLexeme = LexemeState(
            id = 1L,
            translation = TextValueState(origin = "existing", isEdit = false),
            isMenuOpen = false
        )
        
        val initialState = WordCardState(
            lexemeList = listOf(existingLexeme)
        )
        
        // When
        val resultState = initialState.toggleLexemeMenu(999L) // Non-existent ID
        
        // Then
        // Main functionality check
        assertEquals("Lexeme list should remain unchanged", initialState.lexemeList, resultState.lexemeList)
        assertEquals("Existing lexeme should remain unchanged", existingLexeme, resultState.lexemeList[0])
        assertFalse("Existing lexeme menu should remain closed", resultState.lexemeList[0].isMenuOpen)
    }
}
