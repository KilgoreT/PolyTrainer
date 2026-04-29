package me.apomazkin.dictionary.list.reducer

import me.apomazkin.dictionary.list.DictionaryListMsg
import me.apomazkin.dictionary.list.DictionaryListReducer
import me.apomazkin.dictionary.list.DictionaryListScreenState
import me.apomazkin.dictionary.model.DictionaryListItem
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Standard: DictionariesLoaded with list → updates, stops loading
 * 2. Boundary: DictionariesLoaded with empty list
 * 3. Standard: DictionariesLoaded preserves deleteDialogState
 */
class ListDataLoadingTest {

    private val reducer = DictionaryListReducer()

    private val dictList = listOf(
        DictionaryListItem(id = 1, name = "English", flagRes = 123),
        DictionaryListItem(id = 2, name = "Bio", flagRes = null),
    )

    @Test
    fun `should update dictionaries and stop loading when DictionariesLoaded`() {
        // Test case 1
        val initial = DictionaryListScreenState(isLoading = true)
        val result = reducer.testReduce(initial, DictionaryListMsg.DictionariesLoaded(dictList))

        assertFalse("isLoading should be false", result.state().isLoading)
        assertEquals("should have 2 dictionaries", 2, result.state().dictionaries.size)
        assertEquals("first should be English", dictList[0], result.state().dictionaries[0])
        result.assertNoEffects()
    }

    @Test
    fun `should handle empty list when DictionariesLoaded`() {
        // Test case 2
        val result = reducer.testReduce(
            DictionaryListScreenState(isLoading = true),
            DictionaryListMsg.DictionariesLoaded(emptyList()),
        )

        assertFalse("isLoading should be false", result.state().isLoading)
        assertEquals("dictionaries should be empty", 0, result.state().dictionaries.size)
    }

    @Test
    fun `should preserve deleteDialogState when DictionariesLoaded`() {
        // Test case 3
        val initial = DictionaryListScreenState(isLoading = true)
        val result = reducer.testReduce(initial, DictionaryListMsg.DictionariesLoaded(dictList))

        assertEquals("deleteDialogState should not change", initial.deleteDialogState, result.state().deleteDialogState)
    }

}
