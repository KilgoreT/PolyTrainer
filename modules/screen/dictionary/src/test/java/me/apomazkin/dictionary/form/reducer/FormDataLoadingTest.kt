package me.apomazkin.dictionary.form.reducer

import me.apomazkin.dictionary.form.DictionaryFormMsg
import me.apomazkin.dictionary.form.DictionaryFormReducer
import me.apomazkin.dictionary.form.DictionaryFormScreenState
import me.apomazkin.dictionary.model.CountryFlagItem
import me.apomazkin.dictionary.model.LanguageItem
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Standard: LanguagesLoaded → sets languages and filteredLanguages
 * 2. Standard: FlagsLoaded with multiple → sets flags, no auto-select
 * 3. Standard: FlagsLoaded with single → auto-selects flag
 * 4. Standard: DictionarySaved → needClose=true
 */
class FormDataLoadingTest {

    private val reducer = DictionaryFormReducer()

    private val languages = listOf(
        LanguageItem("en", "English"),
        LanguageItem("es", "Spanish"),
    )

    private val multipleFlags = listOf(
        CountryFlagItem(724, "Spain", 100),
        CountryFlagItem(484, "Mexico", 101),
        CountryFlagItem(32, "Argentina", 102),
    )

    private val singleFlag = listOf(
        CountryFlagItem(392, "Japan", 200),
    )

    @Test
    fun `should set languages when LanguagesLoaded`() {
        // Test case 1
        val result = reducer.testReduce(DictionaryFormScreenState(), DictionaryFormMsg.LanguagesLoaded(languages))

        assertEquals("languages should be set", languages, result.state().languagePickerState.languages)
        assertEquals("filteredLanguages should match", languages, result.state().languagePickerState.filteredLanguages)
        result.assertNoEffects()
    }

    @Test
    fun `should set flags without auto-select when FlagsLoaded multiple`() {
        // Test case 2
        val result = reducer.testReduce(DictionaryFormScreenState(), DictionaryFormMsg.FlagsLoaded(multipleFlags))

        assertEquals("should have 3 flags", 3, result.state().availableFlags.size)
        assertNull("selectedFlag should be null", result.state().selectedFlag)
        result.assertNoEffects()
    }

    @Test
    fun `should auto-select flag when FlagsLoaded single`() {
        // Test case 3
        val result = reducer.testReduce(DictionaryFormScreenState(), DictionaryFormMsg.FlagsLoaded(singleFlag))

        assertEquals("should have 1 flag", 1, result.state().availableFlags.size)
        assertEquals("should auto-select Japan", singleFlag[0], result.state().selectedFlag)
    }

    @Test
    fun `should set needClose when DictionarySaved`() {
        // Test case 4
        val result = reducer.testReduce(DictionaryFormScreenState(), DictionaryFormMsg.DictionarySaved)

        assertTrue("needClose should be true", result.state().needClose)
    }
}
