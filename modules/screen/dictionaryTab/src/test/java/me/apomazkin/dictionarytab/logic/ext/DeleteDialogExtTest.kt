package me.apomazkin.dictionarytab.logic.ext

import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.dictionarytab.logic.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for extensions related to confirm delete dialog management
 * 
 * Test cases:
 * 1. Boundary case: showConfirmDeleteDialog opens dialog with empty wordIds
 * 2. Standard case: showConfirmDeleteDialog opens dialog with single wordId
 * 3. Standard case: showConfirmDeleteDialog opens dialog with multiple wordIds
 * 4. Boundary case: hideConfirmDeleteDialog closes dialog when already closed
 * 5. Standard case: hideConfirmDeleteDialog closes dialog and resets wordIds
 */
class DeleteDialogExtTest {

    @Test
    fun `should open dialog with empty wordIds when showConfirmDeleteDialog is called with empty set`() {
        // Test case 1: Boundary case - showConfirmDeleteDialog opens dialog with empty wordIds
        // Given
        val initialState = DictionaryTabState(
            isLoading = false,
            topBarState = TopBarState(isActionMode = true),
            termList = TermsSource(pattern = "test"),
            termListMap = mapOf("test" to kotlinx.coroutines.flow.flowOf()),
            addWordDialogState = AddWordDialogState(isOpen = false),
            snackbarState = SnackbarState(title = "test"),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                isOpen = false,
                wordIds = emptySet()
            )
        )
        val emptyWordIds = emptySet<WordInfo>()

        // When
        val resultState = initialState.showConfirmDeleteDialog(emptyWordIds)

        // Then
        // Main functionality check
        assertTrue("Dialog should be opened", resultState.confirmWordDeleteDialogState.isOpen)
        assertEquals(
            "Word IDs should be set to empty set",
            emptyWordIds,
            resultState.confirmWordDeleteDialogState.wordIds
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
    }

    @Test
    fun `should open dialog with single wordId when showConfirmDeleteDialog is called with single word`() {
        // Test case 2: Standard case - showConfirmDeleteDialog opens dialog with single wordId
        // Given
        val initialState = DictionaryTabState(
            isLoading = true,
            topBarState = TopBarState(isActionMode = false),
            termList = TermsSource(pattern = ""),
            termListMap = emptyMap(),
            addWordDialogState = AddWordDialogState(isOpen = true, wordValue = "test"),
            snackbarState = SnackbarState(title = "", show = true),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                isOpen = false,
                wordIds = emptySet()
            )
        )
        val singleWordId = setOf(WordInfo(id = 1L, wordValue = "hello"))

        // When
        val resultState = initialState.showConfirmDeleteDialog(singleWordId)

        // Then
        // Main functionality check
        assertTrue("Dialog should be opened", resultState.confirmWordDeleteDialogState.isOpen)
        assertEquals(
            "Word IDs should be set to single word",
            singleWordId,
            resultState.confirmWordDeleteDialogState.wordIds
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
    }

    @Test
    fun `should open dialog with multiple wordIds when showConfirmDeleteDialog is called with multiple words`() {
        // Test case 3: Standard case - showConfirmDeleteDialog opens dialog with multiple wordIds
        // Given
        val initialState = DictionaryTabState(
            isLoading = false,
            topBarState = TopBarState(
                isActionMode = true,
                actionState = TopBarState.Action(selectedTermIds = setOf(WordInfo(1L, "word1")))
            ),
            termList = TermsSource(pattern = "search"),
            termListMap = mapOf("search" to kotlinx.coroutines.flow.flowOf()),
            addWordDialogState = AddWordDialogState(isOpen = false),
            snackbarState = SnackbarState(),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                isOpen = false,
                wordIds = emptySet()
            )
        )
        val multipleWordIds = setOf(
            WordInfo(id = 1L, wordValue = "hello"),
            WordInfo(id = 2L, wordValue = "world"),
            WordInfo(id = 3L, wordValue = "test")
        )

        // When
        val resultState = initialState.showConfirmDeleteDialog(multipleWordIds)

        // Then
        // Main functionality check
        assertTrue("Dialog should be opened", resultState.confirmWordDeleteDialogState.isOpen)
        assertEquals(
            "Word IDs should be set to multiple words",
            multipleWordIds,
            resultState.confirmWordDeleteDialogState.wordIds
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
    }

    @Test
    fun `should close dialog when hideConfirmDeleteDialog is called on already closed dialog`() {
        // Test case 4: Boundary case - hideConfirmDeleteDialog closes dialog when already closed
        // Given
        val initialState = DictionaryTabState(
            isLoading = true,
            topBarState = TopBarState(),
            termList = TermsSource(pattern = ""),
            termListMap = emptyMap(),
            addWordDialogState = AddWordDialogState(),
            snackbarState = SnackbarState(),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                isOpen = false,
                wordIds = emptySet()
            )
        )

        // When
        val resultState = initialState.hideConfirmDeleteDialog()

        // Then
        // Main functionality check
        assertFalse("Dialog should remain closed", resultState.confirmWordDeleteDialogState.isOpen)
        assertEquals(
            "Word IDs should be empty set",
            emptySet<WordInfo>(),
            resultState.confirmWordDeleteDialogState.wordIds
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
    }

    @Test
    fun `should close dialog and reset wordIds when hideConfirmDeleteDialog is called on open dialog`() {
        // Test case 5: Standard case - hideConfirmDeleteDialog closes dialog and resets wordIds
        // Given
        val initialState = DictionaryTabState(
            isLoading = false,
            topBarState = TopBarState(isActionMode = true),
            termList = TermsSource(pattern = "test"),
            termListMap = mapOf("test" to kotlinx.coroutines.flow.flowOf()),
            addWordDialogState = AddWordDialogState(isOpen = true, wordValue = "test"),
            snackbarState = SnackbarState(title = "test", show = true),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                isOpen = true,
                wordIds = setOf(WordInfo(id = 1L, wordValue = "hello"), WordInfo(id = 2L, wordValue = "world"))
            )
        )

        // When
        val resultState = initialState.hideConfirmDeleteDialog()

        // Then
        // Main functionality check
        assertFalse("Dialog should be closed", resultState.confirmWordDeleteDialogState.isOpen)
        assertEquals(
            "Word IDs should be reset to empty set",
            emptySet<WordInfo>(),
            resultState.confirmWordDeleteDialogState.wordIds
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
    }
}
