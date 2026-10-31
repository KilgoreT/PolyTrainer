package me.apomazkin.candidates

import org.junit.Assert.assertEquals
import org.junit.Test

/** Русские формы счёта на кнопке и в вопросе (AWP-13). */
class CountPhrasesTest {

    @Test
    fun `AWP-13_1 three lexemes in two words`() {
        assertEquals("3 значения в 2 слова", countPhrase(3, 2))
    }

    @Test
    fun `AWP-13_5 two lexemes in one word`() {
        assertEquals("2 значения в 1 слово", countPhrase(2, 1))
    }

    @Test
    fun `fifteen hundred lexemes in three hundred words`() {
        assertEquals("1500 значений в 300 слов", countPhrase(1500, 300))
    }

    @Test
    fun `eleven and twenty-one take the many and the one forms`() {
        assertEquals("11 значений в 21 слово", countPhrase(11, 21))
        assertEquals("1 значение в 1 слово", countPhrase(1, 1))
    }
}
