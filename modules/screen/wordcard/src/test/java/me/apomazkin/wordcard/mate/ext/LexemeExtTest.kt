package me.apomazkin.wordcard.mate.ext

import me.apomazkin.wordcard.mate.lexeme
import me.apomazkin.wordcard.mate.loaded
import me.apomazkin.wordcard.mate.removeLexeme
import me.apomazkin.wordcard.mate.savedCv
import me.apomazkin.wordcard.mate.updateLexeme
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * WordCardState.updateLexeme / removeLexeme (generic, id-based) — мигрировано на
 * компонентную модель IS481.
 */
class LexemeExtTest {

    @Test
    fun `updateLexeme applies update to matching id only`() {
        val initial = loaded(
            lexemes = listOf(
                lexeme(1L, listOf(savedCv(5L, origin = "a"))),
                lexeme(2L, listOf(savedCv(6L, origin = "b"))),
            ),
        )
        val result = initial.updateLexeme(1L) { it.copy(components = listOf(savedCv(5L, origin = "A"))) }
        assertEquals("A", result.lexemeList.first { it.id == 1L }.components.single().origin)
        assertEquals("b", result.lexemeList.first { it.id == 2L }.components.single().origin)
    }

    @Test
    fun `updateLexeme with non-existent id leaves list intact`() {
        val initial = loaded(lexemes = listOf(lexeme(1L, listOf(savedCv(5L)))))
        val result = initial.updateLexeme(999L) { it.copy(components = emptyList()) }
        assertEquals(initial.lexemeList, result.lexemeList)
    }

    @Test
    fun `removeLexeme filters out matching id`() {
        val initial = loaded(lexemes = listOf(lexeme(1L, emptyList()), lexeme(2L, emptyList())))
        val result = initial.removeLexeme(1L)
        assertEquals(1, result.lexemeList.size)
        assertEquals(2L, result.lexemeList.first().id)
    }
}
