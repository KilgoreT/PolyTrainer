package me.apomazkin.wordstab.logic.photo

import me.apomazkin.textrecognition.RecognizedWord
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Ш5 «Токены перед отправкой»: WFT-17 (пустые и дубли), WFT-18 (потолок).
 * Адреса строк спеки в именах — `WFT-NN_k` (точка в имени метода JVM
 * запрещена). Нормализация на устройстве не проверяется: её нет (Ш8 на
 * сервере).
 */
class PrepareTokensTest {

    private fun word(text: String, confidence: Float = 1f, language: String? = "en") =
        RecognizedWord(text = text, confidence = confidence, language = language)

    @Test
    fun `WFT-17_1 duplicates by exact text are dropped keeping first appearance order`() {
        val input = listOf(word("the"), word("cat"), word("The"), word("dog"), word("cat"))

        assertEquals(
                listOf("the", "cat", "The", "dog"),
                prepareTokens(input).map { it.text },
        )
    }

    @Test
    fun `WFT-17_2 punctuation and case stay untouched`() {
        val input = listOf(word("\"Hello,"), word("world!"))

        assertEquals(input, prepareTokens(input))
    }

    @Test
    fun `WFT-17_3 token of whitespace only is dropped`() {
        val input = listOf(word("hello"), word("   "), word(""), word("world"))

        assertEquals(listOf("hello", "world"), prepareTokens(input).map { it.text })
    }

    @Test
    fun `WFT-17_4 duplicate keeps the place of the first and the best confidence`() {
        val input = listOf(word("the", 0.4f), word("cat", 0.8f), word("the", 0.9f))

        val result = prepareTokens(input)

        assertEquals(listOf("the", "cat"), result.map { it.text })
        assertEquals(0.9f, result.first().confidence)
    }

    @Test
    fun `WFT-17_5 token without language is kept with an empty language`() {
        val input = listOf(word("accorc", 0.4f, language = null))

        assertEquals(input, prepareTokens(input))
    }

    @Test
    fun `WFT-18_1 2001 tokens are cut to the first 2000`() {
        val input = (1..2001).map { word("w$it") }

        val result = prepareTokens(input)

        assertEquals(2000, result.size)
        assertEquals("w1", result.first().text)
        assertEquals("w2000", result.last().text)
    }

    @Test
    fun `WFT-18_2 exactly 2000 tokens are all sent`() {
        val input = (1..2000).map { word("w$it") }

        assertEquals(2000, prepareTokens(input).size)
    }
}
