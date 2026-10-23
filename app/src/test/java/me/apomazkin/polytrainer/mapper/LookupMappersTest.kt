package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_network_api.entity.CandidateLexemeApiEntity
import me.apomazkin.core_network_api.entity.ComponentMatchApiEntity
import me.apomazkin.core_network_api.entity.ComponentNodeApiEntity
import me.apomazkin.core_network_api.entity.ComponentOptionNodeApiEntity
import me.apomazkin.core_network_api.entity.ComponentRecommendationApiEntity
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.LemmaApiEntity
import me.apomazkin.core_network_api.entity.LookupResult
import me.apomazkin.core_network_api.entity.TokenApiEntity
import me.apomazkin.core_network_api.entity.WordApiEntity
import me.apomazkin.core_network_api.entity.WordRemarkApiEntity
import me.apomazkin.core_network_api.entity.WordStatus
import me.apomazkin.lexeme.ComponentMatch
import me.apomazkin.lexeme.ComponentRecommendation
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.LookupDenyReason
import me.apomazkin.lexeme.LookupOutcome
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.LookupWordStatus
import me.apomazkin.lexeme.WordRemark
import me.apomazkin.textrecognition.RecognizedWord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Граница «слой сети → домен» (Д57): запрос из токенов, языков и дерева компонентов
 * словаря (WFT-20, WFT-78), ответ с его словами, леммами, лексемами-кандидатами,
 * сопоставлением, замечаниями и рекомендациями (WFT-50, WFT-71, WFT-76, WFT-87,
 * WFT-89, WFT-90), отказы и недоступность (WFT-23, WFT-63).
 */
class LookupMappersTest {

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

    private fun node(
        id: Long,
        key: String? = null,
        template: String = "text",
        options: List<ComponentOptionNodeApiEntity> = emptyList(),
        children: List<ComponentNodeApiEntity> = emptyList(),
    ) = ComponentNodeApiEntity(
        id = id,
        key = key,
        name = "component $id",
        template = template,
        core = key == "translation",
        multiple = false,
        options = options,
        children = children,
    )

    private fun lexeme(
        key: String,
        partOfSpeechKey: String? = "noun",
        matches: List<ComponentMatchApiEntity> = emptyList(),
    ) = CandidateLexemeApiEntity(
        key = key,
        partOfSpeechKey = partOfSpeechKey,
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
        componentMatches = matches,
    )

    private fun lemma(text: String, formNote: String? = null, vararg lexemes: CandidateLexemeApiEntity) =
        LemmaApiEntity(
            text = text,
            formNote = formNote,
            pronunciations = emptyList(),
            lexemes = lexemes.toList(),
        )

    private fun word(
        text: String,
        status: WordStatus = WordStatus.FOUND,
        lemmas: List<LemmaApiEntity> = emptyList(),
        remarks: List<WordRemarkApiEntity> = emptyList(),
    ) = WordApiEntity(text = text, status = status, lemmas = lemmas, remarks = remarks)

    private fun success(
        vararg words: WordApiEntity,
        recommendations: List<ComponentRecommendationApiEntity> = emptyList(),
    ) = LookupResult.Success(
        attribution = "Wiktionary, CC BY-SA 4.0",
        words = words.toList(),
        recommendations = recommendations,
    )

    // ===== Запрос =====

    @Test
    fun `WFT-20_1 request carries dictionary languages and tokens in order with confidence and language`() {
        val tokens = listOf(
            token("corrían", confidence = 0.97f),
            token("fuera,", confidence = 0.91f),
        )

        val request = lookupRequestOf(dictionary, tokens, components = emptyList())

        assertEquals("es", request.learningLanguage)
        assertEquals("ru", request.translationLanguage)
        assertEquals(listOf("corrían", "fuera,"), request.tokens.map { it.text })
        assertEquals(0.97f, request.tokens.first().confidence)
        assertEquals("es", request.tokens.first().language)
    }

    @Test
    fun `WFT-20_2 token without language stays without language`() {
        val tokens = listOf(token("accorc", confidence = 0.4f, language = null))

        val request = lookupRequestOf(dictionary, tokens, components = emptyList())

        assertNull(request.tokens.single().language)
    }

    @Test
    fun `WFT-20_3 WFP-25_1 repeated tokens stay in the request in the order of the photo`() {
        val tokens = listOf(
            "Las", "nubes", "corrían", "por", "el", "cielo.", "El", "perro", "duerme", "en", "el", "jardín.",
        ).map { token(it) }

        val request = lookupRequestOf(dictionary, tokens, components = emptyList())

        assertEquals(12, request.tokens.size)
        assertEquals(tokens.map { it.text }, request.tokens.map { it.text })
        assertEquals(listOf(4, 10), request.tokens.indices.filter { request.tokens[it].text == "el" })
    }

    @Test
    fun `WFP-25_3 the same token with two confidences goes twice, each with its own`() {
        val tokens = listOf(token("the", confidence = 0.4f), token("the", confidence = 0.9f))

        val request = lookupRequestOf(dictionary, tokens, components = emptyList())

        assertEquals(listOf(0.4f, 0.9f), request.tokens.map { it.confidence })
    }

    @Test
    fun `WCL-03_1 WCL-03_2 request of the card - one token with full confidence and the language of the dictionary`() {
        val english = dictionary.copy(learningLanguage = "en", translationLanguage = "ru")
        val tokens = listOf(LookupToken(text = "give up", confidence = 1f, language = "en"))

        val request = lookupRequestOf(english, tokens, components = emptyList())

        assertEquals(listOf(TokenApiEntity("give up", 1f, "en")), request.tokens)
        assertEquals("en", request.learningLanguage)
        assertEquals("ru", request.translationLanguage)
    }

    @Test
    fun `WFT-78_1 request carries the component tree of the dictionary as it is`() {
        val gender = node(id = 5, template = "choice")
        val tree = listOf(
            node(id = 1, key = "translation"),
            node(
                id = 2,
                key = "part_of_speech",
                template = "choice",
                options = listOf(
                    ComponentOptionNodeApiEntity(id = 21, key = "noun", label = "существительное", children = listOf(gender)),
                    ComponentOptionNodeApiEntity(id = 22, key = "verb", label = "глагол", children = emptyList()),
                ),
            ),
            node(id = 4),
        )

        val request = lookupRequestOf(dictionary, listOf(token("perro")), components = tree)

        assertEquals(tree, request.components)
        assertEquals(gender, request.components[1].options[0].children.single())
    }

    @Test
    fun `WFT-78_7 dictionary without enabled components gives an empty tree in the request`() {
        val request = lookupRequestOf(dictionary, listOf(token("perro")), components = emptyList())

        assertTrue(request.components.isEmpty())
    }

    @Test
    fun `WFT-20_2 recognized word becomes a token with its text confidence and language`() {
        val recognized = RecognizedWord(text = "corrían", confidence = 0.93f, language = "es")

        assertEquals(token("corrían", confidence = 0.93f, language = "es"), recognized.toDomain())
        assertNull(RecognizedWord(text = "accorc", confidence = 0f, language = null).toDomain().language)
    }

    // ===== Ответ =====

    @Test
    fun `AWP-01_1 WFT-49_1 outcome carries the dictionary of the photo and words in response order`() {
        val result = success(
            word("fuera", lemmas = listOf(lemma("fuera", null, lexeme("ru/f1", "adverb")))),
            word("corrían", lemmas = listOf(lemma("correr", "third-person plural imperfect indicative", lexeme("ru/c1", "verb")))),
        )

        val outcome = result.toDomain(dictionaryId = 1L) as LookupOutcome.Words

        assertEquals(1L, outcome.dictionaryId)
        assertEquals(listOf("fuera", "corrían"), outcome.words.map { it.text })
    }

    @Test
    fun `WFP-19_2 answer without words is no words`() {
        assertEquals(LookupOutcome.NoWords, success().toDomain(dictionaryId = 1L))
    }

    @Test
    fun `WFT-50_2 not found and source error words have the status and no lemmas`() {
        val result = success(
            word("accorc", status = WordStatus.NOT_FOUND),
            word("perro", status = WordStatus.ERROR),
        )

        val words = (result.toDomain(1L) as LookupOutcome.Words).words

        assertEquals(LookupWordStatus.NOT_FOUND, words[0].status)
        assertEquals(LookupWordStatus.ERROR, words[1].status)
        assertTrue(words.all { it.lemmas.isEmpty() })
    }

    @Test
    fun `WFT-76_2 lemmas keep text form note and transcriptions in order`() {
        val running = LemmaApiEntity(
            text = "run",
            formNote = "present participle and gerund",
            pronunciations = listOf("/rʌn/", "/ɹʌn/"),
            lexemes = listOf(lexeme("en/v1", "verb")),
        )
        val own = lemma("running", null)

        val word = (success(word("running", lemmas = listOf(running, own))).toDomain(1L) as LookupOutcome.Words).words.single()

        assertEquals(listOf("run", "running"), word.lemmas.map { it.text })
        assertEquals("present participle and gerund", word.lemmas[0].formNote)
        assertEquals(listOf("/rʌn/", "/ɹʌn/"), word.lemmas[0].pronunciations)
        assertNull(word.lemmas[1].formNote)
        assertTrue(word.lemmas[1].lexemes.isEmpty())
    }

    @Test
    fun `WFT-71_1 WFT-81 lexeme keeps every field of the entry`() {
        val full = CandidateLexemeApiEntity(
            key = "en/v1",
            partOfSpeechKey = "verb",
            source = "enwiktionary",
            translations = listOf("бегать", "побегать"),
            gloss = "To move forward quickly upon two feet…",
            caption = "colloquial",
            notes = listOf("informal"),
            examples = listOf("Run, Sarah, run!"),
            grammar = mapOf("tense" to "present"),
            level = "A1",
            synonyms = listOf("sprint"),
            antonyms = listOf("walk"),
            componentMatches = emptyList(),
        )
        val result = success(word("run", lemmas = listOf(lemma("run", null, full))))

        val mapped = (result.toDomain(1L) as LookupOutcome.Words).words.single().lemmas.single().lexemes.single()

        assertEquals("en/v1", mapped.key)
        assertEquals("verb", mapped.partOfSpeechKey)
        assertEquals("enwiktionary", mapped.source)
        assertEquals(listOf("бегать", "побегать"), mapped.translations)
        assertEquals("To move forward quickly upon two feet…", mapped.gloss)
        assertEquals("colloquial", mapped.caption)
        assertEquals(listOf("informal"), mapped.notes)
        assertEquals(listOf("Run, Sarah, run!"), mapped.examples)
        assertEquals(mapOf("tense" to "present"), mapped.grammar)
        assertEquals("A1", mapped.level)
        assertEquals(listOf("sprint"), mapped.synonyms)
        assertEquals(listOf("walk"), mapped.antonyms)
    }

    @Test
    fun `WFT-72_4 null part of speech key and unknown source stay as they are`() {
        val odd = lexeme("x/1", partOfSpeechKey = null).copy(source = "cambridge")
        val result = success(word("los", lemmas = listOf(lemma("el", "masculine plural", odd))))

        val mapped = (result.toDomain(1L) as LookupOutcome.Words).words.single().lemmas.single().lexemes.single()

        assertNull(mapped.partOfSpeechKey)
        assertEquals("cambridge", mapped.source)
    }

    @Test
    fun `WFT-87_1 component matches keep the component, the option and the field`() {
        val matches = listOf(
            ComponentMatchApiEntity(componentId = 1L, optionId = null, field = "translations"),
            ComponentMatchApiEntity(componentId = 2L, optionId = 21L, field = null),
        )
        val result = success(word("perro", lemmas = listOf(lemma("perro", null, lexeme("ru/p1", matches = matches)))))

        val mapped = (result.toDomain(1L) as LookupOutcome.Words).words.single().lemmas.single().lexemes.single()

        assertEquals(
            listOf(
                ComponentMatch(componentId = 1L, optionId = null, field = "translations"),
                ComponentMatch(componentId = 2L, optionId = 21L, field = null),
            ),
            mapped.componentMatches,
        )
    }

    @Test
    fun `WFT-90_8 AWP-30 remarks of the word - unclear component and no translation`() {
        val remarks = listOf(
            WordRemarkApiEntity(code = "component_unclear", componentId = 4L, lemma = null, lexemeKey = null),
            WordRemarkApiEntity(code = "no_translation", componentId = null, lemma = "estudiante", lexemeKey = "enwiktionary/2.adjective.1"),
        )
        val result = success(word("estudiante", remarks = remarks))

        val mapped = (result.toDomain(1L) as LookupOutcome.Words).words.single().remarks

        assertEquals(
            listOf(
                WordRemark.ComponentUnclear(componentId = 4L),
                WordRemark.NoTranslation(lemma = "estudiante", lexemeKey = "enwiktionary/2.adjective.1"),
            ),
            mapped,
        )
    }

    @Test
    fun `WFT-90_8 an unknown remark code or a remark without its fields is dropped, the answer stays`() {
        val remarks = listOf(
            WordRemarkApiEntity(code = "something_new", componentId = 4L, lemma = "a", lexemeKey = "b"),
            WordRemarkApiEntity(code = "component_unclear", componentId = null, lemma = null, lexemeKey = null),
            WordRemarkApiEntity(code = "no_translation", componentId = null, lemma = "estudiante", lexemeKey = null),
        )
        val result = success(word("estudiante", remarks = remarks))

        val words = (result.toDomain(1L) as LookupOutcome.Words).words

        assertEquals(1, words.size)
        assertTrue(words.single().remarks.isEmpty())
    }

    @Test
    fun `WFT-89_1 recommendation keeps name, template and options of a choice`() {
        val recommendations = listOf(
            ComponentRecommendationApiEntity(name = "Род", template = "choice", options = listOf("м.р.", "ж.р.")),
            ComponentRecommendationApiEntity(name = "Пример", template = "captioned_text", options = emptyList()),
        )
        val result = success(word("perro"), recommendations = recommendations)

        val mapped = (result.toDomain(1L) as LookupOutcome.Words).recommendations

        assertEquals(
            listOf(
                ComponentRecommendation("Род", ComponentTemplate.CHOICE, listOf("м.р.", "ж.р.")),
                ComponentRecommendation("Пример", ComponentTemplate.CAPTIONED_TEXT, emptyList()),
            ),
            mapped,
        )
    }

    @Test
    fun `WFT-89 a recommendation of a template the app does not know is dropped, the answer stays`() {
        val recommendations = listOf(
            ComponentRecommendationApiEntity(name = "Звук", template = "audio", options = emptyList()),
        )
        val result = success(word("perro"), recommendations = recommendations)

        val outcome = result.toDomain(1L) as LookupOutcome.Words

        assertEquals(listOf("perro"), outcome.words.map { it.text })
        assertTrue(outcome.recommendations.isEmpty())
    }

    @Test
    fun `WFT-63_1 WFT-63_2 WFT-63_3 WFT-63_4 WFT-63_5 WFT-19_1 deny reasons map one to one`() {
        val expected = mapOf(
            DenyReason.NO_ACCESS_KEY to LookupDenyReason.NO_ACCESS_KEY,
            DenyReason.UNAUTHORIZED to LookupDenyReason.UNAUTHORIZED,
            DenyReason.RATE_LIMITED to LookupDenyReason.RATE_LIMITED,
            DenyReason.REQUEST_TOO_LARGE to LookupDenyReason.REQUEST_TOO_LARGE,
            DenyReason.UNSUPPORTED_LANGUAGE to LookupDenyReason.UNSUPPORTED_LANGUAGE,
            DenyReason.LLM_UNAVAILABLE to LookupDenyReason.LLM_UNAVAILABLE,
        )

        DenyReason.entries.forEach { reason ->
            assertEquals(
                LookupOutcome.Denied(expected.getValue(reason)),
                LookupResult.Denied(reason).toDomain(1L),
            )
        }
        assertEquals(DenyReason.entries.toSet(), expected.keys)
    }

    @Test
    fun `WFT-23_1 unavailable maps to unavailable`() {
        assertEquals(LookupOutcome.Unavailable, LookupResult.Unavailable("offline").toDomain(1L))
    }
}
