package me.apomazkin.per_dictionary_components.mate

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Тесты чистой функции [subscriptions]: декларативный вывод набора
 * подписок экрана компонентов словаря из state. Подписка одна и
 * безусловная — dictionaryId фиксирован при создании экрана, а
 * generation пробрасывается из state (retry ломает equality и
 * перезапускает упавшую подписку).
 */
class SubsTest {
    @Test
    fun `default state - single components subscription`() {
        val state = PerDictionaryComponentsScreenState(dictionaryId = 7L)

        assertEquals(
            setOf(
                PerDictionaryComponentsSub.Components(dictionaryId = 7L, generation = 0),
            ),
            state.subscriptions(),
        )
    }

    @Test
    fun `load generation propagates into subscription`() {
        val state = PerDictionaryComponentsScreenState(dictionaryId = 7L, loadGeneration = 3)

        assertEquals(
            setOf(
                PerDictionaryComponentsSub.Components(dictionaryId = 7L, generation = 3),
            ),
            state.subscriptions(),
        )
    }

    @Test
    fun `retry increments generation - subscription identity changes`() {
        // Дифф-механика: рестарт возможен только через новую identity подписки.
        val before = PerDictionaryComponentsScreenState(dictionaryId = 7L, loadGeneration = 1)
            .subscriptions()
        val after = PerDictionaryComponentsScreenState(dictionaryId = 7L, loadGeneration = 2)
            .subscriptions()

        assertEquals(1, (before - after).size)
        assertEquals(1, (after - before).size)
    }
}
