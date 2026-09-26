package me.apomazkin.components_manager.mate

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Тесты чистой функции [subscriptions]: декларативный вывод набора
 * подписок менеджера компонентов из state. Раннер диффит набор после
 * каждого изменения state — здесь проверяем, что обе подписки
 * безусловны, а generation списка типов пробрасывается из state
 * (retry ломает equality и перезапускает упавшую подписку).
 */
class SubsTest {
    @Test
    fun `default state - both subscriptions with generation zero`() {
        val state = ComponentsManagerScreenState()

        assertEquals(
            setOf(
                ComponentsManagerSub.AllTypes(generation = 0),
                ComponentsManagerSub.Dictionaries,
            ),
            state.subscriptions(),
        )
    }

    @Test
    fun `load generation propagates into AllTypes subscription`() {
        val state = ComponentsManagerScreenState(loadGeneration = 3)

        assertEquals(
            setOf(
                ComponentsManagerSub.AllTypes(generation = 3),
                ComponentsManagerSub.Dictionaries,
            ),
            state.subscriptions(),
        )
    }

    @Test
    fun `retry increments generation - subscription identity changes`() {
        // Дифф-механика: рестарт возможен только через новую identity подписки.
        val before = ComponentsManagerScreenState(loadGeneration = 1).subscriptions()
        val after = ComponentsManagerScreenState(loadGeneration = 2).subscriptions()

        assertEquals(1, (before - after).size)
        assertEquals(1, (after - before).size)
    }
}
