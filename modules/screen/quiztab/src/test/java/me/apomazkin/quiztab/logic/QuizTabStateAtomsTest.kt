package me.apomazkin.quiztab.logic

import me.apomazkin.quiz.QuizGroup
import me.apomazkin.quiz.QuizGroupLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Атомы набора групп: оптимистичный выбор и подпись по порядку опций. */
class QuizTabStateAtomsTest {

    private val atoms = object : QuizTabStateAtoms(NoopLogger) {}

    private fun group(id: Long, name: String) =
        QuizGroup(id = id, name = name, wordCount = 5, isEligible = true)

    private val state = QuizTabState(
        groupOptions = listOf(group(2L, "Быт"), group(1L, "Дом"), group(3L, "Еда")),
    )

    @Test
    fun `pickGroups - replaces set`() = with(atoms) {
        assertEquals(setOf(1L, 3L), state.pickGroups(setOf(1L, 3L)).selectedGroupIds)
    }

    @Test
    fun `applySelectionLabel - empty set is All`() = with(atoms) {
        assertNull(state.applySelectionLabel().selectionLabel)
    }

    @Test
    fun `applySelectionLabel - first in options order, rest counted`() = with(atoms) {
        val labeled = state
            .pickGroups(setOf(3L, 1L))
            .applySelectionLabel()

        assertEquals(QuizGroupLabel(first = "Дом", more = 1), labeled.selectionLabel)
    }

    @Test
    fun `applySelectionLabel - single group, no more`() = with(atoms) {
        val labeled = state
            .pickGroups(setOf(2L))
            .applySelectionLabel()

        assertEquals(QuizGroupLabel(first = "Быт", more = 0), labeled.selectionLabel)
    }
}
