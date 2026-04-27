package me.apomazkin.dictionary.list.ext

import me.apomazkin.dictionary.list.DeleteDialogState
import me.apomazkin.dictionary.list.DictionaryListScreenState
import me.apomazkin.dictionary.list.hideDeleteDialog
import me.apomazkin.dictionary.list.showDeleteDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Boundary: showDeleteDialog sets show to true with id and name
 * 2. Standard: showDeleteDialog preserves other fields
 * 3. Boundary: hideDeleteDialog resets to default
 * 4. Standard: hideDeleteDialog preserves other fields
 */
class DeleteDialogExtTest {

    @Test
    fun `should show dialog with data when showDeleteDialog`() {
        // Test case 1
        val result = DictionaryListScreenState().showDeleteDialog(id = 3, name = "English")

        assertTrue("show should be true", result.deleteDialogState.show)
        assertEquals("dictionaryId should be 3", 3L, result.deleteDialogState.dictionaryId)
        assertEquals("dictionaryName should be English", "English", result.deleteDialogState.dictionaryName)
    }

    @Test
    fun `should preserve other fields when showDeleteDialog`() {
        // Test case 2
        val initial = DictionaryListScreenState(isLoading = false)
        val result = initial.showDeleteDialog(id = 3, name = "English")

        assertEquals("isLoading should not change", initial.isLoading, result.isLoading)
        assertEquals("dictionaries should not change", initial.dictionaries, result.dictionaries)
    }

    @Test
    fun `should reset dialog when hideDeleteDialog`() {
        // Test case 3
        val initial = DictionaryListScreenState(
            deleteDialogState = DeleteDialogState(show = true, dictionaryId = 3, dictionaryName = "English")
        )
        val result = initial.hideDeleteDialog()

        assertFalse("show should be false", result.deleteDialogState.show)
        assertEquals("dictionaryId should be 0", 0L, result.deleteDialogState.dictionaryId)
        assertEquals("dictionaryName should be empty", "", result.deleteDialogState.dictionaryName)
    }

    @Test
    fun `should preserve other fields when hideDeleteDialog`() {
        // Test case 4
        val initial = DictionaryListScreenState(
            isLoading = false,
            deleteDialogState = DeleteDialogState(show = true, dictionaryId = 3, dictionaryName = "English")
        )
        val result = initial.hideDeleteDialog()

        assertEquals("isLoading should not change", initial.isLoading, result.isLoading)
        assertEquals("dictionaries should not change", initial.dictionaries, result.dictionaries)
    }
}
