package me.apomazkin.wordcard.mate

import io.github.kilgoret.mate.state
import io.github.kilgoret.mate.test.assertNoEffects
import io.github.kilgoret.mate.test.testReduce
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Reducer tests for OpenTopBarMenu — simple toggle without Effect.
 */
class OpenTopBarMenuTest {
    @Test
    fun `OpenTopBarMenu sets isMenuOpen true`() {
        val reducer = WordCardReducer(NoopLogger)
        val initial = WordCardState(
            isLoading = false,
            wordState = WordState.Loaded(id = 1L, dictionaryId = 3L, added = Date(0L), value = "w"),
            topBarState = TopBarState(isMenuOpen = false),
        )

        val result = reducer.testReduce(initial, Msg.OpenTopBarMenu)

        assertTrue(result.state().topBarState.isMenuOpen)
        result.assertNoEffects()
    }

    @Test
    fun `OpenTopBarMenu is idempotent when already open`() {
        val reducer = WordCardReducer(NoopLogger)
        val initial = WordCardState(
            isLoading = false,
            wordState = WordState.Loaded(id = 1L, dictionaryId = 3L, added = Date(0L), value = "w"),
            topBarState = TopBarState(isMenuOpen = true),
        )

        val result = reducer.testReduce(initial, Msg.OpenTopBarMenu)

        assertTrue(result.state().topBarState.isMenuOpen)
        result.assertNoEffects()
    }
}
