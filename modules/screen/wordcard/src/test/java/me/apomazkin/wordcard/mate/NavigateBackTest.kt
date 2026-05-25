package me.apomazkin.wordcard.mate

import me.apomazkin.mate.NavigationEffect
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertSingleEffect
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.Date

/**
 * Reducer tests for NavigateBack (shared UI/handler-confirm ветка).
 *
 * Test cases:
 * - NavigateBack emits NavigationEffect.Back.
 * - NavigateBack clears isPendingDbOp (handler-confirm после RemoveWord).
 * - NavigateBack works regardless of WordState variant.
 */
class NavigateBackTest {

    @Test
    fun `NavigateBack emits Back effect`() {
        val reducer = WordCardReducer()
        val initial = WordCardState(
            isLoading = false,
            wordState = WordState.Loaded(id = 1L, added = Date(0L), value = "w"),
        )

        val result = reducer.testReduce(initial, Msg.NavigateBack)

        result.assertSingleEffect<NavigationEffect.Back>()
    }

    @Test
    fun `NavigateBack clears isPendingDbOp`() {
        val reducer = WordCardReducer()
        val initial = WordCardState(
            isLoading = false,
            isPendingDbOp = true,
            wordState = WordState.Loaded(id = 1L, added = Date(0L), value = "w"),
        )

        val result = reducer.testReduce(initial, Msg.NavigateBack)

        assertFalse(result.state().isPendingDbOp)
        result.assertSingleEffect<NavigationEffect.Back>()
    }

    @Test
    fun `NavigateBack works under NotLoaded too`() {
        val reducer = WordCardReducer()
        val initial = WordCardState(isLoading = true, wordState = WordState.NotLoaded)

        val result = reducer.testReduce(initial, Msg.NavigateBack)

        result.assertSingleEffect<NavigationEffect.Back>()
    }
}
