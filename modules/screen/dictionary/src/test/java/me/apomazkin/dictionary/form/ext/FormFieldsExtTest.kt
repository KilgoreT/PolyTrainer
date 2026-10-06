package me.apomazkin.dictionary.form.ext

import me.apomazkin.dictionary.form.DictionaryFormScreenState
import me.apomazkin.dictionary.form.LanguageTarget
import me.apomazkin.dictionary.form.applyAllLanguages
import me.apomazkin.dictionary.form.chooseLanguage
import me.apomazkin.dictionary.form.deselectFlag
import me.apomazkin.dictionary.form.openLanguagePicker
import me.apomazkin.dictionary.form.prefillForEdit
import me.apomazkin.dictionary.form.selectFlag
import me.apomazkin.dictionary.form.updateFlagFilter
import me.apomazkin.dictionary.form.updateFlags
import me.apomazkin.dictionary.form.updateLanguageQuery
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
 * 1. Boundary: updateName with empty string disables save
 * 2. Standard: updateName with text sets name and enables save
 * 3. Edge: updateName with whitespace only disables save
 * 4. Standard: updateName preserves all other state fields
 * 5. Boundary: selectFlag on default state sets selectedFlag
 * 6. Standard: selectFlag replaces previously selected flag
 * 7. Standard: selectFlag preserves all other state fields
 * 8. Boundary: deselectFlag on default state (selectedFlag already null)
 * 9. Standard: deselectFlag clears selectedFlag
 * 10. Standard: deselectFlag preserves all other state fields
 * 11. Boundary: updateFlagFilter with empty string sets empty flagFilter
 * 12. Standard: updateFlagFilter with text sets flagFilter
 * 13. Standard: updateFlagFilter with whitespace sets flagFilter
 * 14. Standard: updateFlagFilter preserves all other state fields
 * 15. Boundary: updateFlags with empty list sets empty flags
 * 16. Standard: updateFlags with list sets flags
 * 17. Standard: updateFlags preserves all other state fields
 * 18. Standard: prefillForEdit with name and flag sets all three fields
 * 19. Standard: prefillForEdit with null flag sets name and null selectedFlag
 * 20. Edge: prefillForEdit with empty name disables save
 * 21. Standard: prefillForEdit preserves editingDictionaryId, flagFilter, flags
 *
 * IS525, языки:
 * 22. selectFlag подставляет основной язык страны
 * 23. selectFlag не трогает язык, выбранный вручную
 * 24. deselectFlag возвращает язык без флага (английский)
 * 25. deselectFlag не трогает язык, выбранный вручную
 * 26. prefillForEdit: «вручную» = язык отличается от языка флага
 * 27. chooseLanguage для изучаемого: ручной выбор, окно закрывается
 * 28. chooseLanguage языка флага — не ручной
 * 29. chooseLanguage для перевода не трогает признак «вручную»
 * 30. applyAllLanguages пересчитывает видимый список при открытом окне
 * 31. openLanguagePicker: языки страны сверху, без повторов, выбранный отмечен
 * 32. updateLanguageQuery фильтрует по названию и коду
 */
class FormFieldsExtTest {

    private val spanish = LanguageItem(tag = "es-ES", name = "Spanish (Spain)")
    private val mexican = LanguageItem(tag = "es-MX", name = "Spanish (Mexico)")
    private val english = LanguageItem(tag = "en", name = "English")
    private val russian = LanguageItem(tag = "ru", name = "Russian")
    private val spainFlag = CountryFlagItem(
        numericCode = 724, countryName = "Spain", flagRes = 100,
        languages = listOf("Spanish", "Catalan"),
        languageItems = listOf(spanish, LanguageItem(tag = "ca", name = "Catalan")),
    )
    private val mexicoFlag = CountryFlagItem(
        numericCode = 484, countryName = "Mexico", flagRes = 101,
        languages = listOf("Spanish"),
        languageItems = listOf(mexican),
    )
    private val flags = listOf(spainFlag, mexicoFlag)

    // === updateName ===

    @Test
    fun `should disable save when updateName with empty string`() {
        val initial = DictionaryFormScreenState(name = "Old", saveButtonEnabled = true)
        val result = initial.updateName("")

        assertEquals("name should be empty", "", result.name)
        assertFalse("saveButtonEnabled should be false", result.saveButtonEnabled)
    }

    @Test
    fun `should set name and enable save when updateName with text`() {
        val result = DictionaryFormScreenState().updateName("English")

        assertEquals("name should be English", "English", result.name)
        assertTrue("saveButtonEnabled should be true", result.saveButtonEnabled)
    }

    @Test
    fun `should disable save when updateName with whitespace only`() {
        val result = DictionaryFormScreenState().updateName("   ")

        assertEquals("name should be whitespace", "   ", result.name)
        assertFalse("saveButtonEnabled should be false", result.saveButtonEnabled)
    }

    @Test
    fun `should preserve other fields when updateName`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 5L,
            flagFilter = "spa",
            flags = flags,
            selectedFlag = spainFlag,
        )
        val result = initial.updateName("New Name")

        assertEquals("editingDictionaryId should not change", 5L, result.editingDictionaryId)
        assertEquals("flagFilter should not change", "spa", result.flagFilter)
        assertEquals("flags should not change", flags, result.flags)
        assertEquals("selectedFlag should not change", spainFlag, result.selectedFlag)
    }

    // === selectFlag ===

    @Test
    fun `should set selectedFlag when selectFlag on default state`() {
        val result = DictionaryFormScreenState().selectFlag(spainFlag)

        assertEquals("selectedFlag should be set", spainFlag, result.selectedFlag)
    }

    @Test
    fun `should replace flag when selectFlag with different flag`() {
        val initial = DictionaryFormScreenState(selectedFlag = spainFlag)
        val result = initial.selectFlag(mexicoFlag)

        assertEquals("selectedFlag should be mexico", mexicoFlag, result.selectedFlag)
    }

    @Test
    fun `should preserve other fields when selectFlag`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 3L,
            name = "Eng",
            flagFilter = "spa",
            flags = flags,
            saveButtonEnabled = true,
        )
        val result = initial.selectFlag(spainFlag)

        assertEquals("editingDictionaryId should not change", 3L, result.editingDictionaryId)
        assertEquals("name should not change", "Eng", result.name)
        assertEquals("flagFilter should not change", "spa", result.flagFilter)
        assertEquals("flags should not change", flags, result.flags)
        assertTrue("saveButtonEnabled should not change", result.saveButtonEnabled)
    }

    // === deselectFlag ===

    @Test
    fun `should remain null when deselectFlag on default state`() {
        val result = DictionaryFormScreenState().deselectFlag()

        assertNull("selectedFlag should remain null", result.selectedFlag)
    }

    @Test
    fun `should clear selectedFlag when deselectFlag`() {
        val initial = DictionaryFormScreenState(selectedFlag = spainFlag)
        val result = initial.deselectFlag()

        assertNull("selectedFlag should be null", result.selectedFlag)
    }

    @Test
    fun `should preserve other fields when deselectFlag`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 3L,
            name = "Eng",
            flagFilter = "spa",
            flags = flags,
            selectedFlag = spainFlag,
            saveButtonEnabled = true,
        )
        val result = initial.deselectFlag()

        assertEquals("editingDictionaryId should not change", 3L, result.editingDictionaryId)
        assertEquals("name should not change", "Eng", result.name)
        assertEquals("flagFilter should not change", "spa", result.flagFilter)
        assertEquals("flags should not change", flags, result.flags)
        assertTrue("saveButtonEnabled should not change", result.saveButtonEnabled)
    }

    // === updateFlagFilter ===

    @Test
    fun `should set empty flagFilter when updateFlagFilter with empty`() {
        val initial = DictionaryFormScreenState(flagFilter = "old")
        val result = initial.updateFlagFilter("")

        assertEquals("flagFilter should be empty", "", result.flagFilter)
    }

    @Test
    fun `should set flagFilter when updateFlagFilter with text`() {
        val result = DictionaryFormScreenState().updateFlagFilter("spa")

        assertEquals("flagFilter should be spa", "spa", result.flagFilter)
    }

    @Test
    fun `should set flagFilter when updateFlagFilter with whitespace`() {
        val result = DictionaryFormScreenState().updateFlagFilter("   ")

        assertEquals("flagFilter should be whitespace", "   ", result.flagFilter)
    }

    @Test
    fun `should preserve other fields when updateFlagFilter`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 3L,
            name = "Eng",
            flags = flags,
            selectedFlag = spainFlag,
            saveButtonEnabled = true,
        )
        val result = initial.updateFlagFilter("spa")

        assertEquals("editingDictionaryId should not change", 3L, result.editingDictionaryId)
        assertEquals("name should not change", "Eng", result.name)
        assertEquals("flags should not change", flags, result.flags)
        assertEquals("selectedFlag should not change", spainFlag, result.selectedFlag)
        assertTrue("saveButtonEnabled should not change", result.saveButtonEnabled)
    }

    // === updateFlags ===

    @Test
    fun `should set empty flags when updateFlags with empty list`() {
        val initial = DictionaryFormScreenState(flags = flags)
        val result = initial.updateFlags(emptyList())

        assertTrue("flags should be empty", result.flags.isEmpty())
    }

    @Test
    fun `should set flags when updateFlags with list`() {
        val result = DictionaryFormScreenState().updateFlags(flags)

        assertEquals("flags should have 2 items", 2, result.flags.size)
        assertEquals("flags should match", flags, result.flags)
    }

    @Test
    fun `should preserve other fields when updateFlags`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 3L,
            name = "Eng",
            flagFilter = "spa",
            selectedFlag = spainFlag,
            saveButtonEnabled = true,
        )
        val result = initial.updateFlags(flags)

        assertEquals("editingDictionaryId should not change", 3L, result.editingDictionaryId)
        assertEquals("name should not change", "Eng", result.name)
        assertEquals("flagFilter should not change", "spa", result.flagFilter)
        assertEquals("selectedFlag should not change", spainFlag, result.selectedFlag)
        assertTrue("saveButtonEnabled should not change", result.saveButtonEnabled)
    }

    // === prefillForEdit ===

    @Test
    fun `should set name flag and enable save when prefillForEdit`() {
        val initial = DictionaryFormScreenState(editingDictionaryId = 5L)
        val result = initial.prefillForEdit("English", spainFlag, spanish, russian)

        assertEquals("name should be English", "English", result.name)
        assertEquals("selectedFlag should be set", spainFlag, result.selectedFlag)
        assertTrue("saveButtonEnabled should be true", result.saveButtonEnabled)
    }

    @Test
    fun `should set name and null flag when prefillForEdit without flag`() {
        val initial = DictionaryFormScreenState(editingDictionaryId = 5L)
        val result = initial.prefillForEdit("English", null, english, russian)

        assertEquals("name should be English", "English", result.name)
        assertNull("selectedFlag should be null", result.selectedFlag)
        assertTrue("saveButtonEnabled should be true", result.saveButtonEnabled)
    }

    @Test
    fun `should disable save when prefillForEdit with empty name`() {
        val initial = DictionaryFormScreenState(editingDictionaryId = 5L)
        val result = initial.prefillForEdit("", spainFlag, spanish, russian)

        assertEquals("name should be empty", "", result.name)
        assertEquals("selectedFlag should be set", spainFlag, result.selectedFlag)
        assertFalse("saveButtonEnabled should be false", result.saveButtonEnabled)
    }

    @Test
    fun `should preserve other fields when prefillForEdit`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 5L,
            flagFilter = "spa",
            flags = flags,
        )
        val result = initial.prefillForEdit("English", spainFlag, spanish, russian)

        assertEquals("editingDictionaryId should not change", 5L, result.editingDictionaryId)
        assertEquals("flagFilter should not change", "spa", result.flagFilter)
        assertEquals("flags should not change", flags, result.flags)
    }

    // === IS525: языки ===

    @Test
    fun `selectFlag substitutes main country language`() {
        val result = DictionaryFormScreenState(noFlagLanguage = english).selectFlag(mexicoFlag)

        assertEquals(mexican, result.learningLanguage)
        assertFalse(result.isLearningLanguageManual)
    }

    @Test
    fun `selectFlag keeps manual language`() {
        val initial = DictionaryFormScreenState(
            learningLanguage = russian,
            isLearningLanguageManual = true,
        )
        val result = initial.selectFlag(mexicoFlag)

        assertEquals(russian, result.learningLanguage)
    }

    @Test
    fun `deselectFlag returns no-flag language`() {
        val initial = DictionaryFormScreenState(
            noFlagLanguage = english,
            selectedFlag = mexicoFlag,
            learningLanguage = mexican,
        )
        val result = initial.deselectFlag()

        assertEquals(english, result.learningLanguage)
    }

    @Test
    fun `deselectFlag keeps manual language`() {
        val initial = DictionaryFormScreenState(
            noFlagLanguage = english, selectedFlag = mexicoFlag,
            learningLanguage = russian, isLearningLanguageManual = true,
        )
        val result = initial.deselectFlag()

        assertEquals(russian, result.learningLanguage)
    }

    @Test
    fun `prefillForEdit marks language manual when it differs from flag`() {
        val initial = DictionaryFormScreenState(noFlagLanguage = english)

        val byFlag = initial.prefillForEdit("MX", mexicoFlag, mexican, russian)
        val manual = initial.prefillForEdit("MX", mexicoFlag, russian, russian)
        val noFlagEnglish = initial.prefillForEdit("Bio", null, english, russian)
        val noFlagOther = initial.prefillForEdit("Bio", null, spanish, russian)

        assertFalse(byFlag.isLearningLanguageManual)
        assertTrue(manual.isLearningLanguageManual)
        assertFalse(noFlagEnglish.isLearningLanguageManual)
        assertTrue(noFlagOther.isLearningLanguageManual)
    }

    @Test
    fun `chooseLanguage for learning sets manual and closes picker`() {
        val initial = DictionaryFormScreenState().openLanguagePicker(LanguageTarget.LEARNING)
        val result = initial.chooseLanguage(russian)

        assertEquals(russian, result.learningLanguage)
        assertTrue(result.isLearningLanguageManual)
        assertFalse(result.languagePicker.isOpen)
    }

    @Test
    fun `chooseLanguage of the flag language is not manual`() {
        val initial = DictionaryFormScreenState(selectedFlag = mexicoFlag, learningLanguage = russian)
            .openLanguagePicker(LanguageTarget.LEARNING)
        val result = initial.chooseLanguage(mexican)

        assertEquals(mexican, result.learningLanguage)
        assertFalse(result.isLearningLanguageManual)
    }

    @Test
    fun `chooseLanguage for translation does not touch manual flag`() {
        val initial = DictionaryFormScreenState().openLanguagePicker(LanguageTarget.TRANSLATION)
        val result = initial.chooseLanguage(russian)

        assertEquals(russian, result.translationLanguage)
        assertFalse(result.isLearningLanguageManual)
    }

    @Test
    fun `applyAllLanguages recomputes visible list when picker is open`() {
        val opened = DictionaryFormScreenState().openLanguagePicker(LanguageTarget.TRANSLATION)
        val result = opened.applyAllLanguages(listOf(english, russian))

        assertEquals(listOf(english, russian), result.languagePicker.visibleLanguages)
    }

    @Test
    fun `openLanguagePicker lists country languages first without duplicates`() {
        val initial = DictionaryFormScreenState(selectedFlag = spainFlag)
            .applyAllLanguages(listOf(english, russian, spanish))
        val result = initial.openLanguagePicker(LanguageTarget.LEARNING)

        assertEquals(
            listOf("es-ES", "ca", "en", "ru"),
            result.languagePicker.visibleLanguages.map { it.tag },
        )
        assertEquals("en", result.languagePicker.selectedTag)
    }

    @Test
    fun `updateLanguageQuery filters by name and tag`() {
        val initial = DictionaryFormScreenState()
            .applyAllLanguages(listOf(english, russian, spanish))
            .openLanguagePicker(LanguageTarget.TRANSLATION)

        val byName = initial.updateLanguageQuery("rus")
        val byTag = initial.updateLanguageQuery("es-")

        assertEquals(listOf(russian), byName.languagePicker.visibleLanguages)
        assertEquals(listOf(spanish), byTag.languagePicker.visibleLanguages)
    }
}
