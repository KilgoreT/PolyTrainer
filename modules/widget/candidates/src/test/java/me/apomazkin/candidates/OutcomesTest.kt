package me.apomazkin.candidates

import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.AddWordsBatchOutcome
import me.apomazkin.lexeme.ComponentRecommendation
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.FormOutcome
import me.apomazkin.lexeme.LexemesOutcome
import me.apomazkin.lexeme.LookupDenyReason
import me.apomazkin.lexeme.LookupLemma
import me.apomazkin.lexeme.LookupWord
import me.apomazkin.lexeme.LookupWordStatus
import me.apomazkin.lexeme.WordLookupOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

/** Тексты отказов сервера (WCL-11) и строки исходов для лога. */
class OutcomesTest {

    private val lemma = LookupLemma("run", null, emptyList(), emptyList())

    @Test
    fun `WCL-11_2 a denied access gives the text of the code`() {
        val unauthorized = errorResOf(LookupDenyReason.UNAUTHORIZED)
        val rateLimited = errorResOf(LookupDenyReason.RATE_LIMITED)
        val tooLarge = errorResOf(LookupDenyReason.REQUEST_TOO_LARGE)
        val language = errorResOf(LookupDenyReason.UNSUPPORTED_LANGUAGE)

        assertEquals(R.string.photo_words_error_no_access, unauthorized)
        assertEquals(R.string.photo_words_error_rate_limited, rateLimited)
        assertEquals(R.string.photo_words_error_too_many, tooLarge)
        assertEquals(R.string.photo_words_error_language, language)
    }

    @Test
    fun `WCL-11_2 a build without the key gives the same text as an unknown key`() {
        assertEquals(
            errorResOf(LookupDenyReason.UNAUTHORIZED),
            errorResOf(LookupDenyReason.NO_ACCESS_KEY),
        )
    }

    @Test
    fun `WCL-11_6 the server without the neural network answer gives the unavailable text`() {
        assertEquals(R.string.scan_server_unavailable, errorResOf(LookupDenyReason.LLM_UNAVAILABLE))
    }

    @Test
    fun `lexemes outcomes are described for the log`() {
        val word = LookupWord("run", LookupWordStatus.FOUND, listOf(lemma, lemma), emptyList())
        val recommendation = ComponentRecommendation("Род", ComponentTemplate.CHOICE, emptyList())

        assertEquals(
            "found lemmas=2 recommendations=1 existing=0",
            LexemesOutcome.Found(word, listOf(recommendation)).describe(),
        )
        assertEquals("not found", LexemesOutcome.NotFound.describe())
        assertEquals("no lexemes", LexemesOutcome.NoLexemes.describe())
        assertEquals(
            "denied=RATE_LIMITED",
            LexemesOutcome.Denied(LookupDenyReason.RATE_LIMITED).describe(),
        )
        assertEquals("unavailable", LexemesOutcome.Unavailable.describe())
        assertEquals("word failed", LexemesOutcome.WordFailed.describe())
    }

    @Test
    fun `word lookup outcomes are described for the log`() {
        val word = LookupWord("run", LookupWordStatus.FOUND, listOf(lemma), emptyList())

        assertEquals(
            "found lemmas=1 recommendations=0",
            WordLookupOutcome.Found(word, emptyList()).describe(),
        )
        assertEquals("not found", WordLookupOutcome.NotFound.describe())
        assertEquals("word missing", WordLookupOutcome.WordMissing.describe())
        assertEquals(
            "denied=UNAUTHORIZED",
            WordLookupOutcome.Denied(LookupDenyReason.UNAUTHORIZED).describe(),
        )
        assertEquals("unavailable", WordLookupOutcome.Unavailable.describe())
    }

    @Test
    fun `form and batch outcomes are described for the log`() {
        val added = AddWordsBatchOutcome.Added(wordIds = listOf(1, 2), lexemeIds = listOf(1, 2, 3))

        assertEquals("dictionary missing", FormOutcome.DictionaryMissing.describe())
        assertEquals("failed: boom", FormOutcome.Failed("boom").describe())
        assertEquals("added words=2 lexemes=3", added.describe())
        assertEquals(
            "rejected GroupMissing(groupId=5)",
            AddWordsBatchOutcome.Rejected.GroupMissing(5).describe(),
        )
    }
}
