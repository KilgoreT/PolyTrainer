package me.apomazkin.wordstab.logic.photo

import org.junit.Assert.assertEquals
import org.junit.Test

/** Русские формы счёта на кнопке, в вопросе и сообщении (AWP-13, AWP-23). */
class PhotoTextsTest {

    @Test
    fun `AWP-13_1 three lexemes in two words`() {
        assertEquals("3 значения в 2 слова", countPhrase(3, 2))
    }

    @Test
    fun `AWP-13_5 two lexemes in one word`() {
        assertEquals("2 значения в 1 слово", countPhrase(2, 1))
    }

    @Test
    fun `AWP-04_6 fifteen hundred lexemes in three hundred words`() {
        assertEquals("1500 значений в 300 слов", countPhrase(1500, 300))
    }

    @Test
    fun `AWP-23_1 added message`() {
        assertEquals("Добавлено 3 значения в 2 слова", addedMessage(3, 2))
        assertEquals("Добавлено 11 значений в 21 слово", addedMessage(11, 21))
        assertEquals("Добавлено 1 значение в 1 слово", addedMessage(1, 1))
    }
}
