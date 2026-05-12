package me.apomazkin.wordcard.mate

import me.apomazkin.mate.NavigationEffect
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertSingleEffect
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test cases:
 * 1. NavigateBack emits NavigationEffect.Back, state unchanged
 * 2. NavigateBack with complex state — only effect emitted, state preserved
 * 3. NavigateBack while loading — emits Back, isLoading preserved
 * 4. NavigateBack with active dialogs — emits Back, dialogs preserved
 */
class NavigateBackTest {

    @Test
    fun `NavigateBack emits Back effect without state change`() {
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = false),
            addLexemeBottomState = AddLexemeBottomState(show = false),
            isLoading = false,
            wordState = WordState(id = 123L, value = "test"),
            lexemeList = listOf(),
            snackbarState = SnackbarState(),
        )

        val result = reducer.testReduce(initialState, Msg.NavigateBack)

        result.assertSingleEffect<NavigationEffect.Back>()
        assertEquals("state should remain unchanged", initialState, result.state())
    }

    @Test
    fun `NavigateBack with complex state preserves all fields`() {
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true),
            addLexemeBottomState = AddLexemeBottomState(
                show = true,
                isTranslationCheck = true,
                isDefinitionCheck = false,
            ),
            isLoading = true,
            wordState = WordState(
                id = 456L,
                value = "complex word",
                isEditMode = true,
                edited = "edited complex word",
                showWarningDialog = true,
            ),
            lexemeList = listOf(
                LexemeState(
                    id = 1L,
                    translation = TextValueState(origin = "translation", isEdit = false),
                    definition = TextValueState(origin = "definition", isEdit = true),
                    isMenuOpen = true,
                ),
            ),
            snackbarState = SnackbarState(title = "Test message", show = true),
        )

        val result = reducer.testReduce(initialState, Msg.NavigateBack)

        result.assertSingleEffect<NavigationEffect.Back>()
        assertEquals(initialState, result.state())
    }

    @Test
    fun `NavigateBack while loading preserves isLoading`() {
        val reducer = WordCardReducer()
        val initialState = WordCardState(isLoading = true)

        val result = reducer.testReduce(initialState, Msg.NavigateBack)

        result.assertSingleEffect<NavigationEffect.Back>()
        assertEquals(true, result.state().isLoading)
    }

    @Test
    fun `NavigateBack preserves active dialogs`() {
        val reducer = WordCardReducer()
        val initialState = WordCardState(
            topBarState = TopBarState(isMenuOpen = true),
            addLexemeBottomState = AddLexemeBottomState(show = true),
            wordState = WordState(showWarningDialog = true),
            snackbarState = SnackbarState(title = "Active", show = true),
        )

        val result = reducer.testReduce(initialState, Msg.NavigateBack)

        result.assertSingleEffect<NavigationEffect.Back>()
        assertEquals(initialState, result.state())
    }
}
