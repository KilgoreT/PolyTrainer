package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.LemmasRequestApiEntity
import me.apomazkin.core_network_api.entity.LemmasResult
import me.apomazkin.core_network_api.entity.TokenApiEntity
import me.apomazkin.lexeme.LemmasOutcome
import me.apomazkin.lexeme.LookupDenyReason
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.TextLemmas
import me.apomazkin.textrecognition.RecognizedWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

/**
 * Граница «слой сети → домен» для `lemmas/from-tokens` (Д57): запрос из токенов
 * и языков словаря без дерева (LFT-02), ответ — текст, перевод и четыре списка
 * (LFT-14), пустой ответ — «слов нет» (TFP-19), словарь снимка (AWP-01), отказы
 * и недоступность (LFL-23, LFL-63).
 */
class LemmasMappersTest {

    private val dictionary = DictionaryApiEntity(
        id = 1L,
        numericCode = 724,
        name = "Español",
        addDate = Date(0L),
        learningLanguage = "es",
        translationLanguage = "ru",
    )

    private fun token(text: String, confidence: Float = 0.9f, language: String? = "es") =
        LookupToken(text = text, confidence = confidence, language = language)

    private fun success(
        lexical: List<String> = emptyList(),
        proper: List<String> = emptyList(),
        function: List<String> = emptyList(),
        unparsed: List<String> = emptyList(),
        text: String = "Las nubes corrían por el cielo.",
        translation: String = "Облака бежали по небу.",
    ) = LemmasResult.Success(
        text = text,
        translation = translation,
        lexical = lexical,
        proper = proper,
        function = function,
        unparsed = unparsed,
    )

    // ===== Запрос =====

    @Test
    fun `LFT-02_1 a recognized word becomes a token with its text confidence and language`() {
        val recognized = RecognizedWord(text = "corrían", confidence = 0.93f, language = "es")

        assertEquals(token("corrían", confidence = 0.93f, language = "es"), recognized.toDomain())
        assertNull(RecognizedWord(text = "accorc", confidence = 0f, language = null).toDomain().language)
    }

    @Test
    fun `LFT-02_1 LFT-02_3 the request carries the twelve tokens in order with repeats confidence and language, the dictionary languages and nothing else`() {
        val texts = listOf(
            "Las", "nubes", "corrían", "por", "el", "cielo.",
            "El", "perro", "duerme", "en", "el", "jardín.",
        )
        val tokens = texts.mapIndexed { index, text -> token(text, confidence = 1f - index / 20f) }

        val request = lemmasRequestOf(dictionary, tokens)

        val expected = LemmasRequestApiEntity(
            learningLanguage = "es",
            translationLanguage = "ru",
            tokens = tokens.map {
                TokenApiEntity(text = it.text, confidence = it.confidence, language = "es")
            },
        )
        assertEquals(expected, request)
        val elAt = request.tokens.indices.filter { request.tokens[it].text == "el" }
        assertEquals(listOf(4, 10), elAt)
    }

    @Test
    fun `LFT-02_2 a token without language stays without language`() {
        val request = lemmasRequestOf(dictionary, listOf(token("cielo.", language = null)))

        assertEquals(listOf<String?>(null), request.tokens.map { it.language })
    }

    // ===== Ответ =====

    @Test
    fun `AWP-01_1 a success with lists is found with the id of the dictionary of the photo and the fields as they came`() {
        val result = success(
            lexical = listOf("nube", "correr", "cielo"),
            function = listOf("el", "por"),
        )

        val outcome = result.toDomain(dictionaryId = 1L)

        val expected = LemmasOutcome.Found(
            dictionaryId = 1L,
            lemmas = TextLemmas(
                text = "Las nubes corrían por el cielo.",
                translation = "Облака бежали по небу.",
                lexical = listOf("nube", "correr", "cielo"),
                proper = emptyList(),
                function = listOf("el", "por"),
                unparsed = emptyList(),
            ),
        )
        assertEquals(expected, outcome)
    }

    @Test
    fun `TFP-19_2 four empty lists are no words even when the text is not empty`() {
        assertEquals(LemmasOutcome.NoWords, success(text = "sh").toDomain(dictionaryId = 1L))
    }

    @Test
    fun `TFP-19_4 three empty lists and sh in unparsed are found with the unparsed list`() {
        val outcome = success(unparsed = listOf("sh"), text = "sh", translation = "")
            .toDomain(dictionaryId = 1L)

        outcome as LemmasOutcome.Found
        assertEquals(listOf("sh"), outcome.lemmas.unparsed)
        assertEquals(emptyList<String>(), outcome.lemmas.lexical)
    }

    @Test
    fun `LFL-23_1 a denial gives its reason and no answer gives unavailable`() {
        assertEquals(
            LemmasOutcome.Denied(LookupDenyReason.LLM_UNAVAILABLE),
            LemmasResult.Denied(DenyReason.LLM_UNAVAILABLE).toDomain(dictionaryId = 1L),
        )
        assertEquals(
            LemmasOutcome.Denied(LookupDenyReason.UNSUPPORTED_LANGUAGE),
            LemmasResult.Denied(DenyReason.UNSUPPORTED_LANGUAGE).toDomain(dictionaryId = 1L),
        )
        val unavailable = LemmasResult.Unavailable("offline").toDomain(dictionaryId = 1L)
        assertEquals(LemmasOutcome.Unavailable, unavailable)
    }
}
