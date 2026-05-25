package me.apomazkin.wordcard.mate.ext

import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.TextValueState
import me.apomazkin.wordcard.mate.WordCardState
import me.apomazkin.wordcard.mate.WordState
import me.apomazkin.wordcard.mate.removeLexeme
import me.apomazkin.wordcard.mate.updateLexeme
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class LexemeExtTest {

    private fun loaded(lexemes: List<LexemeState> = emptyList()): WordCardState =
        WordCardState(
            isLoading = false,
            wordState = WordState.Loaded(id = 1L, added = Date(0L), value = "w"),
            lexemeList = lexemes,
        )

    @Test
    fun `updateLexeme applies update to matching id`() {
        val initial = loaded(listOf(
            LexemeState(id = 1L, translation = TextValueState(origin = "a")),
            LexemeState(id = 2L, translation = TextValueState(origin = "b")),
        ))

        val result = initial.updateLexeme(1L) {
            it.copy(translation = it.translation?.copy(origin = "A"))
        }

        assertEquals("A", result.lexemeList.first { it.id == 1L }.translation?.origin)
        assertEquals("b", result.lexemeList.first { it.id == 2L }.translation?.origin)
    }

    @Test
    fun `updateLexeme with non-existent id leaves list intact`() {
        val initial = loaded(listOf(LexemeState(id = 1L, translation = TextValueState(origin = "a"))))

        val result = initial.updateLexeme(999L) { it.copy(translation = null) }

        assertEquals(initial.lexemeList, result.lexemeList)
    }

    @Test
    fun `removeLexeme filters out matching id`() {
        val initial = loaded(listOf(
            LexemeState(id = 1L),
            LexemeState(id = 2L),
        ))

        val result = initial.removeLexeme(1L)

        assertEquals(1, result.lexemeList.size)
        assertEquals(2L, result.lexemeList.first().id)
    }
}
