package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import me.apomazkin.wordcard.entity.Lexeme
import me.apomazkin.wordcard.entity.LexemeId
import me.apomazkin.wordcard.entity.Translation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Standard case: CreateTranslation creates translation in edit mode
 * 2. Edge case: CreateTranslation with non-existent lexeme ID
 * 3. Standard case: UpdateTranslationInput updates translation text
 * 4. Standard case: UpdateTranslationInput with empty text
 * 5. Edge case: UpdateTranslationInput with non-existent lexeme ID
 * 6. Standard case: EnterTranslationEditMode enables edit mode
 * 7. Boundary case: EnterTranslationEditMode when already in edit mode
 * 8. Edge case: EnterTranslationEditMode with non-existent lexeme ID
 * 9. Standard case: ExitTranslationEditMode triggers UpdateLexemeTranslation effect
 * 10. Standard case: ExitTranslationEditMode with empty edited text
 * 11. Edge case: ExitTranslationEditMode with non-existent lexeme ID
 * 12. Standard case: RefreshTranslation updates translation and exits edit mode
 * 13. Edge case: RefreshTranslation with non-existent lexeme ID
 * 14. Standard case: RemoveTranslation triggers RemoveTranslation effect
 */
class TranslationManagementTest {

    @Test
    fun `should create translation in edit mode when CreateTranslation is received`() {
        // Test case 1: Standard case - CreateTranslation creates translation in edit mode
        // Given
        val reducer = WordCardReducer()
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
        
        val message = Msg.CreateTranslation(lexemeId = lexemeId)
        
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
            "Translation should be created",
            updatedLexeme.translation
        )
        assertEquals(
            "Translation origin should be empty",
            "",
            updatedLexeme.translation?.origin
        )
        assertTrue(
            "Translation should be in edit mode",
            updatedLexeme.translation?.isEdit ?: false
        )
        
        // Other lexeme properties should remain unchanged
        assertEquals(
            "Lexeme ID should remain unchanged",
            lexemeId,
            updatedLexeme.id
        )
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
        
        // Effects check
        result.assertNoEffects("CreateTranslation should not produce any effects")
    }

    @Test
    fun `should not change state when CreateTranslation with non-existent lexeme ID is received`() {
        // Test case 2: Edge case - CreateTranslation with non-existent lexeme ID
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
        
        val message = Msg.CreateTranslation(lexemeId = nonExistentLexemeId)
        
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
        result.assertNoEffects("CreateTranslation should not produce any effects")
    }

    @Test
    fun `should update translation text when UpdateTranslationInput is received`() {
        // Test case 3: Standard case - UpdateTranslationInput updates translation text
        // Given
        val reducer = WordCardReducer()
        val lexemeId = 123L
        val newValue = "updated translation"
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(
                        origin = "original",
                        edited = "old edited",
                        isEdit = true
                    ),
                    definition = null,
                    isMenuOpen = false
                )
            )
        )
        
        val message = Msg.UpdateTranslationInput(lexemeId = lexemeId, value = newValue)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        val updatedLexeme = result.state().lexemeList.first()
        assertEquals(
            "Translation edited text should be updated",
            newValue,
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
        
        // Other lexeme properties should remain unchanged
        assertEquals(
            "Lexeme ID should remain unchanged",
            lexemeId,
            updatedLexeme.id
        )
        assertEquals(
            "Definition should remain unchanged",
            initialState.lexemeList.first().definition,
            updatedLexeme.definition
        )
        
        // Effects check
        result.assertNoEffects("UpdateTranslationInput should not produce any effects")
    }

    @Test
    fun `should update translation with empty text when UpdateTranslationInput is received`() {
        // Test case 4: Standard case - UpdateTranslationInput with empty text
        // Given
        val reducer = WordCardReducer()
        val lexemeId = 123L
        val emptyValue = ""
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
        
        val message = Msg.UpdateTranslationInput(lexemeId = lexemeId, value = emptyValue)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        val updatedLexeme = result.state().lexemeList.first()
        assertEquals(
            "Translation edited text should be empty",
            emptyValue,
            updatedLexeme.translation?.edited
        )
        
        // Effects check
        result.assertNoEffects("UpdateTranslationInput should not produce any effects")
    }

    @Test
    fun `should enable edit mode when EnterTranslationEditMode is received`() {
        // Test case 6: Standard case - EnterTranslationEditMode enables edit mode
        // Given
        val reducer = WordCardReducer()
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
        
        val message = Msg.EnterTranslationEditMode(lexemeId = lexemeId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        val updatedLexeme = result.state().lexemeList.first()
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
        
        // Effects check
        result.assertNoEffects("EnterTranslationEditMode should not produce any effects")
    }

    @Test
    fun `should remain in edit mode when EnterTranslationEditMode is received while already in edit mode`() {
        // Test case 7: Boundary case - EnterTranslationEditMode when already in edit mode
        // Given
        val reducer = WordCardReducer()
        val lexemeId = 123L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(
                        origin = "original translation",
                        edited = "edited text",
                        isEdit = true // Already in edit mode
                    )
                )
            )
        )
        
        val message = Msg.EnterTranslationEditMode(lexemeId = lexemeId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check - should remain in edit mode
        val updatedLexeme = result.state().lexemeList.first()
        assertTrue(
            "Translation should remain in edit mode",
            updatedLexeme.translation?.isEdit ?: false
        )
        
        // Effects check
        result.assertNoEffects("EnterTranslationEditMode should not produce any effects")
    }

    @Test
    fun `should trigger UpdateLexemeTranslation effect when ExitTranslationEditMode is received`() {
        // Test case 9: Standard case - ExitTranslationEditMode triggers UpdateLexemeTranslation effect
        // Given
        val reducer = WordCardReducer()
        val wordId = 456L
        val lexemeId = 123L
        val editedText = "edited translation"
        val initialState = WordCardState(
            wordState = WordState(id = wordId, value = "test"),
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(
                        origin = "original",
                        edited = editedText,
                        isEdit = true
                    )
                )
            )
        )
        
        val message = Msg.ExitTranslationEditMode(lexemeId = lexemeId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // State should remain unchanged
        assertEquals(
            "State should remain unchanged",
            initialState,
            result.state()
        )
        
        // Effects check - should trigger UpdateLexemeTranslation effect
        result.assertEffects(
            setOf(DatasourceEffect.UpdateLexemeTranslation(
                wordId = wordId,
                lexemeId = lexemeId,
                translation = editedText
            )),
            "Should trigger UpdateLexemeTranslation effect with correct parameters"
        )
    }

    @Test
    fun `should trigger UpdateLexemeTranslation effect with empty text when ExitTranslationEditMode is received`() {
        // Test case 10: Standard case - ExitTranslationEditMode with empty edited text
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
                    translation = TextValueState(
                        origin = "original",
                        edited = emptyEditedText,
                        isEdit = true
                    )
                )
            )
        )
        
        val message = Msg.ExitTranslationEditMode(lexemeId = lexemeId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Effects check - should trigger UpdateLexemeTranslation effect with empty text
        result.assertEffects(
            setOf(DatasourceEffect.UpdateLexemeTranslation(
                wordId = wordId,
                lexemeId = lexemeId,
                translation = emptyEditedText
            )),
            "Should trigger UpdateLexemeTranslation effect with empty translation"
        )
    }

    @Test
    fun `should update translation and exit edit mode when RefreshTranslation is received`() {
        // Test case 12: Standard case - RefreshTranslation updates translation and exits edit mode
        // Given
        val reducer = WordCardReducer()
        val lexemeId = 123L
        val newTranslationValue = "refreshed translation"
        val lexeme = Lexeme(
            lexemeId = LexemeId(lexemeId),
            translation = Translation(newTranslationValue),
            definition = null,
            category = "noun",
            addDate = java.util.Date(),
            changeDate = null
        )
        
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(
                        origin = "old translation",
                        edited = "edited translation",
                        isEdit = true // In edit mode
                    ),
                    definition = TextValueState(origin = "definition", isEdit = false)
                )
            )
        )
        
        val message = Msg.RefreshTranslation(lexeme = lexeme)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // Main functionality check
        val updatedLexeme = result.state().lexemeList.first()
        assertEquals(
            "Translation origin should be updated",
            newTranslationValue,
            updatedLexeme.translation?.origin
        )
        assertEquals(
            "Translation edited should remain unchanged",
            "edited translation",
            updatedLexeme.translation?.edited
        )
        assertFalse(
            "Translation should exit edit mode",
            updatedLexeme.translation?.isEdit ?: true
        )
        
        // Other lexeme properties should remain unchanged
        assertEquals(
            "Definition should remain unchanged",
            initialState.lexemeList.first().definition,
            updatedLexeme.definition
        )
        
        // Effects check
        result.assertNoEffects("RefreshTranslation should not produce any effects")
    }

    @Test
    fun `should not change state when RefreshTranslation with non-existent lexeme ID is received`() {
        // Test case 13: Edge case - RefreshTranslation with non-existent lexeme ID
        // Given
        val reducer = WordCardReducer()
        val nonExistentLexemeId = 999L
        val lexeme = Lexeme(
            lexemeId = LexemeId(nonExistentLexemeId),
            translation = Translation("new translation"),
            definition = null,
            category = "noun",
            addDate = java.util.Date(),
            changeDate = null
        )
        
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = 123L,
                    translation = TextValueState(origin = "existing", isEdit = false)
                )
            )
        )
        
        val message = Msg.RefreshTranslation(lexeme = lexeme)
        
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
        result.assertNoEffects("RefreshTranslation should not produce any effects")
    }

    @Test
    fun `should trigger RemoveTranslation effect when RemoveTranslation is received`() {
        // Test case 14: Standard case - RemoveTranslation triggers RemoveTranslation effect
        // Given
        val reducer = WordCardReducer()
        val lexemeId = 123L
        val initialState = WordCardState(
            lexemeList = listOf(
                LexemeState(
                    id = lexemeId,
                    translation = TextValueState(origin = "translation", isEdit = false)
                )
            )
        )
        
        val message = Msg.RemoveTranslation(lexemeId = lexemeId)
        
        // When
        val result = reducer.testReduce(initialState, message)
        
        // Then
        // State should remain unchanged
        assertEquals(
            "State should remain unchanged",
            initialState,
            result.state()
        )
        
        // Effects check - should trigger RemoveTranslation effect
        result.assertEffects(
            setOf(DatasourceEffect.RemoveTranslation(lexemeId)),
            "Should trigger RemoveTranslation effect with correct lexeme ID"
        )
    }
}
