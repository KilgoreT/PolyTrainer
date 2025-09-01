package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import me.apomazkin.wordcard.entity.Definition
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.LexemeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Standard case: CreateDefinition creates definition in edit mode
 * 2. Edge case: CreateDefinition with non-existent lexeme ID
 * 3. Standard case: UpdateDefinitionInput updates definition text
 * 4. Standard case: UpdateDefinitionInput with empty text
 * 5. Edge case: UpdateDefinitionInput with non-existent lexeme ID
 * 6. Standard case: EnterDefinitionEditMode enables edit mode
 * 7. Boundary case: EnterDefinitionEditMode when already in edit mode
 * 8. Edge case: EnterDefinitionEditMode with non-existent lexeme ID
 * 9. Standard case: ExitDefinitionEditMode triggers UpdateLexemeDefinition effect
 * 10. Standard case: ExitDefinitionEditMode with empty edited text
 * 11. Edge case: ExitDefinitionEditMode with non-existent lexeme ID
 * 12. Standard case: RefreshDefinition updates definition and exits edit mode
 * 13. Edge case: RefreshDefinition with non-existent lexeme ID
 * 14. Standard case: RemoveDefinition triggers RemoveDefinition effect
 */
class DefinitionManagementTest {

    @Test
    fun `should create definition in edit mode when CreateDefinition is received`() {
        // Test case 1: Standard case - CreateDefinition creates definition in edit mode
        // Given
        val reducer = WordCardReducer()
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
        
        val message = Msg.CreateDefinition(lexemeId = lexemeId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        assertEquals(
            "Lexeme list size should remain unchanged",
            1,
            result.state().lexemeList.size
        )
        
        val updatedLexeme = result.state().lexemeList.first()
        assertNotNull(
            "Definition should be created",
            updatedLexeme.definition
        )
        assertEquals(
            "Definition origin should be empty",
            "",
            updatedLexeme.definition?.origin
        )
        assertTrue(
            "Definition should be in edit mode",
            updatedLexeme.definition?.isEdit ?: false
        )
        
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
            "Menu open state should remain unchanged",
            initialState.lexemeList.first().isMenuOpen,
            updatedLexeme.isMenuOpen
        )
        
        // Effects check
        result.assertNoEffects("CreateDefinition should not produce any effects")
    }

    @Test
    fun `should not change state when CreateDefinition with non-existent lexeme ID is received`() {
        // Test case 2: Edge case - CreateDefinition with non-existent lexeme ID
        // Given
        val reducer = WordCardReducer()
        val nonExistentLexemeId = 999L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = 123L,
                    translation = null,
                    definition = null,
                    isMenuOpen = false
                )
            )
        )
        
        val message = Msg.CreateDefinition(lexemeId = nonExistentLexemeId)
        
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
        result.assertNoEffects("CreateDefinition should not produce any effects")
    }

    @Test
    fun `should update definition text when UpdateDefinitionInput is received`() {
        // Test case 3: Standard case - UpdateDefinitionInput updates definition text
        // Given
        val reducer = WordCardReducer()
        val lexemeId = 123L
        val newValue = "updated definition"
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = null,
                    definition = TextValueState(
                        origin = "original",
                        edited = "old edited",
                        isEdit = true
                    ),
                    isMenuOpen = false
                )
            )
        )
        
        val message = Msg.UpdateDefinitionInput(lexemeId = lexemeId, value = newValue)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        val updatedLexeme = result.state().lexemeList.first()
        assertEquals(
            "Definition edited text should be updated",
            newValue,
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
        
        // Effects check
        result.assertNoEffects("UpdateDefinitionInput should not produce any effects")
    }

    @Test
    fun `should update definition with empty text when UpdateDefinitionInput is received`() {
        // Test case 4: Standard case - UpdateDefinitionInput with empty text
        // Given
        val reducer = WordCardReducer()
        val lexemeId = 123L
        val emptyValue = ""
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    definition = TextValueState(
                        origin = "original",
                        edited = "previous text",
                        isEdit = true
                    )
                )
            )
        )
        
        val message = Msg.UpdateDefinitionInput(lexemeId = lexemeId, value = emptyValue)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        val updatedLexeme = result.state().lexemeList.first()
        assertEquals(
            "Definition edited text should be empty",
            emptyValue,
            updatedLexeme.definition?.edited
        )
        
        // Effects check
        result.assertNoEffects("UpdateDefinitionInput should not produce any effects")
    }

    @Test
    fun `should enable edit mode when EnterDefinitionEditMode is received`() {
        // Test case 6: Standard case - EnterDefinitionEditMode enables edit mode
        // Given
        val reducer = WordCardReducer()
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
        
        val message = Msg.EnterDefinitionEditMode(lexemeId = lexemeId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        val updatedLexeme = result.state().lexemeList.first()
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
        
        // Effects check
        result.assertNoEffects("EnterDefinitionEditMode should not produce any effects")
    }

    @Test
    fun `should remain in edit mode when EnterDefinitionEditMode is received while already in edit mode`() {
        // Test case 7: Boundary case - EnterDefinitionEditMode when already in edit mode
        // Given
        val reducer = WordCardReducer()
        val lexemeId = 123L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    definition = TextValueState(
                        origin = "original definition",
                        edited = "edited text",
                        isEdit = true // Already in edit mode
                    )
                )
            )
        )
        
        val message = Msg.EnterDefinitionEditMode(lexemeId = lexemeId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check - should remain in edit mode
        val updatedLexeme = result.state().lexemeList.first()
        assertTrue(
            "Definition should remain in edit mode",
            updatedLexeme.definition?.isEdit ?: false
        )
        
        // Effects check
        result.assertNoEffects("EnterDefinitionEditMode should not produce any effects")
    }

    @Test
    fun `should trigger UpdateLexemeDefinition effect when ExitDefinitionEditMode is received`() {
        // Test case 9: Standard case - ExitDefinitionEditMode triggers UpdateLexemeDefinition effect
        // Given
        val reducer = WordCardReducer()
        val wordId = 456L
        val lexemeId = 123L
        val editedText = "edited definition"
        val initialState = WordCardState(
            wordState = WordState(id = wordId, value = "test"),
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    definition = TextValueState(
                        origin = "original",
                        edited = editedText,
                        isEdit = true
                    )
                )
            )
        )
        
        val message = Msg.ExitDefinitionEditMode(lexemeId = lexemeId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // State should remain unchanged
        assertEquals(
            "State should remain unchanged",
            initialState,
            result.state()
        )
        
        // Effects check - should trigger UpdateLexemeDefinition effect
        result.assertEffects(
            setOf(DatasourceEffect.UpdateLexemeDefinition(
                wordId = wordId,
                lexemeId = lexemeId,
                definition = editedText
            )),
            "Should trigger UpdateLexemeDefinition effect with correct parameters"
        )
    }

    @Test
    fun `should trigger UpdateLexemeDefinition effect with empty text when ExitDefinitionEditMode is received`() {
        // Test case 10: Standard case - ExitDefinitionEditMode with empty edited text
        // Given
        val reducer = WordCardReducer()
        val wordId = 456L
        val lexemeId = 123L
        val emptyEditedText = ""
        val initialState = WordCardState(
            wordState = WordState(id = wordId, value = "test"),
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    definition = TextValueState(
                        origin = "original",
                        edited = emptyEditedText,
                        isEdit = true
                    )
                )
            )
        )
        
        val message = Msg.ExitDefinitionEditMode(lexemeId = lexemeId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Effects check - should trigger UpdateLexemeDefinition effect with empty text
        result.assertEffects(
            setOf(DatasourceEffect.UpdateLexemeDefinition(
                wordId = wordId,
                lexemeId = lexemeId,
                definition = emptyEditedText
            )),
            "Should trigger UpdateLexemeDefinition effect with empty definition"
        )
    }

    @Test
    fun `should update definition and exit edit mode when RefreshDefinition is received`() {
        // Test case 12: Standard case - RefreshDefinition updates definition and exits edit mode
        // Given
        val reducer = WordCardReducer()
        val lexemeId = 123L
        val newDefinitionValue = "refreshed definition"
        val lexeme = Lexeme(
            lexemeId = LexemeId(lexemeId),
            translation = null,
            definition = Definition(newDefinitionValue),
            category = "noun",
            addDate = java.util.Date(),
            changeDate = null
        )
        
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(origin = "translation", isEdit = false),
                    definition = TextValueState(
                        origin = "old definition",
                        edited = "edited definition",
                        isEdit = true // In edit mode
                    )
                )
            )
        )
        
        val message = Msg.RefreshDefinition(lexeme = lexeme)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        val updatedLexeme = result.state().lexemeList.first()
        assertEquals(
            "Definition origin should be updated",
            newDefinitionValue,
            updatedLexeme.definition?.origin
        )
        assertEquals(
            "Definition edited should remain unchanged",
            "edited definition",
            updatedLexeme.definition?.edited
        )
        assertFalse(
            "Definition should exit edit mode",
            updatedLexeme.definition?.isEdit ?: true
        )
        
        // Other lexeme properties should remain unchanged
        assertEquals(
            "Translation should remain unchanged",
            initialState.lexemeList.first().translation,
            updatedLexeme.translation
        )
        
        // Effects check
        result.assertNoEffects("RefreshDefinition should not produce any effects")
    }

    @Test
    fun `should not change state when RefreshDefinition with non-existent lexeme ID is received`() {
        // Test case 13: Edge case - RefreshDefinition with non-existent lexeme ID
        // Given
        val reducer = WordCardReducer()
        val nonExistentLexemeId = 999L
        val lexeme = Lexeme(
            lexemeId = LexemeId(nonExistentLexemeId),
            translation = null,
            definition = Definition("new definition"),
            category = "noun",
            addDate = java.util.Date(),
            changeDate = null
        )
        
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = 123L,
                    definition = TextValueState(origin = "existing", isEdit = false)
                )
            )
        )
        
        val message = Msg.RefreshDefinition(lexeme = lexeme)
        
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
        result.assertNoEffects("RefreshDefinition should not produce any effects")
    }

    @Test
    fun `should trigger RemoveDefinition effect when RemoveDefinition is received`() {
        // Test case 14: Standard case - RemoveDefinition triggers RemoveDefinition effect
        // Given
        val reducer = WordCardReducer()
        val lexemeId = 123L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    definition = TextValueState(origin = "definition", isEdit = false)
                )
            )
        )
        
        val message = Msg.RemoveDefinition(lexemeId = lexemeId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // State should remain unchanged
        assertEquals(
            "State should remain unchanged",
            initialState,
            result.state()
        )
        
        // Effects check - should trigger RemoveDefinition effect
        result.assertEffects(
            setOf(DatasourceEffect.RemoveDefinition(lexemeId)),
            "Should trigger RemoveDefinition effect with correct lexeme ID"
        )
    }
}
