package me.apomazkin.wordcard.mate.ext

import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.TextValueState
import me.apomazkin.wordcard.mate.WordCardState
import me.apomazkin.wordcard.mate.WordState
import me.apomazkin.wordcard.mate.closeAllEditModes
import me.apomazkin.wordcard.mate.createLexemeDefinition
import me.apomazkin.wordcard.mate.createLexemeTranslation
import me.apomazkin.wordcard.mate.enableLexemeDefinitionEdit
import me.apomazkin.wordcard.mate.enableLexemeTranslationEdit
import me.apomazkin.wordcard.mate.updateLexemeDefinitionText
import me.apomazkin.wordcard.mate.updateLexemeTranslationText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Tests for specialized lexeme/text extensions + новый хелпер closeAllEditModes.
 */
class SpecializedLexemeExtTest {

    private fun loaded(lexemes: List<LexemeState> = emptyList()): WordCardState =
        WordCardState(
            isLoading = false,
            wordState = WordState.Loaded(id = 1L, added = Date(0L), value = "w"),
            lexemeList = lexemes,
        )

    @Test
    fun `createLexemeTranslation produces empty editable TextValueState`() {
        val initial = loaded(listOf(LexemeState(id = 1L, translation = null)))

        val result = initial.createLexemeTranslation(1L)

        val lex = result.lexemeList.first()
        assertNotNull(lex.translation)
        assertEquals("", lex.translation?.origin)
        assertTrue(lex.translation?.isEdit ?: false)
    }

    @Test
    fun `createLexemeDefinition produces empty editable TextValueState`() {
        val initial = loaded(listOf(LexemeState(id = 1L, definition = null)))

        val result = initial.createLexemeDefinition(1L)

        val lex = result.lexemeList.first()
        assertNotNull(lex.definition)
        assertEquals("", lex.definition?.origin)
        assertTrue(lex.definition?.isEdit ?: false)
    }

    @Test
    fun `updateLexemeTranslationText updates edited`() {
        val initial = loaded(listOf(LexemeState(
            id = 1L,
            translation = TextValueState(origin = "", isEdit = true, edited = ""),
        )))

        val result = initial.updateLexemeTranslationText(1L, "abc")

        assertEquals("abc", result.lexemeList.first().translation?.edited)
    }

    @Test
    fun `updateLexemeDefinitionText updates edited`() {
        val initial = loaded(listOf(LexemeState(
            id = 1L,
            definition = TextValueState(origin = "", isEdit = true, edited = ""),
        )))

        val result = initial.updateLexemeDefinitionText(1L, "def")

        assertEquals("def", result.lexemeList.first().definition?.edited)
    }

    @Test
    fun `enableLexemeTranslationEdit sets isEdit true`() {
        val initial = loaded(listOf(LexemeState(
            id = 1L,
            translation = TextValueState(origin = "a", isEdit = false),
        )))

        val result = initial.enableLexemeTranslationEdit(1L)

        assertTrue(result.lexemeList.first().translation?.isEdit ?: false)
    }

    @Test
    fun `enableLexemeDefinitionEdit sets isEdit true`() {
        val initial = loaded(listOf(LexemeState(
            id = 1L,
            definition = TextValueState(origin = "a", isEdit = false),
        )))

        val result = initial.enableLexemeDefinitionEdit(1L)

        assertTrue(result.lexemeList.first().definition?.isEdit ?: false)
    }

    @Test
    fun `closeAllEditModes resets word edit and all chip edits`() {
        val initial = WordCardState(
            isLoading = false,
            wordState = WordState.Loaded(
                id = 1L, added = Date(0L), value = "w",
                isEditMode = true, edited = "wedit",
            ),
            lexemeList = listOf(
                LexemeState(
                    id = 1L,
                    translation = TextValueState(origin = "a", edited = "ta", isEdit = true),
                    definition = TextValueState(origin = "b", edited = "tb", isEdit = true),
                ),
                LexemeState(
                    id = 2L,
                    translation = TextValueState(origin = "c", edited = "tc", isEdit = true),
                ),
            ),
        )

        val result = initial.closeAllEditModes()

        val word = result.wordState as WordState.Loaded
        assertFalse(word.isEditMode)
        assertEquals("", word.edited)
        result.lexemeList.forEach { lex ->
            assertFalse(lex.translation?.isEdit ?: false)
            assertEquals("", lex.translation?.edited ?: "")
            assertFalse(lex.definition?.isEdit ?: false)
            assertEquals("", lex.definition?.edited ?: "")
        }
    }

    @Test
    fun `closeAllEditModes on NotLoaded leaves wordState untouched`() {
        val initial = WordCardState(
            isLoading = true,
            wordState = WordState.NotLoaded,
            lexemeList = emptyList(),
        )

        val result = initial.closeAllEditModes()

        assertTrue(result.wordState is WordState.NotLoaded)
    }
}
