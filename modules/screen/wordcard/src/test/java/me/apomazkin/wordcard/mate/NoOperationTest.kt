package me.apomazkin.wordcard.mate

import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

/**
 * Reducer tests for NoOperation Msg — no-op identity.
 */
class NoOperationTest {

    @Test
    fun `NoOperation leaves NotLoaded state untouched`() {
        val reducer = WordCardReducer()
        val initial = WordCardState(isLoading = true, wordState = WordState.NotLoaded)

        val result = reducer.testReduce(initial, Msg.NoOperation)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }

    @Test
    fun `NoOperation leaves Loaded state untouched`() {
        val reducer = WordCardReducer()
        val initial = WordCardState(
            isLoading = false,
            wordState = WordState.Loaded(id = 1L, added = Date(0L), value = "w"),
            lexemeList = listOf(
                LexemeState(id = 5L, translation = TextValueState(origin = "x", isEdit = false)),
            ),
        )

        val result = reducer.testReduce(initial, Msg.NoOperation)

        assertEquals(initial, result.state())
        result.assertNoEffects()
    }
}
