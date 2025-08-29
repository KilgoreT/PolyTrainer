package me.apomazkin.dictionarytab.logic.ext

import me.apomazkin.dictionarytab.logic.*
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.mate.Effect
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for utility extensions
 * 
 * Test cases:
 * 1. Standard case: choose executes yes branch when condition is true
 * 2. Standard case: choose executes no branch when condition is false
 * 3. Boundary case: choose handles edge cases correctly
 */
class UtilityExtTest {

    @Test
    fun `should execute yes branch when condition is true`() {
        // Test case 1: Standard case - choose executes yes branch when condition is true
        // Given
        val initialState = DictionaryTabState(
            isLoading = false,
            topBarState = TopBarState(
                isActionMode = false,
                actionState = TopBarState.Action(selectedTermIds = emptySet())
            ),
            termList = TermsSource(pattern = "test"),
            termListMap = emptyMap(),
            addWordDialogState = AddWordDialogState(
                isOpen = false,
                wordValue = "",
                wordId = null
            ),
            snackbarState = SnackbarState(
                title = "",
                show = false
            ),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                isOpen = false,
                wordIds = emptySet()
            )
        )
        
        val expectedEffects = setOf<Effect>(DatasourceEffect.LoadTermFlow())
        
        // When
        val (resultState, resultEffects) = initialState.choose<DictionaryTabState, Effect>(
            check = { it.isLoading.not() },
            yes = { state ->
                state.copy(isLoading = true) to expectedEffects
            },
            no = { state ->
                state.copy(isLoading = false) to emptySet()
            }
        )
        
        // Then
        // Main functionality check
        assertTrue(
            "Loading state should be enabled when yes branch executes",
            resultState.isLoading
        )
        assertEquals(
            "Effects should match expected effects from yes branch",
            expectedEffects,
            resultEffects
        )
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "TopBarState should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "TermList should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "TermListMap should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
        )
        assertEquals(
            "AddWordDialogState should remain unchanged",
            initialState.addWordDialogState,
            resultState.addWordDialogState
        )
        assertEquals(
            "SnackbarState should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "ConfirmWordDeleteDialogState should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }

    @Test
    fun `should execute no branch when condition is false`() {
        // Test case 2: Standard case - choose executes no branch when condition is false
        // Given
        val initialState = DictionaryTabState(
            isLoading = true,
            topBarState = TopBarState(
                isActionMode = true,
                actionState = TopBarState.Action(selectedTermIds = setOf(WordInfo(id = 1L, wordValue = "test")))
            ),
            termList = TermsSource(pattern = "search"),
            termListMap = mapOf("key" to kotlinx.coroutines.flow.flowOf()),
            addWordDialogState = AddWordDialogState(
                isOpen = true,
                wordValue = "new word",
                wordId = 123L
            ),
            snackbarState = SnackbarState(
                title = "Error message",
                show = true
            ),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState(
                isOpen = true,
                wordIds = setOf(WordInfo(id = 2L, wordValue = "delete"))
            )
        )
        
        val expectedEffects = setOf<Effect>(UiEffect.ShowNotification(message = "Operation completed"))
        
        // When
        val (resultState, resultEffects) = initialState.choose<DictionaryTabState, Effect>(
            check = { it.isLoading.not() },
            yes = { state ->
                state.copy(isLoading = false) to emptySet()
            },
            no = { state ->
                state.copy(isLoading = false) to expectedEffects
            }
        )
        
        // Then
        // Main functionality check
        assertFalse(
            "Loading state should be disabled when no branch executes",
            resultState.isLoading
        )
        assertEquals(
            "Effects should match expected effects from no branch",
            expectedEffects,
            resultEffects
        )
        
        // Immutability checks - ALL other properties must remain unchanged
        assertEquals(
            "TopBarState should remain unchanged",
            initialState.topBarState,
            resultState.topBarState
        )
        assertEquals(
            "TermList should remain unchanged",
            initialState.termList,
            resultState.termList
        )
        assertEquals(
            "TermListMap should remain unchanged",
            initialState.termListMap,
            resultState.termListMap
        )
        assertEquals(
            "AddWordDialogState should remain unchanged",
            initialState.addWordDialogState,
            resultState.addWordDialogState
        )
        assertEquals(
            "SnackbarState should remain unchanged",
            initialState.snackbarState,
            resultState.snackbarState
        )
        assertEquals(
            "ConfirmWordDeleteDialogState should remain unchanged",
            initialState.confirmWordDeleteDialogState,
            resultState.confirmWordDeleteDialogState
        )
    }

    @Test
    fun `should handle edge cases correctly`() {
        // Test case 3: Boundary case - choose handles edge cases correctly
        // Given
        val initialState = DictionaryTabState(
            isLoading = false,
            topBarState = TopBarState(),
            termList = TermsSource(pattern = ""),
            termListMap = emptyMap(),
            addWordDialogState = AddWordDialogState(),
            snackbarState = SnackbarState(),
            confirmWordDeleteDialogState = ConfirmWordDeleteDialogState()
        )
        
        // When - testing with empty effects
        val (resultStateEmpty, resultEffectsEmpty) = initialState.choose<DictionaryTabState, Effect>(
            check = { true },
            yes = { state -> state to emptySet() },
            no = { state -> state to emptySet() }
        )
        
        // Then - empty effects case
        assertEquals(
            "State should remain unchanged when both branches return empty effects",
            initialState,
            resultStateEmpty
        )
        assertTrue(
            "Effects should be empty when yes branch returns empty set",
            resultEffectsEmpty.isEmpty()
        )
        
        // When - testing with complex condition
        val (resultStateComplex, resultEffectsComplex) = initialState.choose<DictionaryTabState, Effect>(
            check = { state -> 
                state.isLoading.not() && 
                state.topBarState.isActionMode.not() && 
                state.termList.pattern.isEmpty()
            },
            yes = { state ->
                state.copy(
                    isLoading = true,
                    topBarState = state.topBarState.copy(isActionMode = true)
                ) to setOf(DatasourceEffect.LoadTermFlow())
            },
            no = { state ->
                state.copy(isLoading = false) to emptySet()
            }
        )
        
        // Then - complex condition case
        assertTrue(
            "Loading state should be enabled when complex condition is true",
            resultStateComplex.isLoading
        )
        assertTrue(
            "Action mode should be enabled when complex condition is true",
            resultStateComplex.topBarState.isActionMode
        )
        assertEquals(
            "Effects should contain LoadTermFlow when yes branch executes",
            setOf(DatasourceEffect.LoadTermFlow()),
            resultEffectsComplex
        )
        
        // Immutability checks for complex case
        assertEquals(
            "TermList should remain unchanged in complex condition",
            initialState.termList,
            resultStateComplex.termList
        )
        assertEquals(
            "TermListMap should remain unchanged in complex condition",
            initialState.termListMap,
            resultStateComplex.termListMap
        )
        assertEquals(
            "AddWordDialogState should remain unchanged in complex condition",
            initialState.addWordDialogState,
            resultStateComplex.addWordDialogState
        )
        assertEquals(
            "SnackbarState should remain unchanged in complex condition",
            initialState.snackbarState,
            resultStateComplex.snackbarState
        )
        assertEquals(
            "ConfirmWordDeleteDialogState should remain unchanged in complex condition",
            initialState.confirmWordDeleteDialogState,
            resultStateComplex.confirmWordDeleteDialogState
        )
    }
}
