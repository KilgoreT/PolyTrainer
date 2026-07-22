package me.apomazkin.lexeme

import me.apomazkin.lexeme.HierarchyFixtures.removed
import me.apomazkin.lexeme.HierarchyFixtures.type
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * IS486 фаза 1, зона A7: уникальность builtin «(ключ, словарь)» (spec §10) —
 * замена дропнутому UNIQUE-индексу `system_key`.
 */
class BuiltInKeyAvailabilityTest {

    @Test
    fun `same key same dictionary is taken`() {
        val graph = ComponentGraph(
            types = listOf(type(id = 1, systemKey = BuiltInComponent.TRANSLATION, dictionaryId = 1L)),
        )
        assertFalse(graph.isBuiltInKeyAvailable(BuiltInComponent.TRANSLATION, dictionaryId = 1L))
    }

    @Test
    fun `same key other dictionary is available`() {
        val graph = ComponentGraph(
            types = listOf(type(id = 1, systemKey = BuiltInComponent.TRANSLATION, dictionaryId = 1L)),
        )
        assertTrue(graph.isBuiltInKeyAvailable(BuiltInComponent.TRANSLATION, dictionaryId = 2L))
    }

    @Test
    fun `removed row frees the key`() {
        val graph = ComponentGraph(
            types = listOf(
                type(id = 1, systemKey = BuiltInComponent.TRANSLATION, dictionaryId = 1L, removedAt = removed),
            ),
        )
        assertTrue(graph.isBuiltInKeyAvailable(BuiltInComponent.TRANSLATION, dictionaryId = 1L))
    }

    @Test
    fun `other key same dictionary is available`() {
        val graph = ComponentGraph(
            types = listOf(type(id = 1, systemKey = BuiltInComponent.TRANSLATION, dictionaryId = 1L)),
        )
        assertTrue(graph.isBuiltInKeyAvailable(BuiltInComponent.PART_OF_SPEECH, dictionaryId = 1L))
    }
}
