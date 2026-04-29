package me.apomazkin.dictionary.list.reducer

import me.apomazkin.dictionary.list.DeleteDialogState
import me.apomazkin.dictionary.list.DictionaryListEffect
import me.apomazkin.dictionary.list.DictionaryListMsg
import me.apomazkin.dictionary.list.DictionaryListReducer
import me.apomazkin.dictionary.list.DictionaryListScreenState
import me.apomazkin.dictionary.model.DictionaryListItem
import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.assertSingleEffect
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases:
 * 1. Standard: RequestDelete → show dialog with id and name, no effects
 * 2. Standard: RequestDelete preserves dictionaries
 * 3. Standard: ConfirmDelete → hide dialog, DeleteDictionary effect
 * 4. Standard: DismissDelete → hide dialog, no effects
 * 5. Standard: DictionaryDeleted → no effects (Flow updates list automatically)
 */
class DeleteFlowTest {

    private val reducer = DictionaryListReducer()
    private val item1 = DictionaryListItem(id = 1, name = "English", flagRes = 123)
    private val item2 = DictionaryListItem(id = 2, name = "Bio", flagRes = null)

    @Test
    fun `should show dialog with data when RequestDelete`() {
        // Test case 1
        val result = reducer.testReduce(
            DictionaryListScreenState(),
            DictionaryListMsg.RequestDelete(id = 3, name = "English"),
        )

        assertTrue("dialog should show", result.state().deleteDialogState.show)
        assertEquals("dictionaryId should be 3", 3L, result.state().deleteDialogState.dictionaryId)
        assertEquals("name should be English", "English", result.state().deleteDialogState.dictionaryName)
        result.assertNoEffects()
    }

    @Test
    fun `should preserve dictionaries when RequestDelete`() {
        // Test case 2
        val initial = DictionaryListScreenState(
            dictionaries = listOf(item1, item2),
        )
        val result = reducer.testReduce(initial, DictionaryListMsg.RequestDelete(id = 1, name = "English"))

        assertEquals("dictionaries should not change", initial.dictionaries, result.state().dictionaries)
    }

    @Test
    fun `should hide dialog and trigger delete when ConfirmDelete`() {
        // Test case 3
        val initial = DictionaryListScreenState(
            deleteDialogState = DeleteDialogState(show = true, dictionaryId = 3, dictionaryName = "English"),
        )
        val result = reducer.testReduce(initial, DictionaryListMsg.ConfirmDelete)

        assertFalse("dialog should be hidden", result.state().deleteDialogState.show)
        result.assertSingleEffect<DictionaryListEffect.DeleteDictionary>()
        val effect = result.effects().first() as DictionaryListEffect.DeleteDictionary
        assertEquals("should delete id=3", 3L, effect.id)
    }

    @Test
    fun `should hide dialog when DismissDelete`() {
        // Test case 4
        val initial = DictionaryListScreenState(
            deleteDialogState = DeleteDialogState(show = true, dictionaryId = 3, dictionaryName = "English"),
        )
        val result = reducer.testReduce(initial, DictionaryListMsg.DismissDelete)

        assertFalse("dialog should be hidden", result.state().deleteDialogState.show)
        result.assertNoEffects()
    }

    @Test
    fun `should have no effects when DictionaryDeleted`() {
        // Test case 5: FlowHandler updates list automatically via Room Flow
        val result = reducer.testReduce(DictionaryListScreenState(), DictionaryListMsg.DictionaryDeleted)

        result.assertNoEffects()
    }

}
