package me.apomazkin.lexeme

import me.apomazkin.lexeme.HierarchyFixtures.option
import me.apomazkin.lexeme.HierarchyFixtures.removed
import me.apomazkin.lexeme.HierarchyFixtures.type
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * IS486 фаза 1, зона A4: ацикличность (spec §8).
 * Подъём по ссылкам НЕЗАВИСИМО от removed-статуса узлов.
 *
 * Граф D1: A(1) ← B(2) ← C(3); часть речи PoS(4, CHOICE, опции 40/41) ← D(5 ← опция 40);
 * dead(6, removed, ← C(3)); E(7 ← dead 6).
 */
class AcyclicityCheckTest {

    private val graph = ComponentGraph(
        types = listOf(
            type(id = 1),
            type(id = 2, dependsOn = DependencyTarget.Component(ComponentTypeId(1))),
            type(id = 3, dependsOn = DependencyTarget.Component(ComponentTypeId(2))),
            type(id = 4, template = ComponentTemplate.CHOICE),
            type(id = 5, dependsOn = DependencyTarget.Option(40)),
            type(id = 6, removedAt = removed, dependsOn = DependencyTarget.Component(ComponentTypeId(3))),
            type(id = 7, dependsOn = DependencyTarget.Component(ComponentTypeId(6))),
        ),
        options = listOf(
            option(id = 40, ownerTypeId = 4),
            option(id = 41, ownerTypeId = 4),
        ),
    )

    private fun check(componentId: Long, target: DependencyTarget) =
        graph.checkAcyclic(ComponentTypeId(componentId), target)

    @Test
    fun `assigning ancestor as target is ok`() {
        // C(3) перепривязывается на A(1) — A не в поддереве C.
        assertEquals(AcyclicityCheck.Ok, check(3, DependencyTarget.Component(ComponentTypeId(1))))
    }

    @Test
    fun `self reference is cycle`() {
        assertEquals(AcyclicityCheck.CycleDetected, check(1, DependencyTarget.Component(ComponentTypeId(1))))
    }

    @Test
    fun `descendant at depth 3 as target is cycle`() {
        // A(1) → C(3), где C — потомок A через B: A←B←C.
        assertEquals(AcyclicityCheck.CycleDetected, check(1, DependencyTarget.Component(ComponentTypeId(3))))
    }

    @Test
    fun `cycle through option link in the middle of chain`() {
        // PoS(4) → D(5); D зависит от опции 40, принадлежащей PoS → кольцо.
        assertEquals(AcyclicityCheck.CycleDetected, check(4, DependencyTarget.Component(ComponentTypeId(5))))
    }

    @Test
    fun `rebinding to own option is cycle of length one`() {
        assertEquals(AcyclicityCheck.CycleDetected, check(4, DependencyTarget.Option(40)))
    }

    @Test
    fun `cycle through removed link is detected`() {
        // C(3) → dead(6); dead зависит от C → кольцо через мёртвое звено.
        assertEquals(AcyclicityCheck.CycleDetected, check(3, DependencyTarget.Component(ComponentTypeId(6))))
    }

    @Test
    fun `dead chain without cycle is ok`() {
        // Новая цель E(7) → dead(6): подъём 6→3→2→1→лексема, E не встретили.
        assertEquals(AcyclicityCheck.Ok, check(7, DependencyTarget.Component(ComponentTypeId(6))))
    }

    @Test
    fun `option target of foreign component is ok when no cycle`() {
        // B(2) → опция 41 части речи: подъём 41→PoS(4)→лексема, B не встретили.
        assertEquals(AcyclicityCheck.Ok, check(2, DependencyTarget.Option(41)))
    }
}
