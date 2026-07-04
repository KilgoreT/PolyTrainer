package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Date

/**
 * Reducer tests for CloseTopBarMenu — simple toggle with guard !isMenuOpen.
 */
class CloseTopBarMenuTest {

    @Test
    fun `CloseTopBarMenu sets isMenuOpen false`() {
        val reducer = WordCardReducer()
        val initial = WordCardState(
            isLoading = false,
            wordState = WordState.Loaded(id = 1L, dictionaryId = 3L, added = Date(0L), value = "w"),
            topBarState = TopBarState(isMenuOpen = true),
        )

        val result = reducer.testReduce(initial, Msg.CloseTopBarMenu)

        assertFalse(result.state().topBarState.isMenuOpen)
        result.assertNoEffects()
    }

    @Test
    fun `CloseTopBarMenu on already-closed menu is no-op (guard)`() {
        val reducer = WordCardReducer()
        val initial = WordCardState(
            isLoading = false,
            wordState = WordState.Loaded(id = 1L, dictionaryId = 3L, added = Date(0L), value = "w"),
            topBarState = TopBarState(isMenuOpen = false),
        )

        val result = reducer.testReduce(initial, Msg.CloseTopBarMenu)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }
}
