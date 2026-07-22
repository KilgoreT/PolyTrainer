package me.apomazkin.lexeme

import me.apomazkin.lexeme.HierarchyFixtures.option
import me.apomazkin.lexeme.HierarchyFixtures.removed
import me.apomazkin.lexeme.HierarchyFixtures.type
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * IS486 фаза 1, зона A5: degraded-предикат (spec §6) — компонент degraded ⇔ цель мертва.
 */
class DegradedPredicateTest {

    private val graph = ComponentGraph(
        types = listOf(
            type(id = 1),
            type(id = 2, removedAt = removed),
            type(id = 3, template = ComponentTemplate.CHOICE),
        ),
        options = listOf(
            option(id = 30, ownerTypeId = 3),
            option(id = 31, ownerTypeId = 3, removedAt = removed),
        ),
    )

    @Test
    fun `lexeme target is never degraded`() {
        assertFalse(graph.isDegraded(type(id = 10, dependsOn = DependencyTarget.Lexeme)))
    }

    @Test
    fun `alive component target is not degraded`() {
        assertFalse(
            graph.isDegraded(type(id = 10, dependsOn = DependencyTarget.Component(ComponentTypeId(1)))),
        )
    }

    @Test
    fun `removed component target is degraded`() {
        assertTrue(
            graph.isDegraded(type(id = 10, dependsOn = DependencyTarget.Component(ComponentTypeId(2)))),
        )
    }

    @Test
    fun `alive option target is not degraded`() {
        assertFalse(graph.isDegraded(type(id = 10, dependsOn = DependencyTarget.Option(30))))
    }

    @Test
    fun `removed option target is degraded`() {
        assertTrue(graph.isDegraded(type(id = 10, dependsOn = DependencyTarget.Option(31))))
    }

    @Test
    fun `missing target is degraded`() {
        assertTrue(
            graph.isDegraded(type(id = 10, dependsOn = DependencyTarget.Component(ComponentTypeId(99)))),
        )
    }
}
