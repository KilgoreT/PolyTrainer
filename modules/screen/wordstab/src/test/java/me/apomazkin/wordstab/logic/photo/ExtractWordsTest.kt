package me.apomazkin.wordstab.logic.photo

import me.apomazkin.textrecognition.RecognizedWord
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 1. пунктуация и кавычки по краям снимаются, в т. ч. британские ‘…’
 * 2. апостроф (оба вида) и дефис внутри остаются
 * 3. апостроф и дефис по краям снимаются (перенос, притяжательный, 'tis)
 * 4. точка внутри остаётся (e.g.)
 * 5. диакритика латиницы — буквы (café, niño)
 * 6. числа и токены без букв выбрасываются
 * 7. одиночные буквы выбрасываются
 * 8. регистр приводится к нижнему
 * 9. дубли схлопываются, порядок — первое появление
 * 10. пустой вход — пустой список
 * 11. порог уверенности отсеивает слабые слова
 * 12. фильтр языка пропускает слово без языка
 */
class ExtractWordsTest {

    private fun word(text: String, confidence: Float = 1f, language: String? = "en") =
        RecognizedWord(text = text, confidence = confidence, language = language)

    @Test
    fun `should strip punctuation and quotes from edges`() {
        val input = listOf(word("\"Hello,"), word("world!"), word("(end)."), word("‘Hello,’"))
        assertEquals(listOf("hello", "world", "end"), extractWords(input))
    }

    @Test
    fun `should keep apostrophe and hyphen inside word`() {
        val input = listOf(word("don't"), word("isn’t"), word("well-known"))
        assertEquals(listOf("don't", "isn’t", "well-known"), extractWords(input))
    }

    @Test
    fun `should strip apostrophe and hyphen from edges`() {
        val input = listOf(word("well-"), word("dogs'"), word("'tis"), word("-"))
        assertEquals(listOf("well", "dogs", "tis"), extractWords(input))
    }

    @Test
    fun `should keep dot inside word`() {
        assertEquals(listOf("e.g"), extractWords(listOf(word("e.g."))))
    }

    @Test
    fun `should treat latin diacritics as letters`() {
        val input = listOf(word("Café,"), word("niño"))
        assertEquals(listOf("café", "niño"), extractWords(input))
    }

    @Test
    fun `should drop tokens without letters`() {
        val input = listOf(word("1984"), word("--"), word("42nd"), word("..."))
        assertEquals(listOf("42nd"), extractWords(input))
    }

    @Test
    fun `should drop single letters`() {
        assertEquals(listOf("an"), extractWords(listOf(word("a"), word("I"), word("an"))))
    }

    @Test
    fun `should lowercase words`() {
        assertEquals(listOf("the", "london"), extractWords(listOf(word("The"), word("LONDON"))))
    }

    @Test
    fun `should collapse duplicates keeping first appearance order`() {
        val input = listOf(
            word("the"),
            word("cat"),
            word("The"),
            word("dog"),
            word("cat"),
        )
        assertEquals(listOf("the", "cat", "dog"), extractWords(input))
    }

    @Test
    fun `should return empty list for empty input`() {
        assertEquals(emptyList<String>(), extractWords(emptyList()))
    }

    @Test
    fun `should drop words below confidence threshold when enabled`() {
        val result = extractWords(
            words = listOf(word("sure", confidence = 0.9f), word("blurry", confidence = 0.2f)),
            rules = WordRules(minConfidence = 0.5f),
        )
        assertEquals(listOf("sure"), result)
    }

    @Test
    fun `should keep dictionary language and unknown language when filter enabled`() {
        val result = extractWords(
            words = listOf(
                word("perro", language = "es"),
                word("dog", language = "en"),
                word("gato", language = null),
            ),
            rules = WordRules(language = "es"),
        )
        assertEquals(listOf("perro", "gato"), result)
    }
}
