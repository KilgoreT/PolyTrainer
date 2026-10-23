package me.apomazkin.lexeme

import org.junit.Assert.assertEquals
import org.junit.Test

/** Исход запроса одного слова из исхода запроса (WCL-04.7, WCL-11). */
class LookupTest {

    private fun lexeme(key: String) = LookupLexeme(
        key = key,
        partOfSpeechKey = "noun",
        source = "ruwiktionary",
        translations = listOf("собака"),
        gloss = null,
        caption = null,
        notes = emptyList(),
        examples = emptyList(),
        grammar = emptyMap(),
        level = null,
        synonyms = emptyList(),
        antonyms = emptyList(),
        componentMatches = listOf(ComponentMatch(componentId = 1L, optionId = null, field = "translations")),
    )

    private fun word(
        text: String,
        status: LookupWordStatus,
        vararg lemmas: LookupLemma,
    ) = LookupWord(text = text, status = status, lemmas = lemmas.toList(), remarks = emptyList())

    private fun words(vararg words: LookupWord) =
        LookupOutcome.Words(dictionaryId = 1L, words = words.toList(), recommendations = emptyList())

    @Test
    fun `WCL-04_8 a found word with lexemes is found with its recommendations`() {
        val perro = word("perro", LookupWordStatus.FOUND, LookupLemma("perro", null, emptyList(), listOf(lexeme("ruwiktionary/1.noun.1"))))
        val recommendation = ComponentRecommendation(name = "Род", template = ComponentTemplate.CHOICE, options = listOf("м.р.", "ж.р."))

        val outcome = wordLookupOf(words(perro).copy(recommendations = listOf(recommendation)))

        assertEquals(WordLookupOutcome.Found(perro, listOf(recommendation)), outcome)
    }

    @Test
    fun `WCL-11_1 WCL-04_7 not found and found without lexemes have no candidate lexemes`() {
        val notFound = word("qwrtzk", LookupWordStatus.NOT_FOUND)
        val emptyLemmas = word("bank", LookupWordStatus.FOUND)
        val lemmaWithoutLexemes = word("bank", LookupWordStatus.FOUND, LookupLemma("bank", null, emptyList(), emptyList()))

        assertEquals(WordLookupOutcome.NotFound, wordLookupOf(words(notFound)))
        assertEquals(WordLookupOutcome.NotFound, wordLookupOf(words(emptyLemmas)))
        assertEquals(WordLookupOutcome.NotFound, wordLookupOf(words(lemmaWithoutLexemes)))
        assertEquals(WordLookupOutcome.NotFound, wordLookupOf(LookupOutcome.NoWords))
    }

    @Test
    fun `WCL-11_7 an error word means the server failed on it`() {
        val error = word("perro", LookupWordStatus.ERROR)

        assertEquals(WordLookupOutcome.WordFailed, wordLookupOf(words(error)))
    }

    @Test
    fun `WCL-11_2 WCL-11_3 WCL-11_6 denials and unavailable pass through`() {
        assertEquals(
            WordLookupOutcome.Denied(LookupDenyReason.LLM_UNAVAILABLE),
            wordLookupOf(LookupOutcome.Denied(LookupDenyReason.LLM_UNAVAILABLE)),
        )
        assertEquals(
            WordLookupOutcome.Denied(LookupDenyReason.RATE_LIMITED),
            wordLookupOf(LookupOutcome.Denied(LookupDenyReason.RATE_LIMITED)),
        )
        assertEquals(WordLookupOutcome.Unavailable, wordLookupOf(LookupOutcome.Unavailable))
    }
}
