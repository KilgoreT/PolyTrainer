package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_network_api.entity.CandidateLexeme
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.Lemma
import me.apomazkin.core_network_api.entity.LookupResult
import me.apomazkin.core_network_api.entity.WordResult
import me.apomazkin.core_network_api.entity.WordStatus
import me.apomazkin.textrecognition.RecognizedWord
import me.apomazkin.wordstab.deps.PhotoDenyReason
import me.apomazkin.wordstab.deps.PhotoLookupOutcome
import me.apomazkin.wordstab.deps.PhotoWordStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Граница «слой сети → экран» (IS529/IS532): запрос из токенов и языков
 * словаря (WFT-20), слова ответа со словарём снимка (AWP-01) и леммами с
 * лексемами-кандидатами (WFT-71, WFT-76), причины отказа (WFT-63).
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

    private fun lexeme(
        key: String,
        partOfSpeechKey: String? = "verb",
        translations: List<String> = emptyList(),
        gloss: String? = null,
        parent: Boolean = false,
        parentKey: String? = null,
    ) = CandidateLexeme(
        key = key,
        partOfSpeechKey = partOfSpeechKey,
        source = if (gloss == null) "ruwiktionary" else "enwiktionary",
        translations = translations,
        gloss = gloss,
        caption = null,
        notes = emptyList(),
        examples = emptyList(),
        parent = parent,
        parentKey = parentKey,
    )

    private fun success(vararg words: WordResult) = LookupResult.Success("Wiktionary, CC BY-SA 4.0", words.toList())

    private fun found(text: String, vararg lemmas: Lemma) = WordResult(text, WordStatus.FOUND, emptyList(), lemmas.toList())

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
    fun `AWP-01_1 WFT-49_1 outcome carries the dictionary of the photo and words in response order`() {
        val result = success(found("fuera", Lemma("fuera", null, listOf(lexeme("ru/f1", "adverb", listOf("снаружи"))))), found("corrían", Lemma("correr", "third-person plural imperfect indicative", listOf(lexeme("ru/c1", translations = listOf("бегать; бежать"))))))

        val outcome = result.toOutcome(dictionaryId = 1L) as PhotoLookupOutcome.Words

        assertEquals(1L, outcome.dictionaryId)
        assertEquals(listOf("fuera", "corrían"), outcome.words.map { it.text })
    }

    @Test
    fun `WFT-50_2 not found and source error words have the status and no lemmas`() {
        val result = success(WordResult("accorc", WordStatus.NOT_FOUND, emptyList()), WordResult("perro", WordStatus.ERROR, emptyList()))

        val words = (result.toOutcome(1L) as PhotoLookupOutcome.Words).words

        assertEquals(PhotoWordStatus.NOT_FOUND, words[0].status)
        assertEquals(PhotoWordStatus.ERROR, words[1].status)
        assertTrue(words.all { it.lemmas.isEmpty() })
    }

    @Test
    fun `WFT-76_2 WFT-71_1 lemmas keep text form note and every lexeme field`() {
        val parent = lexeme("en/g1", gloss = "To move swiftly.", parent = true)
        val child = CandidateLexeme(
            key = "en/v1",
            partOfSpeechKey = "verb",
            source = "enwiktionary",
            translations = listOf("бегать", "побегать"),
            gloss = "To move forward quickly upon two feet…",
            caption = null,
            notes = listOf("colloquial"),
            examples = listOf("Run, Sarah, run!"),
            parent = false,
            parentKey = "en/g1",
        )
        val result = success(found("running", Lemma("run", "present participle and gerund", listOf(parent, child)), Lemma("running", null, emptyList())))

        val word = (result.toOutcome(1L) as PhotoLookupOutcome.Words).words.single()

        assertEquals(listOf("run", "running"), word.lemmas.map { it.text })
        assertEquals("present participle and gerund", word.lemmas[0].formNote)
        assertNull(word.lemmas[1].formNote)
        assertTrue(word.lemmas[1].lexemes.isEmpty())
        val mapped = word.lemmas[0].lexemes[1]
        assertEquals("en/v1", mapped.key)
        assertEquals("verb", mapped.partOfSpeechKey)
        assertEquals("enwiktionary", mapped.source)
        assertEquals(listOf("бегать", "побегать"), mapped.translations)
        assertEquals("To move forward quickly upon two feet…", mapped.gloss)
        assertEquals(listOf("colloquial"), mapped.notes)
        assertEquals(listOf("Run, Sarah, run!"), mapped.examples)
        assertEquals("en/g1", mapped.parentKey)
        assertTrue(word.lemmas[0].lexemes[0].parent)
    }

    @Test
    fun `WFT-72_4 null part of speech key and unknown source stay as they are`() {
        val result = success(found("los", Lemma("el", "masculine plural", listOf(lexeme("x/1", partOfSpeechKey = null, gloss = "the").copy(source = "cambridge")))))

        val mapped = (result.toOutcome(1L) as PhotoLookupOutcome.Words).words.single().lemmas.single().lexemes.single()

        assertNull(mapped.partOfSpeechKey)
        assertEquals("cambridge", mapped.source)
    }

    @Test
    fun `WFT-63_1 WFT-63_2 WFT-63_3 WFT-63_4 WFT-19_1 deny reasons map one to one`() {
        assertEquals(PhotoLookupOutcome.Denied(PhotoDenyReason.UNAUTHORIZED), LookupResult.Denied(DenyReason.UNAUTHORIZED).toOutcome(1L))
        assertEquals(PhotoLookupOutcome.Denied(PhotoDenyReason.RATE_LIMITED), LookupResult.Denied(DenyReason.RATE_LIMITED).toOutcome(1L))
        assertEquals(PhotoLookupOutcome.Denied(PhotoDenyReason.REQUEST_TOO_LARGE), LookupResult.Denied(DenyReason.REQUEST_TOO_LARGE).toOutcome(1L))
        assertEquals(PhotoLookupOutcome.Denied(PhotoDenyReason.UNSUPPORTED_LANGUAGE), LookupResult.Denied(DenyReason.UNSUPPORTED_LANGUAGE).toOutcome(1L))
        assertEquals(PhotoLookupOutcome.Denied(PhotoDenyReason.NO_ACCESS_KEY), LookupResult.Denied(DenyReason.NO_ACCESS_KEY).toOutcome(1L))
    }

    @Test
    fun `WFT-23_1 unavailable maps to unavailable`() {
        assertEquals(PhotoLookupOutcome.Unavailable, LookupResult.Unavailable("offline").toOutcome(1L))
    }
}
