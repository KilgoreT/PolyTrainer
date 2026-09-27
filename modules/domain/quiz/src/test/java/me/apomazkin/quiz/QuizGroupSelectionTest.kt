package me.apomazkin.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * IS500 | Воронка resolveQuizGroupState: порог (границы, действие на
 * «Все»), пометка eligible, резолв персиста с молчаливым фолбэком,
 * кликабельность карточки.
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
            persistedGroupId = null,
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
            persistedGroupId = null,
        )

        assertEquals(listOf(1L, 2L), state.options.groups.map { it.id })
    }

    @Test
    fun `threshold applies to All too`() {
        val state = resolveQuizGroupState(
            groupCounts = emptyList(),
            dictionaryWordCount = MIN_QUIZ_WORDS - 1,
            persistedGroupId = null,
        )

        assertFalse(state.options.isAllEligible)
        assertEquals(MIN_QUIZ_WORDS - 1, state.options.allWordCount)
    }

    @Test
    fun `custom threshold parameter - per-type override slot`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to 3),
            dictionaryWordCount = 3,
            persistedGroupId = null,
            threshold = 5,
        )

        assertFalse(state.options.isAllEligible)
        assertFalse(state.options.groups.single().isEligible)
    }

    // === Резолв персиста ===

    @Test
    fun `persisted eligible group - kept`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to 5),
            dictionaryWordCount = 5,
            persistedGroupId = 1L,
        )

        assertEquals(1L, state.selectedGroupId)
    }

    @Test
    fun `persisted null - All`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to 5),
            dictionaryWordCount = 5,
            persistedGroupId = null,
        )

        assertNull(state.selectedGroupId)
    }

    @Test
    fun `persisted dead group - silent fallback to All`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to 5),
            dictionaryWordCount = 5,
            persistedGroupId = 99L,
        )

        assertNull(state.selectedGroupId)
    }

    @Test
    fun `persisted shrunken group - silent fallback to All`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to MIN_QUIZ_WORDS - 1),
            dictionaryWordCount = 5,
            persistedGroupId = 1L,
        )

        assertNull(state.selectedGroupId)
    }

    // === Кликабельность карточки ===

    @Test
    fun `no eligible options - card disabled`() {
        val state = resolveQuizGroupState(
            groupCounts = counts(1L to 1),
            dictionaryWordCount = 2,
            persistedGroupId = null,
        )

        assertFalse(state.options.hasEligibleOption)
    }

    @Test
    fun `eligible All - card enabled even without groups`() {
        val state = resolveQuizGroupState(
            groupCounts = emptyList(),
            dictionaryWordCount = 3,
            persistedGroupId = null,
        )

        assertTrue(state.options.hasEligibleOption)
    }
}
