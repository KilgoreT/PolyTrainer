package me.apomazkin.settingstab.logic

import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.testReduce
import me.apomazkin.settingstab.SettingsNavigationEffect
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Test cases for SettingsTabReducer (IS481 — Msg.OpenComponentsManager nav-effect).
 *
 * === Msg.OpenComponentsManager ===
 * 1. Standard: OpenComponentsManager → state immutable, emit SettingsNavigationEffect.OpenComponentsManager
 */
class SettingsTabReducerTest {

    private val reducer = SettingsTabReducer(
        logger = object : LexemeLogger {
            override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {}
        }
    )

    // === Msg.OpenComponentsManager ===

    @Test
    fun `should emit OpenComponentsManager nav effect when Msg OpenComponentsManager received`() {
        // Test case 1: Standard - IS481, юзер тапает «Конструктор компонентов» в SettingsTab
        // Given
        val initialState = SettingsTabState()

        // When
        val result = reducer.testReduce(initialState, Msg.OpenComponentsManager)

        // Then
        assertEquals("state should be unchanged", initialState, result.state())
        result.assertEffects(setOf(SettingsNavigationEffect.OpenComponentsManager))
    }
}
