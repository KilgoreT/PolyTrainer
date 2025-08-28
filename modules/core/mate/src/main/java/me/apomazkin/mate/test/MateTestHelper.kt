package me.apomazkin.mate.test

import me.apomazkin.mate.MateReducer
import me.apomazkin.mate.ReducerResult
import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

// Test helpers for ReducerResult
fun <STATE, EFFECTS> ReducerResult<STATE, EFFECTS>.assertState(
    expectedState: STATE,
    message: String = "State should be '$expectedState' but was '${state()}'"
) {
    assertEquals(message, expectedState, state())
}

fun <STATE, EFFECTS> ReducerResult<STATE, EFFECTS>.assertEffects(
    expectedEffects: Set<EFFECTS>,
    message: String = "Effects should be $expectedEffects but was ${effects()}"
) {
    assertEquals(message, expectedEffects, effects())
}

fun <STATE, EFFECTS> ReducerResult<STATE, EFFECTS>.assertNoEffects(
    message: String = "Should have no effects but had ${effects().size}: ${effects()}"
) {
    assertTrue(message, effects().isEmpty())
}

inline fun <reified EFFECT_TYPE> ReducerResult<*, *>.assertSingleEffect(
    message: String = "Should have exactly one ${EFFECT_TYPE::class.simpleName} but had ${effects().size}: ${effects()}"
) {
    assertEquals(message, 1, effects().size)
    assertTrue(message, effects().first() is EFFECT_TYPE)
}

fun <STATE, EFFECTS> ReducerResult<STATE, EFFECTS>.assertEffectsCount(
    expectedCount: Int,
    message: String = "Should have $expectedCount effects but had ${effects().size}: ${effects()}"
) {
    assertEquals(message, expectedCount, effects().size)
}

inline fun <reified EFFECT_TYPE> ReducerResult<*, *>.assertHasEffect(
    message: String = "Should have ${EFFECT_TYPE::class.simpleName} but had: ${effects()}"
) {
    assertTrue(message, effects().any { it is EFFECT_TYPE })
}

// Test helpers for MateReducer
fun <STATE, MESSAGE, EFFECT> MateReducer<STATE, MESSAGE, EFFECT>.testReduce(
    initialState: STATE,
    message: MESSAGE
): ReducerResult<STATE, EFFECT> {
    return reduce(initialState, message)
}

fun <STATE, MESSAGE, EFFECT> MateReducer<STATE, MESSAGE, EFFECT>.testScenario(
    initialState: STATE,
    vararg messages: MESSAGE
): List<ReducerResult<STATE, EFFECT>> {
    var currentState = initialState
    return messages.map { message ->
        val result = reduce(currentState, message)
        currentState = result.state()
        result
    }
}
