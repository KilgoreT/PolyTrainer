package me.apomazkin.lexeme

import me.apomazkin.lexeme.HierarchyFixtures.removed
import me.apomazkin.lexeme.HierarchyFixtures.type
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * IS486 фаза 1, зона A6: инвариант последнего включённого ядра (spec §7.8).
 * Единая проверка для всех трёх путей потери: disable / soft-delete / перепривязка.
 */
class CoreLossCheckTest {

    private fun graphOf(vararg types: ComponentType) = ComponentGraph(types = types.toList())

    @Test
    fun `two enabled cores - losing one is ok`() {
        val graph = graphOf(
            type(id = 1, core = true),
            type(id = 2, core = true),
        )
        assertEquals(CoreLossCheck.Ok, graph.checkCoreLoss(ComponentTypeId(1)))
    }

    @Test
    fun `single enabled core - losing it is LastEnabledCore`() {
        val graph = graphOf(
            type(id = 1, core = true),
            type(id = 2, core = true, enabled = false),
        )
        assertEquals(CoreLossCheck.LastEnabledCore, graph.checkCoreLoss(ComponentTypeId(1)))
    }

    @Test
    fun `losing non-core is always ok`() {
        val graph = graphOf(
            type(id = 1, core = true),
            type(id = 2, core = false),
        )
        assertEquals(CoreLossCheck.Ok, graph.checkCoreLoss(ComponentTypeId(2)))
    }

    @Test
    fun `removed core does not count as backup`() {
        val graph = graphOf(
            type(id = 1, core = true),
            type(id = 2, core = true, removedAt = removed),
        )
        assertEquals(CoreLossCheck.LastEnabledCore, graph.checkCoreLoss(ComponentTypeId(1)))
    }

    @Test
    fun `losing already disabled core is ok`() {
        val graph = graphOf(
            type(id = 1, core = true),
            type(id = 2, core = true, enabled = false),
        )
        assertEquals(CoreLossCheck.Ok, graph.checkCoreLoss(ComponentTypeId(2)))
    }
}
