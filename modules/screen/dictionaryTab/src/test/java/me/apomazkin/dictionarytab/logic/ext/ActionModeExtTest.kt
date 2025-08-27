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
import java.util.Date

/**
 * Tests for extensions related to action mode management
 * 
 * Test cases:
 * 1. Boundary case: showActionMode enables action mode
 * 2. Boundary case: hideActionMode disables action mode  
 * 3. Standard case: checkActionMode enables when terms selected
 * 4. Boundary case: checkActionMode disables when no terms selected
 * 5. Standard case: modifySelectedSet adds unselected term
 * 6. Standard case: modifySelectedSet removes selected term
 * 7. Standard case: clearSelectedSet removes all terms
 * 8. Standard case: highlightWord updates term list flow
 * 9. Standard case: clearHighlighted updates term list flow
 */
class ActionModeExtTest {

    @Test
    fun `should enable action mode when showActionMode is called`() {
        // Test case 1: Boundary case - showActionMode enables action mode
        // Given
        val initialState = DictionaryTabState(
            topBarState = TopBarState(isActionMode = false)
        )

        // When
        val resultState = initialState.showActionMode()

        // Then
        assertTrue("Action mode should be enabled", resultState.topBarState.isActionMode)
        assertEquals(
            "Other state properties should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
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
    fun `should disable action mode when hideActionMode is called`() {
        // Test case 2: Boundary case - hideActionMode disables action mode
        // Given
        val initialState = DictionaryTabState(
            topBarState = TopBarState(isActionMode = true)
        )

        // When
        val resultState = initialState.hideActionMode()

        // Then
        assertFalse("Action mode should be disabled", resultState.topBarState.isActionMode)
        assertEquals(
            "Other state properties should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
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
    fun `should enable action mode when terms are selected in checkActionMode`() {
        // Test case 3: Standard case - checkActionMode enables when terms selected
        // Given
        val initialState = DictionaryTabState(
            topBarState = TopBarState(
                isActionMode = false,
                actionState = TopBarState.Action(selectedTermIds = setOf(WordInfo(1L, "test")))
            )
        )

        // When
        val resultState = initialState.checkActionMode()

        // Then
        assertTrue(
            "Action mode should be enabled when terms are selected",
            resultState.topBarState.isActionMode
        )
        assertEquals(
            "Selected terms should remain unchanged",
            initialState.topBarState.actionState.selectedTermIds,
            resultState.topBarState.actionState.selectedTermIds
        )
        assertEquals(
            "Other state properties should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
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
    fun `should disable action mode when no terms are selected in checkActionMode`() {
        // Test case 4: Boundary case - checkActionMode disables when no terms selected
        // Given
        val initialState = DictionaryTabState(
            topBarState = TopBarState(
                isActionMode = true,
                actionState = TopBarState.Action(selectedTermIds = emptySet())
            )
        )

        // When
        val resultState = initialState.checkActionMode()

        // Then
        assertFalse(
            "Action mode should be disabled when no terms are selected",
            resultState.topBarState.isActionMode
        )
        assertEquals(
            "Selected terms should remain unchanged",
            initialState.topBarState.actionState.selectedTermIds,
            resultState.topBarState.actionState.selectedTermIds
        )
        assertEquals(
            "Other state properties should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
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
    fun `should add term to selected set when modifySelectedSet is called with unselected term`() {
        // Test case 5: Standard case - modifySelectedSet adds unselected term
        // Given
        val existingTerm = WordInfo(1L, "existing")
        val newTerm = WordInfo(2L, "new")
        val initialState = DictionaryTabState(
            topBarState = TopBarState(
                actionState = TopBarState.Action(selectedTermIds = setOf(existingTerm))
            )
        )

        // When
        val resultState = initialState.modifySelectedSet(newTerm)

        // Then
        val expectedSelectedTerms = setOf(existingTerm, newTerm)
        assertEquals(
            "Term should be added to selected set",
            expectedSelectedTerms,
            resultState.topBarState.actionState.selectedTermIds
        )
        assertEquals(
            "Action mode should remain unchanged",
            initialState.topBarState.isActionMode,
            resultState.topBarState.isActionMode
        )
        assertEquals(
            "Other state properties should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
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
    fun `should remove term from selected set when modifySelectedSet is called with selected term`() {
        // Test case 6: Standard case - modifySelectedSet removes selected term
        // Given
        val termToRemove = WordInfo(1L, "toRemove")
        val remainingTerm = WordInfo(2L, "remaining")
        val initialState = DictionaryTabState(
            topBarState = TopBarState(
                actionState = TopBarState.Action(selectedTermIds = setOf(termToRemove, remainingTerm))
            )
        )

        // When
        val resultState = initialState.modifySelectedSet(termToRemove)

        // Then
        val expectedSelectedTerms = setOf(remainingTerm)
        assertEquals(
            "Term should be removed from selected set",
            expectedSelectedTerms,
            resultState.topBarState.actionState.selectedTermIds
        )
        assertEquals(
            "Action mode should remain unchanged",
            initialState.topBarState.isActionMode,
            resultState.topBarState.isActionMode
        )
        assertEquals(
            "Other state properties should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
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
    fun `should clear all selected terms when clearSelectedSet is called`() {
        // Test case 7: Standard case - clearSelectedSet removes all terms
        // Given
        val initialState = DictionaryTabState(
            topBarState = TopBarState(
                actionState = TopBarState.Action(
                    selectedTermIds = setOf(WordInfo(1L, "term1"), WordInfo(2L, "term2"))
                )
            )
        )

        // When
        val resultState = initialState.clearSelectedSet()

        // Then
        assertTrue(
            "All selected terms should be cleared",
            resultState.topBarState.actionState.selectedTermIds.isEmpty()
        )
        assertEquals(
            "Action mode should remain unchanged",
            initialState.topBarState.isActionMode,
            resultState.topBarState.isActionMode
        )
        assertEquals(
            "Other state properties should remain unchanged",
            initialState.isLoading,
            resultState.isLoading
        )
        assertEquals(
            "Term list should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "Term list map should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
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
    fun `should update term list flow when highlightWord is called`() {
        // Test case 8: Standard case - highlightWord updates term list flow
        // Given
        val termFlow = flowOf(
            PagingData.from(listOf(TermUiItem(1L, "test", 1L, Date())))
        )
        val initialState = DictionaryTabState(
            topBarState = TopBarState(
                actionState = TopBarState.Action(
                    selectedTermIds = setOf(WordInfo(1L, "test"))
                )
            ),
            termList = TermsSource(
                pattern = "test",
                termListFlow = termFlow
            )
        )

        // When
        val resultState = initialState.highlightWord(WordInfo(1L, "test"))

        // Then
        assertEquals(
            "Pattern should remain unchanged",
            initialState.termList.pattern,
            resultState.termList.pattern
        )
        assertEquals(
            "Action mode should remain unchanged",
            initialState.topBarState.isActionMode,
            resultState.topBarState.isActionMode
        )
    }

    @Test
    fun `should update term list flow when clearHighlighted is called`() {
        // Test case 9: Standard case - clearHighlighted updates term list flow
        // Given
        val termFlow = flowOf(
            PagingData.from(listOf(TermUiItem(1L, "test", 1L, Date())))
        )
        val initialState = DictionaryTabState(
            termList = TermsSource(
                pattern = "test",
                termListFlow = termFlow
            )
        )

        // When
        val resultState = initialState.clearHighlighted()

        // Then
        assertEquals(
            "Pattern should remain unchanged",
            initialState.termList.pattern,
            resultState.termList.pattern
        )
        assertEquals(
            "Action mode should remain unchanged",
            initialState.topBarState.isActionMode,
            resultState.topBarState.isActionMode
        )
    }
}
