package me.apomazkin.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Кодек набора групп в pref: round-trip, «Все», мусор и его пометка. */
class QuizGroupsCodecTest {

    @Test
    fun `round trip - sorted ids joined by comma`() {
        val encoded = encodeQuizGroupIds(setOf(15L, 3L, 40L))

        assertEquals("3,15,40", encoded)
        assertEquals(PersistedQuizGroups(setOf(3L, 15L, 40L), hasGarbage = false), decodeQuizGroupIds(encoded))
    }

    @Test
    fun `empty set - null, key erased`() {
        assertNull(encodeQuizGroupIds(emptySet()))
    }

    @Test
    fun `null or empty raw - All without garbage`() {
        val clean = PersistedQuizGroups(emptySet(), hasGarbage = false)

        assertEquals(clean, decodeQuizGroupIds(null))
        assertEquals(clean, decodeQuizGroupIds(""))
    }

    @Test
    fun `single id - one group`() {
        assertEquals(PersistedQuizGroups(setOf(12L), hasGarbage = false), decodeQuizGroupIds("12"))
    }

    @Test
    fun `garbage token dropped and flagged`() {
        assertEquals(PersistedQuizGroups(setOf(12L), hasGarbage = true), decodeQuizGroupIds("12,abc"))
    }

    @Test
    fun `whole garbage - All flagged`() {
        assertEquals(PersistedQuizGroups(emptySet(), hasGarbage = true), decodeQuizGroupIds("abc"))
    }

    @Test
    fun `unsorted or duplicated - flagged for canonical rewrite`() {
        assertEquals(PersistedQuizGroups(setOf(3L, 15L), hasGarbage = true), decodeQuizGroupIds("15,3,3"))
    }
}
