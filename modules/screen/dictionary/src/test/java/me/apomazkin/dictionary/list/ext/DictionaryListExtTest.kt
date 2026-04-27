package me.apomazkin.dictionary.list.ext

import me.apomazkin.dictionary.list.DictionaryListScreenState
import me.apomazkin.dictionary.list.updateDictionaries
import me.apomazkin.dictionary.model.DictionaryListItem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test cases:
 * 1. Boundary: updateDictionaries sets list on empty state
 * 2. Standard: updateDictionaries replaces existing list
 * 3. Standard: updateDictionaries preserves other fields
 */
class DictionaryListExtTest {

    private val item1 = DictionaryListItem(id = 1, name = "English", flagRes = 123)
    private val item2 = DictionaryListItem(id = 2, name = "Bio", flagRes = null)

    @Test
    fun `should set dictionaries when updateDictionaries on empty`() {
        val initial = DictionaryListScreenState()
        val result = initial.updateDictionaries(listOf(item1, item2))

        assertEquals("should have 2 dictionaries", 2, result.dictionaries.size)
        assertEquals("first should be English", item1, result.dictionaries[0])
    }

    @Test
    fun `should replace dictionaries when updateDictionaries`() {
        val initial = DictionaryListScreenState().updateDictionaries(listOf(item1))
        val result = initial.updateDictionaries(listOf(item2))

        assertEquals("should have 1 dictionary", 1, result.dictionaries.size)
        assertEquals("should be Bio", item2, result.dictionaries[0])
    }

    @Test
    fun `should preserve other fields when updateDictionaries`() {
        val initial = DictionaryListScreenState(isLoading = false)
        val result = initial.updateDictionaries(listOf(item1))

        assertEquals("isLoading should not change", initial.isLoading, result.isLoading)
        assertEquals("deleteDialogState should not change", initial.deleteDialogState, result.deleteDialogState)
    }
}
