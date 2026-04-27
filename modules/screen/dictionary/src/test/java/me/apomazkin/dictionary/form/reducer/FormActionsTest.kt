package me.apomazkin.dictionary.form.reducer

import me.apomazkin.dictionary.form.DictionaryFormEffect
import me.apomazkin.dictionary.form.DictionaryFormMsg
import me.apomazkin.dictionary.form.DictionaryFormReducer
import me.apomazkin.dictionary.form.DictionaryFormScreenState
import me.apomazkin.dictionary.form.LanguagePickerState
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.LanguageItem
import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.assertSingleEffect
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Standard: NameChanged with text → name set, save enabled
 * 2. Boundary: NameChanged with empty → save disabled
 * 3. Edge: NameChanged with whitespace → save disabled
 * 4. Standard: NameChanged preserves other fields
 * 5. Standard: ToggleLanguageBound enables from disabled
 * 6. Standard: ToggleLanguageBound disables and clears from enabled
 * 7. Standard: ToggleLanguageBound preserves name
 * 8. Standard: OpenLanguagePicker shows picker
 * 9. Standard: CloseLanguagePicker hides and clears query
 * 10. Standard: LanguageQueryChanged filters
 * 11. Boundary: LanguageQueryChanged empty shows all
 * 12. Edge: LanguageQueryChanged no match shows empty
 * 13. Standard: SelectLanguage selects and triggers LoadFlags
 * 14. Standard: SelectLanguage preserves name
 * 15. Standard: SelectFlag sets flag
 * 16. Standard: SelectFlag preserves other fields
 * 17. Standard: Save create mode → SaveDictionary effect
 * 18. Standard: Save create mode with flag → SaveDictionary with numericCode
 * 19. Standard: Save edit mode → UpdateDictionary effect
 * 20. Edge: Save edit mode with unbound language → null numericCode
 */
class FormActionsTest {

    private val reducer = DictionaryFormReducer()
    private val spanish = LanguageItem(code = "es", displayName = "Spanish")
    private val spainFlag = CountryFlagItem(numericCode = 724, countryName = "Spain", flagRes = 100)
    private val languages = listOf(
        LanguageItem("en", "English"),
        LanguageItem("es", "Spanish"),
        LanguageItem("fr", "French"),
    )

    // === NameChanged ===

    @Test
    fun `should set name and enable save when NameChanged with text`() {
        val result = reducer.testReduce(DictionaryFormScreenState(), DictionaryFormMsg.NameChanged("English"))

        assertEquals("name should be English", "English", result.state().name)
        assertTrue("save should be enabled", result.state().saveButtonEnabled)
        result.assertNoEffects()
    }

    @Test
    fun `should disable save when NameChanged with empty`() {
        val initial = DictionaryFormScreenState(name = "Old", saveButtonEnabled = true)
        val result = reducer.testReduce(initial, DictionaryFormMsg.NameChanged(""))

        assertFalse("save should be disabled", result.state().saveButtonEnabled)
        result.assertNoEffects()
    }

    @Test
    fun `should disable save when NameChanged with whitespace`() {
        val result = reducer.testReduce(DictionaryFormScreenState(), DictionaryFormMsg.NameChanged("   "))

        assertFalse("save should be disabled", result.state().saveButtonEnabled)
    }

    @Test
    fun `should preserve other fields when NameChanged`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 5,
            isLanguageBound = true,
            selectedLanguage = spanish,
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.NameChanged("New"))

        assertEquals("editingId should not change", 5L, result.state().editingDictionaryId)
        assertTrue("isLanguageBound should not change", result.state().isLanguageBound)
        assertEquals("selectedLanguage should not change", spanish, result.state().selectedLanguage)
    }

    // === ToggleLanguageBound ===

    @Test
    fun `should enable when ToggleLanguageBound from disabled`() {
        val initial = DictionaryFormScreenState(isLanguageBound = false)
        val result = reducer.testReduce(initial, DictionaryFormMsg.ToggleLanguageBound)

        assertTrue("isLanguageBound should be true", result.state().isLanguageBound)
        result.assertNoEffects()
    }

    @Test
    fun `should disable and clear when ToggleLanguageBound from enabled`() {
        val initial = DictionaryFormScreenState(
            isLanguageBound = true,
            selectedLanguage = spanish,
            availableFlags = listOf(spainFlag),
            selectedFlag = spainFlag,
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.ToggleLanguageBound)

        assertFalse("isLanguageBound should be false", result.state().isLanguageBound)
        assertNull("selectedLanguage should be null", result.state().selectedLanguage)
        assertTrue("availableFlags should be empty", result.state().availableFlags.isEmpty())
        assertNull("selectedFlag should be null", result.state().selectedFlag)
    }

    @Test
    fun `should preserve name when ToggleLanguageBound`() {
        val initial = DictionaryFormScreenState(name = "Eng", isLanguageBound = false)
        val result = reducer.testReduce(initial, DictionaryFormMsg.ToggleLanguageBound)

        assertEquals("name should not change", "Eng", result.state().name)
    }

    // === Language Picker ===

    @Test
    fun `should show picker when OpenLanguagePicker`() {
        val result = reducer.testReduce(DictionaryFormScreenState(), DictionaryFormMsg.OpenLanguagePicker)

        assertTrue("picker should be shown", result.state().languagePickerState.show)
        result.assertNoEffects()
    }

    @Test
    fun `should hide picker and clear query when CloseLanguagePicker`() {
        val initial = DictionaryFormScreenState(
            languagePickerState = LanguagePickerState(show = true, query = "test"),
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.CloseLanguagePicker)

        assertFalse("picker should be hidden", result.state().languagePickerState.show)
        assertEquals("query should be empty", "", result.state().languagePickerState.query)
    }

    @Test
    fun `should filter when LanguageQueryChanged`() {
        val initial = DictionaryFormScreenState(
            languagePickerState = LanguagePickerState(languages = languages, filteredLanguages = languages),
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.LanguageQueryChanged("Spa"))

        assertEquals("should have 1 result", 1, result.state().languagePickerState.filteredLanguages.size)
        assertEquals("query should be set", "Spa", result.state().languagePickerState.query)
    }

    @Test
    fun `should show all when LanguageQueryChanged with empty`() {
        val initial = DictionaryFormScreenState(
            languagePickerState = LanguagePickerState(languages = languages),
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.LanguageQueryChanged(""))

        assertEquals("should have all", 3, result.state().languagePickerState.filteredLanguages.size)
    }

    @Test
    fun `should show empty when LanguageQueryChanged no match`() {
        val initial = DictionaryFormScreenState(
            languagePickerState = LanguagePickerState(languages = languages),
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.LanguageQueryChanged("xyz"))

        assertTrue("should be empty", result.state().languagePickerState.filteredLanguages.isEmpty())
    }

    // === SelectLanguage ===

    @Test
    fun `should select language and trigger LoadFlags when SelectLanguage`() {
        val initial = DictionaryFormScreenState(
            languagePickerState = LanguagePickerState(show = true, query = "spa"),
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.SelectLanguage(spanish))

        assertEquals("selectedLanguage should be set", spanish, result.state().selectedLanguage)
        assertFalse("picker should be closed", result.state().languagePickerState.show)
        result.assertSingleEffect<DictionaryFormEffect.LoadFlagsForLanguage>()
        val effect = result.effects().first() as DictionaryFormEffect.LoadFlagsForLanguage
        assertEquals("language code should be es", "es", effect.languageCode)
    }

    @Test
    fun `should preserve name when SelectLanguage`() {
        val initial = DictionaryFormScreenState(
            name = "My Dict",
            languagePickerState = LanguagePickerState(show = true),
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.SelectLanguage(spanish))

        assertEquals("name should not change", "My Dict", result.state().name)
    }

    // === SelectFlag ===

    @Test
    fun `should set flag when SelectFlag`() {
        val result = reducer.testReduce(DictionaryFormScreenState(), DictionaryFormMsg.SelectFlag(spainFlag))

        assertEquals("selectedFlag should be set", spainFlag, result.state().selectedFlag)
        result.assertNoEffects()
    }

    @Test
    fun `should preserve other fields when SelectFlag`() {
        val initial = DictionaryFormScreenState(name = "Eng", selectedLanguage = spanish)
        val result = reducer.testReduce(initial, DictionaryFormMsg.SelectFlag(spainFlag))

        assertEquals("name should not change", "Eng", result.state().name)
        assertEquals("language should not change", spanish, result.state().selectedLanguage)
    }

    // === Save ===

    @Test
    fun `should trigger SaveDictionary when Save in create mode`() {
        val initial = DictionaryFormScreenState(editingDictionaryId = null, name = "Bio")
        val result = reducer.testReduce(initial, DictionaryFormMsg.Save)

        result.assertSingleEffect<DictionaryFormEffect.SaveDictionary>()
        val effect = result.effects().first() as DictionaryFormEffect.SaveDictionary
        assertEquals("name should be Bio", "Bio", effect.name)
        assertNull("numericCode should be null", effect.numericCode)
    }

    @Test
    fun `should trigger SaveDictionary with flag when Save create with flag`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = null,
            name = "Eng",
            selectedFlag = spainFlag,
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.Save)

        val effect = result.effects().first() as DictionaryFormEffect.SaveDictionary
        assertEquals("numericCode should be 724", 724, effect.numericCode)
    }

    @Test
    fun `should trigger UpdateDictionary when Save in edit mode`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 5,
            name = "Updated",
            selectedFlag = spainFlag,
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.Save)

        result.assertSingleEffect<DictionaryFormEffect.UpdateDictionary>()
        val effect = result.effects().first() as DictionaryFormEffect.UpdateDictionary
        assertEquals("id should be 5", 5L, effect.id)
        assertEquals("name should be Updated", "Updated", effect.name)
        assertEquals("numericCode should be 724", 724, effect.numericCode)
    }

    @Test
    fun `should send null numericCode when Save edit with unbound language`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 5,
            name = "Bio",
            isLanguageBound = false,
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.Save)

        val effect = result.effects().first() as DictionaryFormEffect.UpdateDictionary
        assertNull("numericCode should be null", effect.numericCode)
    }
}
