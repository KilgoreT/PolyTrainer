package me.apomazkin.wordcard.mate.ext

import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.savedCv
import me.apomazkin.wordcard.mate.pristineCv
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * §2.6 LexemeState.addedNonMultipleTypeIds — computed: typeId всех НЕ-multi компонентов
 * (для скрытия их chip'ов в ChipsRow).
 */
class LexemeStateTest {

    @Test
    fun `addedNonMultipleTypeIds excludes multi`() {
        val l = LexemeState(
            id = 1L,
            components = listOf(savedCv(5L, typeId = 1L, isMultiple = false), savedCv(6L, typeId = 2L, isMultiple = true)),
        )
        assertEquals(setOf(ComponentTypeId(1L)), l.addedNonMultipleTypeIds)
    }

    @Test
    fun `addedNonMultipleTypeIds empty when all multi`() {
        val l = LexemeState(1L, listOf(savedCv(5L, typeId = 1L, isMultiple = true)))
        assertEquals(emptySet<ComponentTypeId>(), l.addedNonMultipleTypeIds)
    }

    @Test
    fun `addedNonMultipleTypeIds dedupes same typeId`() {
        val l = LexemeState(
            1L,
            listOf(savedCv(5L, typeId = 1L, isMultiple = false), savedCv(6L, typeId = 1L, isMultiple = false)),
        )
        assertEquals(setOf(ComponentTypeId(1L)), l.addedNonMultipleTypeIds)
    }

    @Test
    fun `addedNonMultipleTypeIds includes pristine non-multi`() {
        val l = LexemeState(1L, listOf(pristineCv(10L, typeId = 7L, isMultiple = false)))
        assertEquals(setOf(ComponentTypeId(7L)), l.addedNonMultipleTypeIds)
    }
}
