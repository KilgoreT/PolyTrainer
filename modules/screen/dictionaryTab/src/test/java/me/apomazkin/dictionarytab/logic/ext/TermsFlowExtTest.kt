package me.apomazkin.dictionarytab.logic.ext

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.logic.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Tests for extensions related to terms flow management
 * 
 * Test cases:
 * 1. Boundary case: appendTermsFlow adds new pattern and flow to empty map
 * 2. Standard case: appendTermsFlow adds new pattern and flow to existing map
 * 3. Boundary case: toDefaultTermsFlow switches to default flow when empty map
 * 4. Standard case: toDefaultTermsFlow switches to default flow with existing patterns
 * 5. Standard case: retainDefaultAndCurrentFlow keeps only default and current patterns
 */
class TermsFlowExtTest {

    @Test
    fun `should add new pattern and flow to empty map when appendTermsFlow is called`() {
        // Test case 1: Boundary case - appendTermsFlow adds new pattern and flow to empty map
        // Given
        val initialState = DictionaryTabState(
            isLoading = false,
            termList = TermsSource(pattern = ""),
            termListMap = emptyMap()
        )
        val newPattern = "test"
        val newFlow: Flow<PagingData<TermUiItem>> = flowOf(PagingData.empty())

        // When
        val resultState = initialState.appendTermsFlow(newPattern, newFlow)

        // Then
        // Main functionality check
        assertEquals(
            "Pattern should be updated to new pattern",
            newPattern,
            resultState.termList.pattern
        )
        assertEquals(
            "Term list flow should be updated to new flow",
            newFlow,
            resultState.termList.termListFlow
        )
        assertEquals(
            "Term list map should contain new pattern",
            1,
            resultState.termListMap.size
        )
        assertEquals(
            "Term list map should contain new flow",
            newFlow,
            resultState.termListMap[newPattern]
        )

        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "Add word dialog state should remain unchanged",
            initialState.addWordDialogState,
            resultState.addWordDialogState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "Confirm word delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }

    @Test
    fun `should add new pattern and flow to existing map when appendTermsFlow is called`() {
        // Test case 2: Standard case - appendTermsFlow adds new pattern and flow to existing map
        // Given
        val existingPattern = "existing"
        val existingFlow: Flow<PagingData<TermUiItem>> = flowOf(PagingData.empty())
        val initialState = DictionaryTabState(
            isLoading = true,
            termList = TermsSource(pattern = existingPattern),
            termListMap = mapOf(existingPattern to existingFlow)
        )
        val newPattern = "new"
        val newFlow: Flow<PagingData<TermUiItem>> = flowOf(PagingData.empty())

        // When
        val resultState = initialState.appendTermsFlow(newPattern, newFlow)

        // Then
        // Main functionality check
        assertEquals(
            "Pattern should be updated to new pattern",
            newPattern,
            resultState.termList.pattern
        )
        assertEquals(
            "Term list flow should be updated to new flow",
            newFlow,
            resultState.termList.termListFlow
        )
        assertEquals(
            "Term list map should contain both patterns",
            2,
            resultState.termListMap.size
        )
        assertEquals(
            "Existing flow should remain in map",
            existingFlow,
            resultState.termListMap[existingPattern]
        )
        assertEquals(
            "New flow should be added to map",
            newFlow,
            resultState.termListMap[newPattern]
        )

        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "Add word dialog state should remain unchanged",
            initialState.addWordDialogState,
            resultState.addWordDialogState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "Confirm word delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }

    @Test
    fun `should throw exception when toDefaultTermsFlow is called with empty map`() {
        // Test case 3: Boundary case - toDefaultTermsFlow switches to default flow when empty map
        // Given
        val initialState = DictionaryTabState(
            isLoading = false,
            termList = TermsSource(pattern = "test"),
            termListMap = emptyMap()
        )

        // When & Then
        assertThrows(
            "Should throw IllegalStateException when default flow not found",
            IllegalStateException::class.java
        ) {
            initialState.toDefaultTermsFlow()
        }
    }

    @Test
    fun `should switch to default flow when toDefaultTermsFlow is called with existing patterns`() {
        // Test case 4: Standard case - toDefaultTermsFlow switches to default flow with existing patterns
        // Given
        val defaultFlow: Flow<PagingData<TermUiItem>> = flowOf(PagingData.empty())
        val otherFlow: Flow<PagingData<TermUiItem>> = flowOf(PagingData.empty())
        val initialState = DictionaryTabState(
            isLoading = true,
            termList = TermsSource(pattern = "other"),
            termListMap = mapOf(
                "" to defaultFlow,
                "other" to otherFlow
            )
        )

        // When
        val resultState = initialState.toDefaultTermsFlow()

        // Then
        // Main functionality check
        assertEquals(
            "Pattern should be switched to default",
            "",
            resultState.termList.pattern
        )
        assertEquals(
            "Term list flow should be switched to default flow",
            defaultFlow,
            resultState.termList.termListFlow
        )
        assertEquals(
            "Term list map should contain only default pattern",
            1,
            resultState.termListMap.size
        )
        assertEquals(
            "Default flow should remain in map",
            defaultFlow,
            resultState.termListMap[""]
        )

        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "Add word dialog state should remain unchanged",
            initialState.addWordDialogState,
            resultState.addWordDialogState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "Confirm word delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }

    @Test
    fun `should keep only default and current patterns when retainDefaultAndCurrentFlow is called`() {
        // Test case 5: Standard case - retainDefaultAndCurrentFlow keeps only default and current patterns
        // Given
        val defaultFlow: Flow<PagingData<TermUiItem>> = flowOf(PagingData.empty())
        val currentFlow: Flow<PagingData<TermUiItem>> = flowOf(PagingData.empty())
        val otherFlow: Flow<PagingData<TermUiItem>> = flowOf(PagingData.empty())
        val currentPattern = "current"
        val initialState = DictionaryTabState(
            isLoading = false,
            termList = TermsSource(pattern = currentPattern),
            termListMap = mapOf(
                "" to defaultFlow,
                currentPattern to currentFlow,
                "other" to otherFlow
            )
        )

        // When
        val resultState = initialState.retainDefaultAndCurrentFlow(currentPattern)

        // Then
        // Main functionality check
        assertEquals(
            "Term list map should contain only default and current patterns",
            2,
            resultState.termListMap.size
        )
        assertEquals(
            "Default flow should remain in map",
            defaultFlow,
            resultState.termListMap[""]
        )
        assertEquals(
            "Current flow should remain in map",
            currentFlow,
            resultState.termListMap[currentPattern]
        )

        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "Loading state should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Top bar state should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Add word dialog state should remain unchanged",
            initialState.addWordDialogState,
            resultState.addWordDialogState
        )
        assertEquals(
            "Snackbar state should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "Confirm word delete dialog state should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }
}
