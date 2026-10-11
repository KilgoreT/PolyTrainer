package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_network_api.entity.Candidate
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.LookupResult
import me.apomazkin.core_network_api.entity.Sense
import me.apomazkin.core_network_api.entity.WordResult
import me.apomazkin.core_network_api.entity.WordStatus
import me.apomazkin.textrecognition.RecognizedWord
import me.apomazkin.wordstab.deps.PhotoDenyReason
import me.apomazkin.wordstab.deps.PhotoLookupOutcome
import me.apomazkin.wordstab.deps.PhotoWordStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

/**
 * Граница «слой сети → экран» (IS529): запрос из токенов и языков словаря
 * (WFT-20), слова ответа с первым кандидатом (WFT-56, Д4), причины отказа
 * (WFT-63). Адреса строк спеки в именах — `WFT-NN_k`.
 */
class PhotoLookupMapperTest {

    private val dictionary = DictionaryApiEntity(
        id = 1L,
        numericCode = 724,
        name = "Español",
        addDate = Date(0L),
        learningLanguage = "es",
        translationLanguage = "ru",
    )

    private fun candidate(
        lemma: String,
        partOfSpeech: String,
        formNote: String? = null,
        gloss: String? = "gloss",
        meanings: List<String> = emptyList(),
    ) = Candidate(
        lemma = lemma,
        partOfSpeech = partOfSpeech,
        formNote = formNote,
        senses = listOfNotNull(gloss?.let { Sense(it, emptyList()) }),
        meanings = meanings,
    )

    @Test
    fun `WFT-20_1 request carries dictionary languages and tokens in order with confidence and language`() {
        val tokens = listOf(
            RecognizedWord(text = "corrían", confidence = 0.97f, language = "es"),
            RecognizedWord(text = "fuera,", confidence = 0.91f, language = "es"),
        )

        val request = lookupRequestOf(dictionary, tokens)

        assertEquals("es", request.learningLanguage)
        assertEquals("ru", request.translationLanguage)
        assertEquals(listOf("corrían", "fuera,"), request.tokens.map { it.text })
        assertEquals(0.97f, request.tokens.first().confidence)
        assertEquals("es", request.tokens.first().language)
    }

    @Test
    fun `WFT-20_2 token without language stays without language`() {
        val request = lookupRequestOf(dictionary, listOf(RecognizedWord(text = "accorc", confidence = 0.4f, language = null)))

        assertNull(request.tokens.single().language)
    }

    @Test
    fun `WFT-56_1 words keep response order`() {
        val result = LookupResult.Success(
            attribution = "Wiktionary, CC BY-SA 4.0",
            words = listOf(
                WordResult("fuera", WordStatus.FOUND, listOf(candidate("fuera", "adverb"))),
                WordResult("corrían", WordStatus.FOUND, listOf(candidate("correr", "verb"))),
            ),
        )

        val outcome = result.toOutcome() as PhotoLookupOutcome.Words

        assertEquals(listOf("fuera", "corrían"), outcome.words.map { it.text })
    }

    @Test
    fun `WFT-56_3 not found word has the status and no lemma`() {
        val result = LookupResult.Success("Wiktionary, CC BY-SA 4.0", listOf(WordResult("accorc", WordStatus.NOT_FOUND, emptyList())))

        val word = (result.toOutcome() as PhotoLookupOutcome.Words).words.single()

        assertEquals(PhotoWordStatus.NOT_FOUND, word.status)
        assertNull(word.lemma)
        assertNull(word.partOfSpeech)
    }

    @Test
    fun `WFT-56_4 source error word has the status and no lemma`() {
        val result = LookupResult.Success("Wiktionary, CC BY-SA 4.0", listOf(WordResult("perro", WordStatus.ERROR, emptyList())))

        val word = (result.toOutcome() as PhotoLookupOutcome.Words).words.single()

        assertEquals(PhotoWordStatus.ERROR, word.status)
        assertNull(word.lemma)
    }

    @Test
    fun `WFT-56_7 found word carries lemma and part of speech of the first candidate`() {
        val result = LookupResult.Success(
            "Wiktionary, CC BY-SA 4.0",
            listOf(WordResult("corrían", WordStatus.FOUND, listOf(candidate("correr", "verb", "third-person plural imperfect indicative")))),
        )

        val word = (result.toOutcome() as PhotoLookupOutcome.Words).words.single()

        assertEquals(PhotoWordStatus.FOUND, word.status)
        assertEquals("correr", word.lemma)
        assertEquals("verb", word.partOfSpeech)
    }

    @Test
    fun `WFT-56_9 found word carries the first gloss of the first candidate and none when the candidate has no senses`() {
        val withSenses = LookupResult.Success(
            "Wiktionary, CC BY-SA 4.0",
            listOf(
                WordResult(
                    "corrían",
                    WordStatus.FOUND,
                    listOf(
                        Candidate("correr", "verb", null, listOf(Sense("to run, to jog", emptyList()), Sense("to flow", emptyList())), emptyList()),
                        Candidate("correr", "noun", null, listOf(Sense("a run", emptyList())), emptyList()),
                    ),
                ),
            ),
        )
        val withoutSenses = LookupResult.Success(
            "Wiktionary, CC BY-SA 4.0",
            listOf(WordResult("rara", WordStatus.FOUND, listOf(candidate("raro", "adjective", gloss = null)))),
        )

        val corrian = (withSenses.toOutcome() as PhotoLookupOutcome.Words).words.single()
        val rara = (withoutSenses.toOutcome() as PhotoLookupOutcome.Words).words.single()

        assertEquals("to run, to jog", corrian.gloss)
        assertNull(rara.gloss)
        assertEquals("raro", rara.lemma)
    }

    @Test
    fun `WFT-56_10 found word carries the first meaning in the translation language and none when the server has none`() {
        val withMeanings = LookupResult.Success(
            "Wiktionary, CC BY-SA 4.0",
            listOf(WordResult("corrían", WordStatus.FOUND, listOf(candidate("correr", "verb", meanings = listOf("бегать; бежать", "мчаться, нестись"))))),
        )
        val withoutMeanings = LookupResult.Success(
            "Wiktionary, CC BY-SA 4.0",
            listOf(WordResult("moreno", WordStatus.FOUND, listOf(candidate("moreno", "adjective")))),
        )

        val corrian = (withMeanings.toOutcome() as PhotoLookupOutcome.Words).words.single()
        val moreno = (withoutMeanings.toOutcome() as PhotoLookupOutcome.Words).words.single()

        assertEquals("бегать; бежать", corrian.meaning)
        assertNull(moreno.meaning)
        assertEquals("gloss", moreno.gloss)
    }

    @Test
    fun `WFT-56_8 several candidates give only the first one in article order`() {
        val result = LookupResult.Success(
            "Wiktionary, CC BY-SA 4.0",
            listOf(
                WordResult(
                    "fuera",
                    WordStatus.FOUND,
                    listOf(candidate("fuera", "adverb"), candidate("fuera", "interjection"), candidate("ser", "verb"), candidate("ir", "verb")),
                ),
            ),
        )

        val word = (result.toOutcome() as PhotoLookupOutcome.Words).words.single()

        assertEquals("fuera", word.lemma)
        assertEquals("adverb", word.partOfSpeech)
    }

    @Test
    fun `WFT-63_1 WFT-63_2 WFT-63_3 WFT-63_4 WFT-19_1 deny reasons map one to one`() {
        assertEquals(PhotoLookupOutcome.Denied(PhotoDenyReason.UNAUTHORIZED), LookupResult.Denied(DenyReason.UNAUTHORIZED).toOutcome())
        assertEquals(PhotoLookupOutcome.Denied(PhotoDenyReason.RATE_LIMITED), LookupResult.Denied(DenyReason.RATE_LIMITED).toOutcome())
        assertEquals(PhotoLookupOutcome.Denied(PhotoDenyReason.REQUEST_TOO_LARGE), LookupResult.Denied(DenyReason.REQUEST_TOO_LARGE).toOutcome())
        assertEquals(PhotoLookupOutcome.Denied(PhotoDenyReason.UNSUPPORTED_LANGUAGE), LookupResult.Denied(DenyReason.UNSUPPORTED_LANGUAGE).toOutcome())
        assertEquals(PhotoLookupOutcome.Denied(PhotoDenyReason.NO_ACCESS_KEY), LookupResult.Denied(DenyReason.NO_ACCESS_KEY).toOutcome())
    }

    @Test
    fun `WFT-23_1 unavailable maps to unavailable`() {
        assertEquals(PhotoLookupOutcome.Unavailable, LookupResult.Unavailable("offline").toOutcome())
    }
}
