package me.apomazkin.dictionary.form.reducer

import me.apomazkin.dictionary.form.DictionaryFormEffect
import me.apomazkin.mate.NavigationEffect
import me.apomazkin.dictionary.form.DictionaryFormMsg
import me.apomazkin.dictionary.form.DictionaryFormReducer
import me.apomazkin.dictionary.form.DictionaryFormScreenState
import me.apomazkin.dictionary.model.CountryFlagItem
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
 * 1. Boundary: FlagsUpdated with empty list
 * 2. Standard: FlagsUpdated with list
 * 3. Standard: FlagsUpdated preserves other fields
 * 4. Standard: DictionaryLoaded with name and flag
 * 5. Standard: DictionaryLoaded with null flag
 * 6. Edge: DictionaryLoaded with empty name
 * 7. Standard: DictionaryLoaded preserves other fields
 * 8. Standard: DictionarySaved emits Close
 * 9. Standard: DictionarySaved preserves state
 */
class FormDataLoadingTest {

    private val reducer = DictionaryFormReducer()

    private val spainFlag = CountryFlagItem(
        numericCode = 724, countryName = "Spain", flagRes = 100,
        languages = listOf("Spanish", "Catalan"),
    )
    private val mexicoFlag = CountryFlagItem(
        numericCode = 484, countryName = "Mexico", flagRes = 101,
        languages = listOf("Spanish"),
    )
    private val flags = listOf(spainFlag, mexicoFlag)

    // === FlagsUpdated ===

    @Test
    fun `should set empty flags when FlagsUpdated with empty list`() {
        val result = reducer.testReduce(
            DictionaryFormScreenState(),
            DictionaryFormMsg.FlagsUpdated(emptyList()),
        )

        assertTrue("flags should be empty", result.state().flags.isEmpty())
        result.assertNoEffects()
    }

    @Test
    fun `should set flags when FlagsUpdated with list`() {
        val result = reducer.testReduce(
            DictionaryFormScreenState(),
            DictionaryFormMsg.FlagsUpdated(flags),
        )

        assertEquals("should have 2 flags", 2, result.state().flags.size)
        assertEquals("flags should match", flags, result.state().flags)
        result.assertNoEffects()
    }

    @Test
    fun `should preserve other fields when FlagsUpdated`() {
        val initial = DictionaryFormScreenState(
            name = "Eng",
            selectedFlag = spainFlag,
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.FlagsUpdated(flags))

        assertEquals("name should not change", "Eng", result.state().name)
        assertEquals("selectedFlag should not change", spainFlag, result.state().selectedFlag)
    }

    // === DictionaryLoaded ===

    @Test
    fun `should prefill name and flag when DictionaryLoaded`() {
        val initial = DictionaryFormScreenState(editingDictionaryId = 5)
        val result = reducer.testReduce(
            initial,
            DictionaryFormMsg.DictionaryLoaded("English", spainFlag),
        )

        assertEquals("name should be English", "English", result.state().name)
        assertEquals("selectedFlag should be set", spainFlag, result.state().selectedFlag)
        assertTrue("saveButtonEnabled should be true", result.state().saveButtonEnabled)
        result.assertNoEffects()
    }

    @Test
    fun `should prefill name with null flag when DictionaryLoaded without flag`() {
        val initial = DictionaryFormScreenState(editingDictionaryId = 5)
        val result = reducer.testReduce(
            initial,
            DictionaryFormMsg.DictionaryLoaded("English", null),
        )

        assertEquals("name should be English", "English", result.state().name)
        assertNull("selectedFlag should be null", result.state().selectedFlag)
        assertTrue("saveButtonEnabled should be true", result.state().saveButtonEnabled)
        result.assertNoEffects()
    }

    @Test
    fun `should disable save when DictionaryLoaded with empty name`() {
        val initial = DictionaryFormScreenState(editingDictionaryId = 5)
        val result = reducer.testReduce(
            initial,
            DictionaryFormMsg.DictionaryLoaded("", null),
        )

        assertEquals("name should be empty", "", result.state().name)
        assertFalse("saveButtonEnabled should be false", result.state().saveButtonEnabled)
        result.assertNoEffects()
    }

    @Test
    fun `should preserve other fields when DictionaryLoaded`() {
        val initial = DictionaryFormScreenState(
            editingDictionaryId = 5,
            flagFilter = "spa",
        )
        val result = reducer.testReduce(
            initial,
            DictionaryFormMsg.DictionaryLoaded("English", spainFlag),
        )

        assertEquals("flagFilter should not change", "spa", result.state().flagFilter)
        assertEquals("editingDictionaryId should not change", 5L, result.state().editingDictionaryId)
    }

    // === DictionarySaved ===

    @Test
    fun `should emit Close effect when DictionarySaved`() {
        val result = reducer.testReduce(
            DictionaryFormScreenState(),
            DictionaryFormMsg.DictionarySaved,
        )

        result.assertSingleEffect<NavigationEffect.Back>()
    }

    @Test
    fun `should preserve state when DictionarySaved`() {
        val initial = DictionaryFormScreenState(
            name = "Eng",
            selectedFlag = spainFlag,
        )
        val result = reducer.testReduce(initial, DictionaryFormMsg.DictionarySaved)

        assertEquals("name should not change", "Eng", result.state().name)
        assertEquals("selectedFlag should not change", spainFlag, result.state().selectedFlag)
        result.assertSingleEffect<NavigationEffect.Back>()
    }
}
