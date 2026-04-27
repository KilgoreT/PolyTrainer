package me.apomazkin.dictionary.form.ext

import me.apomazkin.dictionary.form.DictionaryFormScreenState
import me.apomazkin.dictionary.form.LanguagePickerState
import me.apomazkin.dictionary.form.filterLanguages
import me.apomazkin.dictionary.form.hideLanguagePicker
import me.apomazkin.dictionary.form.showLanguagePicker
import me.apomazkin.dictionary.model.LanguageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Boundary: showLanguagePicker sets show to true
 * 2. Standard: showLanguagePicker preserves languages
 * 3. Boundary: hideLanguagePicker sets show to false and clears query
 * 4. Standard: hideLanguagePicker preserves languages
 * 5. Standard: filterLanguages filters by displayName
 * 6. Boundary: filterLanguages with empty returns all
 * 7. Edge: filterLanguages with no match returns empty
 * 8. Standard: filterLanguages preserves show state
 */
class LanguagePickerExtTest {

    private val languages = listOf(
        LanguageItem(code = "en", displayName = "English"),
        LanguageItem(code = "es", displayName = "Spanish"),
        LanguageItem(code = "fr", displayName = "French"),
    )

    private fun stateWithLanguages() = DictionaryFormScreenState(
        languagePickerState = LanguagePickerState(
            languages = languages,
            filteredLanguages = languages,
        )
    )

    @Test
    fun `should set show to true when showLanguagePicker`() {
        // Test case 1
        val result = stateWithLanguages().showLanguagePicker()
        assertTrue("show should be true", result.languagePickerState.show)
    }

    @Test
    fun `should preserve languages when showLanguagePicker`() {
        // Test case 2
        val initial = stateWithLanguages()
        val result = initial.showLanguagePicker()
        assertEquals("languages should not change", languages, result.languagePickerState.languages)
    }

    @Test
    fun `should hide and clear query when hideLanguagePicker`() {
        // Test case 3
        val initial = stateWithLanguages().copy(
            languagePickerState = stateWithLanguages().languagePickerState.copy(
                show = true,
                query = "spa",
            )
        )
        val result = initial.hideLanguagePicker()

        assertFalse("show should be false", result.languagePickerState.show)
        assertEquals("query should be empty", "", result.languagePickerState.query)
    }

    @Test
    fun `should preserve languages when hideLanguagePicker`() {
        // Test case 4
        val initial = stateWithLanguages().copy(
            languagePickerState = stateWithLanguages().languagePickerState.copy(show = true)
        )
        val result = initial.hideLanguagePicker()
        assertEquals("languages should not change", languages, result.languagePickerState.languages)
    }

    @Test
    fun `should filter by displayName when filterLanguages`() {
        // Test case 5
        val result = stateWithLanguages().filterLanguages("Spa")

        assertEquals("should have 1 result", 1, result.languagePickerState.filteredLanguages.size)
        assertEquals("should be Spanish", "Spanish", result.languagePickerState.filteredLanguages[0].displayName)
        assertEquals("query should be set", "Spa", result.languagePickerState.query)
    }

    @Test
    fun `should return all when filterLanguages with empty query`() {
        // Test case 6
        val result = stateWithLanguages().filterLanguages("")

        assertEquals("should have all 3", 3, result.languagePickerState.filteredLanguages.size)
    }

    @Test
    fun `should return empty when filterLanguages with no match`() {
        // Test case 7
        val result = stateWithLanguages().filterLanguages("xyz")

        assertTrue("should be empty", result.languagePickerState.filteredLanguages.isEmpty())
    }

    @Test
    fun `should preserve show state when filterLanguages`() {
        // Test case 8
        val initial = stateWithLanguages().copy(
            languagePickerState = stateWithLanguages().languagePickerState.copy(show = true)
        )
        val result = initial.filterLanguages("en")

        assertTrue("show should not change", result.languagePickerState.show)
    }
}
