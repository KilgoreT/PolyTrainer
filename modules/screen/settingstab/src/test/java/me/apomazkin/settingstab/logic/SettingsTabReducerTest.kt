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
 * Test cases for SettingsTabReducer.
 *
 * IS486 фаза 4 (spec §20): вход в ComponentsManager из Settings выпилен —
 * тест `Msg.OpenComponentsManager` удалён вместе с Msg/эффектом.
 */
class SettingsTabReducerTest {

    private val reducer = SettingsTabReducer(
        logger = object : LexemeLogger {
            override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {}
        }
    )

    @Test
    fun `should emit OpenLangManagement nav effect when Msg OpenLangManagement received`() {
        val initialState = SettingsTabState()

        val result = reducer.testReduce(initialState, Msg.OpenLangManagement)

        assertEquals("state should be unchanged", initialState, result.state())
        result.assertEffects(setOf(SettingsNavigationEffect.OpenLangManagement))
    }
}
