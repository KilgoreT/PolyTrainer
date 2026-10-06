package me.apomazkin.dictionary.form.reducer

import me.apomazkin.dictionary.form.DictionaryFormEffect
import me.apomazkin.dictionary.form.FlagFilterEffect
import io.github.kilgoret.mate.NavigationEffect
import me.apomazkin.dictionary.form.DictionaryFormMsg
import me.apomazkin.dictionary.form.DictionaryFormReducer
import me.apomazkin.dictionary.form.DictionaryFormScreenState
import me.apomazkin.dictionary.form.LanguageTarget
import me.apomazkin.dictionary.form.applyAllLanguages
import me.apomazkin.dictionary.form.openLanguagePicker
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.LanguageItem
import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.assertSingleEffect
import io.github.kilgoret.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Boundary: NameChanged with empty → save disabled
 * 2. Standard: NameChanged with text → name set, save enabled
 * 3. Edge: NameChanged with whitespace → save disabled
 * 4. Standard: NameChanged preserves other fields
 * 5. Boundary: FlagFilterChanged empty → FilterFlags("")
 * 6. Standard: FlagFilterChanged with text → FilterFlags("spa")
 * 7. Standard: FlagFilterChanged preserves other fields
 * 8. Boundary: SelectFlag on empty selection → select
 * 9. Standard: SelectFlag with different flag → switch
 * 10. Standard: SelectFlag with already selected → deselect (toggle off)
 * 11. Standard: SelectFlag preserves other fields
 * 12. Standard: Save create mode without flag → SaveDictionary
 * 13. Standard: Save create mode with flag → SaveDictionary with numericCode
 * 14. Standard: Save edit mode → UpdateDictionary
 * 15. Edge: Save edit mode without flag → UpdateDictionary with null numericCode
 * 16. Standard: Back on default → Back effect
 * 17. Standard: Back with data → Back effect, state preserved
 * 18. Boundary: Empty → no-op
 *
 * IS525, языки:
 * 19. Save (создание) несёт оба кода языка
 * 20. Save (правка) несёт оба кода языка
 * 21. SelectFlag подставляет язык через редьюсер
 * 22. OpenLanguagePicker открывает окно для цели
 * 23. CloseLanguagePicker закрывает окно
 * 24. LanguageQueryChanged фильтрует видимый список
 * 25. SelectLanguage применяет выбор и закрывает окно
 */
class FormActionsTest {

    private val reducer = DictionaryFormReducer()
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

    // === NameChanged ===

    @Test
    fun `should disable save when NameChanged with empty`() {
        val initial = DictionaryFormScreenState(name = "Old", saveButtonEnabled = true)
        val result = reducer.testReduce(initial, DictionaryFormMsg.NameChanged(""))

        assertEquals("name should be empty", "", result.state().name)
        assertFalse("save should be disabled", result.state().saveButtonEnabled)
        result.assertNoEffects()
    }

    @Test
    fun `should set name and enable save when NameChanged with text`() {
        val result = reducer.testReduce(
            DictionaryFormScreenState(),
            DictionaryFormMsg.NameChanged("English"),
        )

        assertEquals("name should be English", "English", result.state().name)
        assertTrue("save should be enabled", result.state().saveButtonEnabled)
        result.assertNoEffects()
    }

    @Test
    fun `should disable save when NameChanged with whitespace`() {
        val result = reducer.testReduce(
            DictionaryFormScreenState(),
            DictionaryFormMsg.NameChanged("   "),
        )

        assertFalse("save should be disabled", result.state().saveButtonEnabled)
        result.assertNoEffects()
    }

    @Test
    fun `should preserve other fields when NameChanged`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 5,
            selectedFlag = spainFlag,
            flagFilter = "spa",
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.NameChanged("New"))

        assertEquals("editingId should not change", 5L, result.state().editingDictionaryId)
        assertEquals("selectedFlag should not change", spainFlag, result.state().selectedFlag)
        assertEquals("flagFilter should not change", "spa", result.state().flagFilter)
    }

    // === FlagFilterChanged ===

    @Test
    fun `should set empty flagFilter and emit FilterFlags when FlagFilterChanged empty`() {
        val result = reducer.testReduce(
            DictionaryFormScreenState(flagFilter = "old"),
            DictionaryFormMsg.FlagFilterChanged(""),
        )

        assertEquals("flagFilter should be empty", "", result.state().flagFilter)
        result.assertSingleEffect<FlagFilterEffect.FilterFlags>()
        val effect = result.effects().first() as FlagFilterEffect.FilterFlags
        assertEquals("effect query should be empty", "", effect.query)
    }

    @Test
    fun `should set flagFilter and emit FilterFlags when FlagFilterChanged with text`() {
        val result = reducer.testReduce(
            DictionaryFormScreenState(),
            DictionaryFormMsg.FlagFilterChanged("spa"),
        )

        assertEquals("flagFilter should be spa", "spa", result.state().flagFilter)
        result.assertSingleEffect<FlagFilterEffect.FilterFlags>()
        val effect = result.effects().first() as FlagFilterEffect.FilterFlags
        assertEquals("effect query should be spa", "spa", effect.query)
    }

    @Test
    fun `should preserve other fields when FlagFilterChanged`() {
        val initial = DictionaryFormScreenState(
            name = "Eng",
            selectedFlag = spainFlag,
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.FlagFilterChanged("spa"))

        assertEquals("name should not change", "Eng", result.state().name)
        assertEquals("selectedFlag should not change", spainFlag, result.state().selectedFlag)
    }

    // === SelectFlag ===

    @Test
    fun `should select flag when SelectFlag on empty selection`() {
        val result = reducer.testReduce(
            DictionaryFormScreenState(),
            DictionaryFormMsg.SelectFlag(spainFlag),
        )

        assertEquals("selectedFlag should be set", spainFlag, result.state().selectedFlag)
        result.assertNoEffects()
    }

    @Test
    fun `should switch flag when SelectFlag with different flag`() {
        val initial = DictionaryFormScreenState(selectedFlag = spainFlag)
        val result = reducer.testReduce(initial, DictionaryFormMsg.SelectFlag(mexicoFlag))

        assertEquals("selectedFlag should be mexico", mexicoFlag, result.state().selectedFlag)
        result.assertNoEffects()
    }

    @Test
    fun `should deselect flag when SelectFlag with already selected flag`() {
        val initial = DictionaryFormScreenState(selectedFlag = spainFlag)
        val result = reducer.testReduce(initial, DictionaryFormMsg.SelectFlag(spainFlag))

        assertNull("selectedFlag should be null", result.state().selectedFlag)
        result.assertNoEffects()
    }

    @Test
    fun `should preserve other fields when SelectFlag`() {
        val initial = DictionaryFormScreenState(
            name = "Eng",
            flags = listOf(spainFlag, mexicoFlag),
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.SelectFlag(spainFlag))

        assertEquals("name should not change", "Eng", result.state().name)
        assertEquals("flags should not change", 2, result.state().flags.size)
    }

    // === Save ===

    @Test
    fun `should emit SaveDictionary without flag when Save in create mode`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = null,
            name = "Bio",
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.Save)

        result.assertSingleEffect<DictionaryFormEffect.SaveDictionary>()
        val effect = result.effects().first() as DictionaryFormEffect.SaveDictionary
        assertEquals("name should be Bio", "Bio", effect.name)
        assertNull("numericCode should be null", effect.numericCode)
    }

    @Test
    fun `should emit SaveDictionary with flag when Save in create mode with flag`() {
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
    fun `should emit UpdateDictionary when Save in edit mode`() {
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
    fun `should emit UpdateDictionary with null numericCode when Save edit without flag`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 5,
            name = "Bio",
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.Save)

        val effect = result.effects().first() as DictionaryFormEffect.UpdateDictionary
        assertNull("numericCode should be null", effect.numericCode)
    }

    // === IS525: языки ===

    @Test
    fun `Save carries both language tags`() {
        val initial = DictionaryFormScreenState(
            name = "MX",
            selectedFlag = mexicoFlag,
            learningLanguage = mexican,
            translationLanguage = russian,
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.Save)

        val effect = result.effects().first() as DictionaryFormEffect.SaveDictionary
        assertEquals("es-MX", effect.learningLanguage)
        assertEquals("ru", effect.translationLanguage)
    }

    @Test
    fun `Save in edit mode carries both language tags`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 5,
            name = "MX",
            learningLanguage = mexican,
            translationLanguage = russian,
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.Save)

        val effect = result.effects().first() as DictionaryFormEffect.UpdateDictionary
        assertEquals("es-MX", effect.learningLanguage)
        assertEquals("ru", effect.translationLanguage)
    }

    @Test
    fun `SelectFlag substitutes language via reducer`() {
        val initial = DictionaryFormScreenState(noFlagLanguage = english)
        val result = reducer.testReduce(initial, DictionaryFormMsg.SelectFlag(mexicoFlag))

        assertEquals(mexican, result.state().learningLanguage)
        result.assertNoEffects()
    }

    @Test
    fun `OpenLanguagePicker opens picker for target`() {
        val result = reducer.testReduce(
            DictionaryFormScreenState(),
            DictionaryFormMsg.OpenLanguagePicker(LanguageTarget.TRANSLATION),
        )

        assertTrue(result.state().languagePicker.isOpen)
        assertEquals(LanguageTarget.TRANSLATION, result.state().languagePicker.target)
        result.assertNoEffects()
    }

    @Test
    fun `CloseLanguagePicker closes picker`() {
        val opened = DictionaryFormScreenState().openLanguagePicker(LanguageTarget.LEARNING)
        val result = reducer.testReduce(opened, DictionaryFormMsg.CloseLanguagePicker)

        assertFalse(result.state().languagePicker.isOpen)
        result.assertNoEffects()
    }

    @Test
    fun `LanguageQueryChanged filters visible languages`() {
        val opened = DictionaryFormScreenState()
            .applyAllLanguages(listOf(english, russian))
            .openLanguagePicker(LanguageTarget.TRANSLATION)
        val result = reducer.testReduce(opened, DictionaryFormMsg.LanguageQueryChanged("rus"))

        assertEquals(listOf(russian), result.state().languagePicker.visibleLanguages)
        result.assertNoEffects()
    }

    @Test
    fun `SelectLanguage applies choice and closes picker`() {
        val opened = DictionaryFormScreenState().openLanguagePicker(LanguageTarget.TRANSLATION)
        val result = reducer.testReduce(opened, DictionaryFormMsg.SelectLanguage(russian))

        assertEquals(russian, result.state().translationLanguage)
        assertFalse(result.state().languagePicker.isOpen)
        result.assertNoEffects()
    }

    // === Back ===

    @Test
    fun `should emit Back effect when Back on default state`() {
        val result = reducer.testReduce(DictionaryFormScreenState(), DictionaryFormMsg.Back)

        result.assertSingleEffect<NavigationEffect.Back>()
    }

    @Test
    fun `should preserve state and emit Back when Back with data`() {
        val initial = DictionaryFormScreenState(
            name = "Eng",
            selectedFlag = spainFlag,
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.Back)

        assertEquals("name should not change", "Eng", result.state().name)
        assertEquals("selectedFlag should not change", spainFlag, result.state().selectedFlag)
        result.assertSingleEffect<NavigationEffect.Back>()
    }

    // === Empty ===

    @Test
    fun `should not change state and produce no effects when Empty`() {
        val initial = DictionaryFormScreenState()
        val result = reducer.testReduce(initial, DictionaryFormMsg.Empty)

        assertEquals("state should not change", initial, result.state())
        result.assertNoEffects()
    }
}
