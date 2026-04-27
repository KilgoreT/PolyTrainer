package me.apomazkin.dictionary.form.ext

import me.apomazkin.dictionary.form.DictionaryFormScreenState
import me.apomazkin.dictionary.form.LanguagePickerState
import me.apomazkin.dictionary.form.selectFlag
import me.apomazkin.dictionary.form.selectLanguage
import me.apomazkin.dictionary.form.toggleLanguageBound
import me.apomazkin.dictionary.form.updateName
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.LanguageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Standard: updateName sets name and enables save
 * 2. Boundary: updateName with empty disables save
 * 3. Edge: updateName with whitespace only disables save
 * 4. Standard: updateName preserves other form fields
 * 5. Standard: toggleLanguageBound enables when disabled
 * 6. Standard: toggleLanguageBound disables and clears when enabled
 * 7. Standard: toggleLanguageBound preserves name
 * 8. Standard: selectLanguage sets language and closes picker
 * 9. Standard: selectLanguage preserves name
 * 10. Standard: selectFlag sets flag
 * 11. Standard: selectFlag preserves other fields
 */
class FormFieldsExtTest {

    private val spanish = LanguageItem(code = "es", displayName = "Spanish")
    private val spainFlag = CountryFlagItem(numericCode = 724, countryName = "Spain", flagRes = 100)
    private val mexicoFlag = CountryFlagItem(numericCode = 484, countryName = "Mexico", flagRes = 101)

    // === updateName ===

    @Test
    fun `should set name and enable save when updateName with text`() {
        // Test case 1
        val initial = DictionaryFormScreenState()
        val result = initial.updateName("English")

        assertEquals("name should be English", "English", result.name)
        assertTrue("saveButtonEnabled should be true", result.saveButtonEnabled)
    }

    @Test
    fun `should disable save when updateName with empty`() {
        // Test case 2
        val initial = DictionaryFormScreenState(name = "Old", saveButtonEnabled = true)
        val result = initial.updateName("")

        assertEquals("name should be empty", "", result.name)
        assertFalse("saveButtonEnabled should be false", result.saveButtonEnabled)
    }

    @Test
    fun `should disable save when updateName with whitespace only`() {
        // Test case 3
        val result = DictionaryFormScreenState().updateName("   ")

        assertEquals("name should be whitespace", "   ", result.name)
        assertFalse("saveButtonEnabled should be false", result.saveButtonEnabled)
    }

    @Test
    fun `should preserve other form fields when updateName`() {
        // Test case 4
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 5L,
            isLanguageBound = true,
            selectedLanguage = spanish,
        )
        val result = initial.updateName("New Name")

        assertEquals("editingDictionaryId should not change", 5L, result.editingDictionaryId)
        assertTrue("isLanguageBound should not change", result.isLanguageBound)
        assertEquals("selectedLanguage should not change", spanish, result.selectedLanguage)
    }

    // === toggleLanguageBound ===

    @Test
    fun `should enable when toggleLanguageBound from disabled`() {
        // Test case 5
        val initial = DictionaryFormScreenState(isLanguageBound = false)
        val result = initial.toggleLanguageBound()

        assertTrue("isLanguageBound should be true", result.isLanguageBound)
    }

    @Test
    fun `should disable and clear all when toggleLanguageBound from enabled`() {
        // Test case 6
        val initial = DictionaryFormScreenState(
            isLanguageBound = true,
            selectedLanguage = spanish,
            availableFlags = listOf(spainFlag, mexicoFlag),
            selectedFlag = spainFlag,
        )
        val result = initial.toggleLanguageBound()

        assertFalse("isLanguageBound should be false", result.isLanguageBound)
        assertNull("selectedLanguage should be null", result.selectedLanguage)
        assertTrue("availableFlags should be empty", result.availableFlags.isEmpty())
        assertNull("selectedFlag should be null", result.selectedFlag)
    }

    @Test
    fun `should preserve name when toggleLanguageBound`() {
        // Test case 7
        val initial = DictionaryFormScreenState(name = "English", isLanguageBound = false)
        val result = initial.toggleLanguageBound()

        assertEquals("name should not change", "English", result.name)
    }

    // === selectLanguage ===

    @Test
    fun `should select language and close picker when selectLanguage`() {
        // Test case 8
        val initial = DictionaryFormScreenState(
            languagePickerState = LanguagePickerState(show = true, query = "spa")
        )
        val result = initial.selectLanguage(spanish)

        assertEquals("selectedLanguage should be set", spanish, result.selectedLanguage)
        assertFalse("picker should be closed", result.languagePickerState.show)
        assertEquals("query should be cleared", "", result.languagePickerState.query)
    }

    @Test
    fun `should preserve name when selectLanguage`() {
        // Test case 9
        val initial = DictionaryFormScreenState(
            name = "My Dict",
            languagePickerState = LanguagePickerState(show = true),
        )
        val result = initial.selectLanguage(spanish)

        assertEquals("name should not change", "My Dict", result.name)
    }

    // === selectFlag ===

    @Test
    fun `should set selectedFlag when selectFlag`() {
        // Test case 10
        val initial = DictionaryFormScreenState(selectedLanguage = spanish)
        val result = initial.selectFlag(spainFlag)

        assertEquals("selectedFlag should be set", spainFlag, result.selectedFlag)
    }

    @Test
    fun `should preserve other fields when selectFlag`() {
        // Test case 11
        val initial = DictionaryFormScreenState(
            name = "Eng",
            selectedLanguage = spanish,
        )
        val result = initial.selectFlag(spainFlag)

        assertEquals("name should not change", "Eng", result.name)
        assertEquals("selectedLanguage should not change", spanish, result.selectedLanguage)
    }
}
