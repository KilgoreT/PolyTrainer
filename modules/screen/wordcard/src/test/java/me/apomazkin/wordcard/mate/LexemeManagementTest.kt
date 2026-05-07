package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.LexemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Standard case: OpenAddLexemeDialog shows dialog and resets checks
 * 2. Boundary case: OpenAddLexemeDialog when dialog already open
 * 3. Standard case: CloseAddLexemeDialog hides dialog and resets checks
 * 4. Boundary case: CloseAddLexemeDialog when dialog already closed
 * 5. Standard case: EnableTranslationCreation enables translation check
 * 6. Standard case: EnableTranslationCreation disables translation check
 * 7. Standard case: EnableDefinitionCreation enables definition check
 * 8. Standard case: EnableDefinitionCreation disables definition check
 * 9. Standard case: CreateLexeme triggers CreateLexeme effect
 * 10. Standard case: RefreshLexeme adds lexeme and closes dialog
 * 11. Standard case: RemoveLexeme triggers RemoveLexeme effect
 * 12. Standard case: OpenLexemeMenu opens lexeme menu
 * 13. Standard case: OpenLexemeMenu closes lexeme menu
 * 14. Edge case: OpenLexemeMenu with non-existent lexeme ID
 */
class LexemeManagementTest {

    @Test
    fun `should show dialog and reset checks when OpenAddLexemeDialog is received`() {
        // Test case 1: Standard case - OpenAddLexemeDialog shows dialog and resets checks
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(
                show = false, // Initially hidden
                isTranslationCheck = true, // Will be reset
                isDefinitionCheck = true // Will be reset
            ),
            closeScreen = false,
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState()
        )
        
        val message = Msg.OpenAddLexemeDialog
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Add lexeme dialog should be shown",
            result.state().addLexemeBottomState.show
        )
        assertFalse(
            "Translation check should be reset to false",
            result.state().addLexemeBottomState.isTranslationCheck
        )
        assertFalse(
            "Definition check should be reset to false",
            result.state().addLexemeBottomState.isDefinitionCheck
        )
        
        // Effects check
        result.assertNoEffects("OpenAddLexemeDialog should not produce any effects")
        
        // Other state properties should remain unchanged
        assertEquals(
            "topBarState should remain unchanged",
            initialState.topBarState,
            result.state().topBarState
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
            "wordState should remain unchanged",
            initialState.wordState,
            result.state().wordState
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
        // Test case 2: Boundary case - OpenAddLexemeDialog when dialog already open
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            addLexemeBottomState = AddLexemeBottomState(
                show = true, // Already open
                isTranslationCheck = true,
                isDefinitionCheck = false
            )
        )
        
        val message = Msg.OpenAddLexemeDialog
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check - should remain open but reset checks
        assertTrue(
            "Add lexeme dialog should remain shown",
            result.state().addLexemeBottomState.show
        )
        assertFalse(
            "Translation check should be reset to false",
            result.state().addLexemeBottomState.isTranslationCheck
        )
        assertFalse(
            "Definition check should be reset to false",
            result.state().addLexemeBottomState.isDefinitionCheck
        )
        
        // Effects check
        result.assertNoEffects("OpenAddLexemeDialog should not produce any effects")
    }

    @Test
    fun `should hide dialog and reset checks when CloseAddLexemeDialog is received`() {
        // Test case 3: Standard case - CloseAddLexemeDialog hides dialog and resets checks
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            addLexemeBottomState = AddLexemeBottomState(
                show = true, // Currently shown
                isTranslationCheck = true,
                isDefinitionCheck = false
            )
        )
        
        val message = Msg.CloseAddLexemeDialog
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertFalse(
            "Add lexeme dialog should be hidden",
            result.state().addLexemeBottomState.show
        )
        assertFalse(
            "Translation check should be reset to false",
            result.state().addLexemeBottomState.isTranslationCheck
        )
        assertFalse(
            "Definition check should be reset to false",
            result.state().addLexemeBottomState.isDefinitionCheck
        )
        
        // Effects check
        result.assertNoEffects("CloseAddLexemeDialog should not produce any effects")
    }

    @Test
    fun `should enable translation check when EnableTranslationCreation with true is received`() {
        // Test case 5: Standard case - EnableTranslationCreation enables translation check
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            addLexemeBottomState = AddLexemeBottomState(
                show = true,
                isTranslationCheck = false, // Initially disabled
                isDefinitionCheck = true
            )
        )
        
        val message = Msg.EnableTranslationCreation(isAdded = true)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Translation check should be enabled",
            result.state().addLexemeBottomState.isTranslationCheck
        )
        
        // Effects check
        result.assertNoEffects("EnableTranslationCreation should not produce any effects")
        
        // Other addLexemeBottomState properties should remain unchanged
        assertEquals(
            "Show state should remain unchanged",
            initialState.addLexemeBottomState.show,
            result.state().addLexemeBottomState.show
        )
        assertEquals(
            "Definition check should remain unchanged",
            initialState.addLexemeBottomState.isDefinitionCheck,
            result.state().addLexemeBottomState.isDefinitionCheck
        )
    }

    @Test
    fun `should disable translation check when EnableTranslationCreation with false is received`() {
        // Test case 6: Standard case - EnableTranslationCreation disables translation check
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            addLexemeBottomState = AddLexemeBottomState(
                show = true,
                isTranslationCheck = true, // Initially enabled
                isDefinitionCheck = false
            )
        )
        
        val message = Msg.EnableTranslationCreation(isAdded = false)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertFalse(
            "Translation check should be disabled",
            result.state().addLexemeBottomState.isTranslationCheck
        )
        
        // Effects check
        result.assertNoEffects("EnableTranslationCreation should not produce any effects")
    }

    @Test
    fun `should enable definition check when EnableDefinitionCreation with true is received`() {
        // Test case 7: Standard case - EnableDefinitionCreation enables definition check
        // Given
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            addLexemeBottomState = AddLexemeBottomState(
                show = true,
                isTranslationCheck = true,
                isDefinitionCheck = false // Initially disabled
            )
        )
        
        val message = Msg.EnableDefinitionCreation(isAdded = true)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertTrue(
            "Definition check should be enabled",
            result.state().addLexemeBottomState.isDefinitionCheck
        )
        
        // Effects check
        result.assertNoEffects("EnableDefinitionCreation should not produce any effects")
        
        // Other addLexemeBottomState properties should remain unchanged
        assertEquals(
            "Show state should remain unchanged",
            initialState.addLexemeBottomState.show,
            result.state().addLexemeBottomState.show
        )
        assertEquals(
            "Translation check should remain unchanged",
            initialState.addLexemeBottomState.isTranslationCheck,
            result.state().addLexemeBottomState.isTranslationCheck
        )
    }

    @Test
    fun `should trigger CreateLexeme effect when CreateLexeme is received`() {
        // Test case 9: Standard case - CreateLexeme triggers CreateLexeme effect
        // Given
        val reducer = WordCardReducer()
        val wordId = 123L
        val initialState = WordCardState(
            wordState = WordState(id = wordId, value = "test"),
            addLexemeBottomState = AddLexemeBottomState(
                show = true,
                isTranslationCheck = true,
                isDefinitionCheck = false
            )
        )
        
        val message = Msg.CreateLexeme
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // State should remain unchanged
        assertEquals(
            "State should remain unchanged",
            initialState,
            result.state()
        )
        
        // Effects check - should trigger CreateLexeme effect
        result.assertEffects(
            setOf(DatasourceEffect.CreateLexeme(wordId = wordId)),
            "Should trigger CreateLexeme effect with correct word ID"
        )
    }

    @Test
    fun `should add lexeme and close dialog when RefreshLexeme is received`() {
        // Test case 10: Standard case - RefreshLexeme adds lexeme and closes dialog
        // Given
        val reducer = WordCardReducer()
        val lexeme = Lexeme(
            lexemeId = LexemeId(456L),
            translation = null,
            definition = null,
            category = "noun",
            addDate = java.util.Date(),
            changeDate = null
        )
        
        val initialState = WordCardState(
            lexemeList = listOf(),
            addLexemeBottomState = AddLexemeBottomState(
                show = true,
                isTranslationCheck = true,
                isDefinitionCheck = false
            )
        )
        
        val message = Msg.RefreshLexeme(lexeme = lexeme)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertEquals(
            "Lexeme list should have one lexeme",
            1,
            result.state().lexemeList.size
        )
        
        val addedLexeme = result.state().lexemeList.first()
        assertEquals(
            "Added lexeme ID should match",
            456L,
            addedLexeme.id
        )
        assertNotNull(
            "Translation should be created because isTranslationCheck was true",
            addedLexeme.translation
        )
        assertEquals(
            "Translation should be empty",
            "",
            addedLexeme.translation?.origin
        )
        assertTrue(
            "Translation should be in edit mode for new field",
            addedLexeme.translation?.isEdit ?: false
        )
        assertNull(
            "Definition should be null because isDefinitionCheck was false",
            addedLexeme.definition
        )
        
        // Dialog should be closed and checks reset
        assertFalse(
            "Add lexeme dialog should be closed",
            result.state().addLexemeBottomState.show
        )
        assertFalse(
            "Translation check should be reset",
            result.state().addLexemeBottomState.isTranslationCheck
        )
        assertFalse(
            "Definition check should be reset",
            result.state().addLexemeBottomState.isDefinitionCheck
        )
        
        // Effects check
        result.assertNoEffects("RefreshLexeme should not produce any effects")
    }

    @Test
    fun `should trigger RemoveLexeme effect when RemoveLexeme is received`() {
        // Test case 11: Standard case - RemoveLexeme triggers RemoveLexeme effect
        // Given
        val reducer = WordCardReducer()
        val lexemeId = 456L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(id = lexemeId, translation = TextValueState(origin = "test", isEdit = false))
            )
        )
        
        val message = Msg.RemoveLexeme(lexemeId = lexemeId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // State should remain unchanged
        assertEquals(
            "State should remain unchanged",
            initialState,
            result.state()
        )
        
        // Effects check - should trigger RemoveLexeme effect
        result.assertEffects(
            setOf(DatasourceEffect.RemoveLexeme(lexemeId)),
            "Should trigger RemoveLexeme effect with correct lexeme ID"
        )
    }

    @Test
    fun `should open lexeme menu when OpenLexemeMenu with show true is received`() {
        // Test case 12: Standard case - OpenLexemeMenu opens lexeme menu
        // Given
        val reducer = WordCardReducer()
        val lexemeId = 123L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(origin = "test", isEdit = false),
                    isMenuOpen = false // Initially closed
                ),
                LexemeState(
                    id = 456L,
                    definition = TextValueState(origin = "def", isEdit = false),
                    isMenuOpen = true // Other lexeme menu is open
                )
            )
        )
        
        val message = Msg.OpenLexemeMenu(lexemeId = lexemeId, isShow = true)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertEquals(
            "Lexeme list size should remain unchanged",
            2,
            result.state().lexemeList.size
        )
        
        val targetLexeme = result.state().lexemeList.find { it.id == lexemeId }
        assertNotNull("Target lexeme should exist", targetLexeme)
        assertTrue(
            "Target lexeme menu should be opened",
            targetLexeme!!.isMenuOpen
        )
        
        // Other lexeme should remain unchanged
        val otherLexeme = result.state().lexemeList.find { it.id == 456L }
        assertNotNull("Other lexeme should exist", otherLexeme)
        assertTrue(
            "Other lexeme menu should remain open",
            otherLexeme!!.isMenuOpen
        )
        
        // Effects check
        result.assertNoEffects("OpenLexemeMenu should not produce any effects")
    }

    @Test
    fun `should close lexeme menu when OpenLexemeMenu with show false is received`() {
        // Test case 13: Standard case - OpenLexemeMenu closes lexeme menu
        // Given
        val reducer = WordCardReducer()
        val lexemeId = 123L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(origin = "test", isEdit = false),
                    isMenuOpen = true // Initially open
                )
            )
        )
        
        val message = Msg.OpenLexemeMenu(lexemeId = lexemeId, isShow = false)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        val targetLexeme = result.state().lexemeList.find { it.id == lexemeId }
        assertNotNull("Target lexeme should exist", targetLexeme)
        assertFalse(
            "Target lexeme menu should be closed",
            targetLexeme!!.isMenuOpen
        )
        
        // Effects check
        result.assertNoEffects("OpenLexemeMenu should not produce any effects")
    }

    @Test
    fun `should not change state when OpenLexemeMenu with non-existent lexeme ID is received`() {
        // Test case 14: Edge case - OpenLexemeMenu with non-existent lexeme ID
        // Given
        val reducer = WordCardReducer()
        val nonExistentLexemeId = 999L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = 123L,
                    translation = TextValueState(origin = "test", isEdit = false),
                    isMenuOpen = false
                )
            )
        )
        
        val message = Msg.OpenLexemeMenu(lexemeId = nonExistentLexemeId, isShow = true)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // State should remain unchanged
        assertEquals(
            "State should remain unchanged",
            initialState,
            result.state()
        )
        
        // Effects check
        result.assertNoEffects("OpenLexemeMenu should not produce any effects")
    }
}
