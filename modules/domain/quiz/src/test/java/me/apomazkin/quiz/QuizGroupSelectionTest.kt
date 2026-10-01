package me.apomazkin.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Воронка resolveQuizGroupState: порог (границы, действие на «Все»),
 * пометка eligible, поэлементный резолв набора с молчаливым фолбэком,
 * кликабельность карточки; подпись набора quizGroupLabel.
 */
class QuizGroupSelectionTest {

    private fun counts(vararg pairs: Pair<Long, Int>) =
        pairs.map { (id, count) ->
            QuizGroupCount(id = id, name = "g$id", wordCount = count)
        }

    // === Порог ===

    @Test
    fun `group at threshold - eligible, below - not`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to MIN_QUIZ_WORDS, 2L to MIN_QUIZ_WORDS - 1),
            dictionaryWordCount = 10,
            persistedGroupIds = emptySet(),
        )

        val byId = state.options.groups.associateBy { it.id }
        assertTrue(requireNotNull(byId[1L]).isEligible)
        assertFalse(requireNotNull(byId[2L]).isEligible)
    }

    @Test
    fun `ineligible groups stay in list as disabled items`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to 0, 2L to 2),
            dictionaryWordCount = 10,
            persistedGroupIds = emptySet(),
        )

        assertEquals(listOf(1L, 2L), state.options.groups.map { it.id })
    }

    @Test
    fun `threshold applies to All too`() {
        val state = resolveQuizGroupState(
            groupCounts = emptyList(),
            dictionaryWordCount = MIN_QUIZ_WORDS - 1,
            persistedGroupIds = emptySet(),
        )

        assertFalse(state.options.isAllEligible)
        assertEquals(MIN_QUIZ_WORDS - 1, state.options.allWordCount)
    }

    @Test
    fun `custom threshold parameter - per-type override slot`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to 3),
            dictionaryWordCount = 3,
            persistedGroupIds = emptySet(),
            threshold = 5,
        )

        assertFalse(state.options.isAllEligible)
        assertFalse(state.options.groups.single().isEligible)
    }

    // === Резолв набора ===

    @Test
    fun `persisted eligible groups - kept`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to 5, 2L to 4),
            dictionaryWordCount = 9,
            persistedGroupIds = setOf(1L, 2L),
        )

        assertEquals(setOf(1L, 2L), state.selectedGroupIds)
    }

    @Test
    fun `persisted empty - All`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to 5),
            dictionaryWordCount = 5,
            persistedGroupIds = emptySet(),
        )

        assertTrue(state.selectedGroupIds.isEmpty())
    }

    @Test
    fun `dead group drops out, live ones stay`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to 5),
            dictionaryWordCount = 5,
            persistedGroupIds = setOf(1L, 99L),
        )

        assertEquals(setOf(1L), state.selectedGroupIds)
    }

    @Test
    fun `shrunken group drops out, live ones stay`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to MIN_QUIZ_WORDS - 1, 2L to 5),
            dictionaryWordCount = 7,
            persistedGroupIds = setOf(1L, 2L),
        )

        assertEquals(setOf(2L), state.selectedGroupIds)
    }

    @Test
    fun `all persisted dropped - All`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to MIN_QUIZ_WORDS - 1),
            dictionaryWordCount = 5,
            persistedGroupIds = setOf(1L, 99L),
        )

        assertTrue(state.selectedGroupIds.isEmpty())
    }

    // === Кликабельность карточки ===

    @Test
    fun `no eligible options - card disabled`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to 1),
            dictionaryWordCount = 2,
            persistedGroupIds = emptySet(),
        )

        assertFalse(state.options.hasEligibleOption)
    }

    @Test
    fun `eligible All - card enabled even without groups`() {
        val state = resolveQuizGroupState(
            groupCounts = emptyList(),
            dictionaryWordCount = 3,
            persistedGroupIds = emptySet(),
        )

        assertTrue(state.options.hasEligibleOption)
    }

    // === Подпись ===

    private fun groups(vararg names: Pair<Long, String>) =
        names.map { (id, name) -> QuizGroup(id = id, name = name, wordCount = 5, isEligible = true) }

    @Test
    fun `label - nothing selected is All`() {
        assertNull(quizGroupLabel(groups(1L to "Быт"), emptySet()))
    }

    @Test
    fun `label - single group has no more`() {
        assertEquals(
            QuizGroupLabel(first = "Быт", more = 0),
            quizGroupLabel(groups(1L to "Быт", 2L to "Дом"), setOf(1L)),
        )
    }

    @Test
    fun `label - first by list order, rest counted`() {
        val list = groups(3L to "Быт", 1L to "Дом", 2L to "Еда")

        assertEquals(
            QuizGroupLabel(first = "Быт", more = 2),
            quizGroupLabel(list, setOf(2L, 1L, 3L)),
        )
    }

    @Test
    fun `label - same names matched by id`() {
        val list = groups(1L to "Слова", 2L to "Слова")

        assertEquals(QuizGroupLabel(first = "Слова", more = 0), quizGroupLabel(list, setOf(2L)))
    }

    @Test
    fun `label - selected ids missing from list is All`() {
        assertNull(quizGroupLabel(groups(1L to "Быт"), setOf(99L)))
    }
}
