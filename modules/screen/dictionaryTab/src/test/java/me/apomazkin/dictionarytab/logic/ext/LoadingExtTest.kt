package me.apomazkin.dictionarytab.logic.ext

import androidx.paging.PagingData
import kotlinx.coroutines.flow.flowOf
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.dictionarytab.logic.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for extensions related to loading state management
 * 
 * Test cases:
 * 1. Boundary case: showLoading enables loading state when already loading
 * 2. Standard case: showLoading enables loading state when not loading
 * 3. Standard case: showLoading enables loading state with partial data
 * 4. Boundary case: hideLoading disables loading state when already not loading
 * 5. Standard case: hideLoading disables loading state when loading
 * 6. Standard case: hideLoading disables loading state with partial data
 */
class LoadingExtTest {

    /**
     * Test for showLoading()
     *
     * Edge cases:
     * - State already in loading mode (isLoading = true)
     * - State with partially filled data
     * - State with loading errors
     */
    @Test
    fun `should set loading state to true when showLoading is called`() {
        // Test 1: State already in loading mode
        val stateAlreadyLoading = DictionaryTabState(isLoading = true)
        val result1 = stateAlreadyLoading.showLoading()

        assertTrue("isLoading should be true", result1.isLoading)
        assertEquals(
            "topBarState should not mutate on showLoading() - violates immutability",
            stateAlreadyLoading.topBarState,
            result1.topBarState
        )
        assertEquals(
            "termList should not mutate on showLoading() - violates immutability",
            stateAlreadyLoading.termList,
            result1.termList
        )
        assertEquals(
            "termListMap should not mutate on showLoading() - violates immutability",
            stateAlreadyLoading.termListMap,
            result1.termListMap
        )
        assertEquals(
            "addWordDialogState should not mutate on showLoading() - violates immutability",
            stateAlreadyLoading.addWordDialogState,
            result1.addWordDialogState
        )
        assertEquals(
            "snackbarState should not mutate on showLoading() - violates immutability",
            stateAlreadyLoading.snackbarState,
            result1.snackbarState
        )
        assertEquals(
            "confirmWordDeleteDialogState should not mutate on showLoading() - violates immutability",
            stateAlreadyLoading.confirmWordDeleteDialogState,
            result1.confirmWordDeleteDialogState
        )

        // Test 2: State with partially filled data
        val stateWithPartialData = DictionaryTabState(
            isLoading = false,
            topBarState = TopBarState(isActionMode = true),
            termList = TermsSource(pattern = "test"),
            termListMap = mapOf("test" to flowOf(PagingData.empty<TermUiItem>())),
            addWordDialogState = AddWordDialogState(isOpen = true, wordValue = "test"),
            snackbarState = SnackbarState(title = "Error", show = true),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                isOpen = true,
                wordIds = setOf(WordInfo(1L, "test"))
            )
        )
        val result2 = stateWithPartialData.showLoading()

        assertTrue("isLoading should be true", result2.isLoading)
        assertEquals(
            "topBarState should not mutate on showLoading() - violates immutability",
            stateWithPartialData.topBarState,
            result2.topBarState
        )
        assertEquals(
            "termList should not mutate on showLoading() - violates immutability",
            stateWithPartialData.termList,
            result2.termList
        )
        assertEquals(
            "termListMap should not mutate on showLoading() - violates immutability",
            stateWithPartialData.termListMap,
            result2.termListMap
        )
        assertEquals(
            "addWordDialogState should not mutate on showLoading() - violates immutability",
            stateWithPartialData.addWordDialogState,
            result2.addWordDialogState
        )
        assertEquals(
            "snackbarState should not mutate on showLoading() - violates immutability",
            stateWithPartialData.snackbarState,
            result2.snackbarState
        )
        assertEquals(
            "confirmWordDeleteDialogState should not mutate on showLoading() - violates immutability",
            stateWithPartialData.confirmWordDeleteDialogState,
            result2.confirmWordDeleteDialogState
        )

        // Test 3: State with loading errors (empty data)
        val stateWithErrors = DictionaryTabState(
            isLoading = false,
            topBarState = TopBarState(),
            termList = TermsSource(pattern = ""),
            termListMap = emptyMap(),
            addWordDialogState = AddWordDialogState(),
            snackbarState = SnackbarState(title = "Error", show = true),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState()
        )
        val result3 = stateWithErrors.showLoading()

        assertTrue("isLoading should be true", result3.isLoading)
        assertEquals(
            "topBarState should not mutate on showLoading() - violates immutability",
            stateWithErrors.topBarState,
            result3.topBarState
        )
        assertEquals(
            "termList should not mutate on showLoading() - violates immutability",
            stateWithErrors.termList,
            result3.termList
        )
        assertEquals(
            "termListMap should not mutate on showLoading() - violates immutability",
            stateWithErrors.termListMap,
            result3.termListMap
        )
        assertEquals(
            "addWordDialogState should not mutate on showLoading() - violates immutability",
            stateWithErrors.addWordDialogState,
            result3.addWordDialogState
        )
        assertEquals(
            "snackbarState should not mutate on showLoading() - violates immutability",
            stateWithErrors.snackbarState,
            result3.snackbarState
        )
        assertEquals(
            "confirmWordDeleteDialogState should not mutate on showLoading() - violates immutability",
            stateWithErrors.confirmWordDeleteDialogState,
            result3.confirmWordDeleteDialogState
        )
    }

    /**
     * Test for hideLoading()
     *
     * Edge cases:
     * - State already not in loading mode (isLoading = false)
     * - State with empty data after loading
     * - State with partially loaded data
     */
    @Test
    fun `should set loading state to false when hideLoading is called`() {
        // Test 1: State already not in loading mode
        val stateAlreadyNotLoading = DictionaryTabState(isLoading = false)
        val result1 = stateAlreadyNotLoading.hideLoading()

        assertFalse("isLoading should be false", result1.isLoading)
        assertEquals(
            "topBarState should not mutate on hideLoading() - violates immutability",
            stateAlreadyNotLoading.topBarState,
            result1.topBarState
        )
        assertEquals(
            "termList should not mutate on hideLoading() - violates immutability",
            stateAlreadyNotLoading.termList,
            result1.termList
        )
        assertEquals(
            "termListMap should not mutate on hideLoading() - violates immutability",
            stateAlreadyNotLoading.termListMap,
            result1.termListMap
        )
        assertEquals(
            "addWordDialogState should not mutate on hideLoading() - violates immutability",
            stateAlreadyNotLoading.addWordDialogState,
            result1.addWordDialogState
        )
        assertEquals(
            "snackbarState should not mutate on hideLoading() - violates immutability",
            stateAlreadyNotLoading.snackbarState,
            result1.snackbarState
        )
        assertEquals(
            "confirmWordDeleteDialogState should not mutate on hideLoading() - violates immutability",
            stateAlreadyNotLoading.confirmWordDeleteDialogState,
            result1.confirmWordDeleteDialogState
        )

        // Test 2: State with empty data after loading
        val stateWithEmptyData = DictionaryTabState(
            isLoading = true,
            topBarState = TopBarState(),
            termList = TermsSource(pattern = ""),
            termListMap = emptyMap(),
            addWordDialogState = AddWordDialogState(),
            snackbarState = SnackbarState(),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState()
        )
        val result2 = stateWithEmptyData.hideLoading()

        assertFalse("isLoading should be false", result2.isLoading)
        assertEquals(
            "topBarState should not mutate on hideLoading() - violates immutability",
            stateWithEmptyData.topBarState,
            result2.topBarState
        )
        assertEquals(
            "termList should not mutate on hideLoading() - violates immutability",
            stateWithEmptyData.termList,
            result2.termList
        )
        assertEquals(
            "termListMap should not mutate on hideLoading() - violates immutability",
            stateWithEmptyData.termListMap,
            result2.termListMap
        )
        assertEquals(
            "addWordDialogState should not mutate on hideLoading() - violates immutability",
            stateWithEmptyData.addWordDialogState,
            result2.addWordDialogState
        )
        assertEquals(
            "snackbarState should not mutate on hideLoading() - violates immutability",
            stateWithEmptyData.snackbarState,
            result2.snackbarState
        )
        assertEquals(
            "confirmWordDeleteDialogState should not mutate on hideLoading() - violates immutability",
            stateWithEmptyData.confirmWordDeleteDialogState,
            result2.confirmWordDeleteDialogState
        )

        // Test 3: State with partially loaded data
        val stateWithPartialData = DictionaryTabState(
            isLoading = true,
            topBarState = TopBarState(isActionMode = true),
            termList = TermsSource(pattern = "partial"),
            termListMap = mapOf("partial" to flowOf(PagingData.empty<TermUiItem>())),
            addWordDialogState = AddWordDialogState(isOpen = true, wordValue = "partial"),
            snackbarState = SnackbarState(title = "Partial", show = true),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                isOpen = true,
                wordIds = setOf(WordInfo(2L, "partial"))
            )
        )
        val result3 = stateWithPartialData.hideLoading()

        assertFalse("isLoading should be false", result3.isLoading)
        assertEquals(
            "topBarState should not mutate on hideLoading() - violates immutability",
            stateWithPartialData.topBarState,
            result3.topBarState
        )
        assertEquals(
            "termList should not mutate on hideLoading() - violates immutability",
            stateWithPartialData.termList,
            result3.termList
        )
        assertEquals(
            "termListMap should not mutate on hideLoading() - violates immutability",
            stateWithPartialData.termListMap,
            result3.termListMap
        )
        assertEquals(
            "addWordDialogState should not mutate on hideLoading() - violates immutability",
            stateWithPartialData.addWordDialogState,
            result3.addWordDialogState
        )
        assertEquals(
            "snackbarState should not mutate on hideLoading() - violates immutability",
            stateWithPartialData.snackbarState,
            result3.snackbarState
        )
        assertEquals(
            "confirmWordDeleteDialogState should not mutate on hideLoading() - violates immutability",
            stateWithPartialData.confirmWordDeleteDialogState,
            result3.confirmWordDeleteDialogState
        )
    }
}
