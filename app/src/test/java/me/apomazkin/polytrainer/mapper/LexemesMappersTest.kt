package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_network_api.entity.CandidateLexemeApiEntity
import me.apomazkin.core_network_api.entity.ComponentMatchApiEntity
import me.apomazkin.core_network_api.entity.ComponentNodeApiEntity
import me.apomazkin.core_network_api.entity.ComponentOptionNodeApiEntity
import me.apomazkin.core_network_api.entity.ComponentRecommendationApiEntity
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.LemmaApiEntity
import me.apomazkin.core_network_api.entity.LemmaContextApiEntity
import me.apomazkin.core_network_api.entity.LexemesResult
import me.apomazkin.core_network_api.entity.WordApiEntity
import me.apomazkin.core_network_api.entity.WordRemarkApiEntity
import me.apomazkin.core_network_api.entity.WordStatus
import me.apomazkin.lexeme.ComponentMatch
import me.apomazkin.lexeme.ComponentRecommendation
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.LemmaContext
import me.apomazkin.lexeme.LexemesOutcome
import me.apomazkin.lexeme.LookupDenyReason
import me.apomazkin.lexeme.LookupWordStatus
import me.apomazkin.lexeme.WordRemark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Граница «слой сети → домен» операции `lexemes/from-lemma` (Д57, Д114):
 * запрос леммой с контекстом снимка или без него и деревом компонентов
 * (LFL-103, LFL-104, LFL-78), исход по статусу слова и лексемам (LFL-50,
 * LFL-97, LFL-113), отказы и недоступность (LFL-23, LFL-63).
 */
class LexemesMappersTest {

    private val dictionary = DictionaryApiEntity(
        id = 1L,
        numericCode = 724,
        name = "Español",
        addDate = Date(0L),
        learningLanguage = "es",
        translationLanguage = "ru",
    )

    private val context = LemmaContext(
        text = "Las nubes corrían por el cielo.",
        translation = "Облака бежали по небу.",
    )

    private fun node(
        id: Long,
        key: String? = null,
        template: String = "text",
        options: List<ComponentOptionNodeApiEntity> = emptyList(),
    ) = ComponentNodeApiEntity(
        id = id,
        key = key,
        name = "component $id",
        template = template,
        core = key == "translation",
        multiple = false,
        options = options,
        children = emptyList(),
    )

    private fun lexeme(
        key: String,
        inContext: Boolean = false,
        matches: List<ComponentMatchApiEntity> = emptyList(),
    ) = CandidateLexemeApiEntity(
        key = key,
        partOfSpeechKey = "verb",
        source = "ruwiktionary",
        translations = listOf("бегать; бежать"),
        gloss = null,
        caption = null,
        notes = emptyList(),
        examples = emptyList(),
        grammar = emptyMap(),
        level = null,
        synonyms = emptyList(),
        antonyms = emptyList(),
        componentMatches = matches,
        inContext = inContext,
    )

    private fun lemma(text: String, vararg lexemes: CandidateLexemeApiEntity) = LemmaApiEntity(
        text = text,
        formNote = null,
        pronunciations = emptyList(),
        lexemes = lexemes.toList(),
    )

    private fun word(
        text: String,
        status: WordStatus = WordStatus.FOUND,
        lemmas: List<LemmaApiEntity> = emptyList(),
        remarks: List<WordRemarkApiEntity> = emptyList(),
    ) = WordApiEntity(text = text, status = status, lemmas = lemmas, remarks = remarks)

    private fun found(result: LexemesResult.Success) = (result.toDomain() as LexemesOutcome.Found).word

    private fun success(
        word: WordApiEntity,
        recommendations: List<ComponentRecommendationApiEntity> = emptyList(),
    ) = LexemesResult.Success(
        attribution = "Wiktionary, CC BY-SA 4.0",
        word = word,
        recommendations = recommendations,
    )

    // ===== Запрос =====

    @Test
    fun `LFL-103_1 request of the tap carries the dictionary languages the lemma the text with its translation and the tree`() {
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
        )

        val request = lexemesRequestOf(dictionary, lemma = "correr", context = context, components = tree)

        assertEquals("es", request.learningLanguage)
        assertEquals("ru", request.translationLanguage)
        assertEquals("correr", request.lemma)
        assertEquals(LemmaContextApiEntity("Las nubes corrían por el cielo.", "Облака бежали по небу."), request.context)
        assertEquals(tree, request.components)
    }

    @Test
    fun `LFL-104_1 LFL-103_2 request of the card has no context and the lemma goes as given`() {
        val card = lexemesRequestOf(dictionary, lemma = "Perro", context = null, components = emptyList())
        val unparsed = lexemesRequestOf(dictionary, lemma = "destibrg", context = context, components = emptyList())

        assertNull(card.context)
        assertEquals("Perro", card.lemma)
        assertEquals("destibrg", unparsed.lemma)
        assertEquals("Las nubes corrían por el cielo.", unparsed.context?.text)
        assertTrue(card.components.isEmpty())
    }

    // ===== Исход =====

    @Test
    fun `LFL-113_1 found word reaches the domain with its lexemes in order and the in context mark`() {
        val first = lexeme("ruwiktionary/1.verb.1", inContext = true, matches = listOf(ComponentMatchApiEntity(1L, null, "translations")))
        val second = lexeme("ruwiktionary/1.verb.2")
        val recommendation = ComponentRecommendationApiEntity(name = "Род", template = "choice", options = listOf("м.р.", "ж.р."))
        val result = success(word("correr", lemmas = listOf(lemma("correr", first, second))), recommendations = listOf(recommendation))

        val outcome = result.toDomain() as LexemesOutcome.Found

        assertEquals("correr", outcome.word.text)
        assertEquals(LookupWordStatus.FOUND, outcome.word.status)
        val lexemes = outcome.word.lemmas.single().lexemes
        assertEquals(listOf("ruwiktionary/1.verb.1", "ruwiktionary/1.verb.2"), lexemes.map { it.key })
        assertTrue(lexemes[0].inContext)
        assertFalse(lexemes[1].inContext)
        assertEquals("translations", lexemes[0].componentMatches.single().field)
        assertEquals(listOf(ComponentRecommendation("Род", ComponentTemplate.CHOICE, listOf("м.р.", "ж.р."))), outcome.recommendations)
        assertTrue(outcome.existing.isEmpty())
    }

    @Test
    fun `LFL-50_2 not found word is not found`() {
        assertEquals(LexemesOutcome.NotFound, success(word("xyzzyq", status = WordStatus.NOT_FOUND)).toDomain())
    }

    @Test
    fun `LFL-97_2 error word is word failed`() {
        assertEquals(LexemesOutcome.WordFailed, success(word("perro", status = WordStatus.ERROR)).toDomain())
    }

    @Test
    fun `LFL-50_6 AWP-55_7 found word without lexemes in any lemma is no lexemes`() {
        val emptyLemmas = success(word("perro"))
        val lemmaWithoutLexemes = success(word("perro", lemmas = listOf(lemma("perro"))))

        assertEquals(LexemesOutcome.NoLexemes, emptyLemmas.toDomain())
        assertEquals(LexemesOutcome.NoLexemes, lemmaWithoutLexemes.toDomain())
    }

    @Test
    fun `WCL-11_2 WCL-11_6 ACC-19_1 deny reasons map one to one`() {
        val expected = mapOf(
            DenyReason.NO_ACCESS_KEY to LookupDenyReason.NO_ACCESS_KEY,
            DenyReason.UNAUTHORIZED to LookupDenyReason.UNAUTHORIZED,
            DenyReason.RATE_LIMITED to LookupDenyReason.RATE_LIMITED,
            DenyReason.REQUEST_TOO_LARGE to LookupDenyReason.REQUEST_TOO_LARGE,
            DenyReason.UNSUPPORTED_LANGUAGE to LookupDenyReason.UNSUPPORTED_LANGUAGE,
            DenyReason.LLM_UNAVAILABLE to LookupDenyReason.LLM_UNAVAILABLE,
        )

        DenyReason.entries.forEach { reason ->
            assertEquals(LexemesOutcome.Denied(expected.getValue(reason)), LexemesResult.Denied(reason).toDomain())
        }
        assertEquals(DenyReason.entries.toSet(), expected.keys)
    }

    @Test
    fun `LFL-23_1 unavailable maps to unavailable`() {
        assertEquals(LexemesOutcome.Unavailable, LexemesResult.Unavailable("offline").toDomain())
    }

    // ===== Слово ответа: леммы, лексемы, замечания, рекомендации =====

    @Test
    fun `LFL-76_2 lemmas keep text form note and transcriptions in order`() {
        val run = LemmaApiEntity(
            text = "run",
            formNote = "present participle and gerund",
            pronunciations = listOf("/rʌn/", "/ɹʌn/"),
            lexemes = listOf(lexeme("en/v1")),
        )
        val own = lemma("running", lexeme("en/v2"))

        val word = found(success(word("running", lemmas = listOf(run, own))))

        assertEquals(listOf("run", "running"), word.lemmas.map { it.text })
        assertEquals("present participle and gerund", word.lemmas[0].formNote)
        assertEquals(listOf("/rʌn/", "/ɹʌn/"), word.lemmas[0].pronunciations)
        assertNull(word.lemmas[1].formNote)
    }

    @Test
    fun `LFL-71_1 LFL-81_1 lexeme keeps every field of the entry`() {
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
            inContext = false,
        )

        val mapped = found(success(word("run", lemmas = listOf(lemma("run", full))))).lemmas.single().lexemes.single()

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
        assertFalse(mapped.inContext)
    }

    @Test
    fun `LFL-72_4 null part of speech key and unknown source stay as they are`() {
        val odd = lexeme("x/1").copy(partOfSpeechKey = null, source = "cambridge")

        val mapped = found(success(word("los", lemmas = listOf(lemma("el", odd))))).lemmas.single().lexemes.single()

        assertNull(mapped.partOfSpeechKey)
        assertEquals("cambridge", mapped.source)
    }

    @Test
    fun `LFL-87_1 component matches keep the component, the option and the field`() {
        val matches = listOf(
            ComponentMatchApiEntity(componentId = 1L, optionId = null, field = "translations"),
            ComponentMatchApiEntity(componentId = 2L, optionId = 21L, field = null),
        )

        val mapped = found(success(word("perro", lemmas = listOf(lemma("perro", lexeme("ru/p1", matches = matches))))))
            .lemmas
            .single()
            .lexemes
            .single()

        assertEquals(
            listOf(
                ComponentMatch(componentId = 1L, optionId = null, field = "translations"),
                ComponentMatch(componentId = 2L, optionId = 21L, field = null),
            ),
            mapped.componentMatches,
        )
    }

    @Test
    fun `LFL-90_8 AWP-30_15 remarks of the word - unclear component and no translation`() {
        val remarks = listOf(
            WordRemarkApiEntity(code = "component_unclear", componentId = 4L, lemma = null, lexemeKey = null),
            WordRemarkApiEntity(code = "no_translation", componentId = null, lemma = "estudiante", lexemeKey = "enwiktionary/2.adjective.1"),
        )

        val mapped = found(success(word("estudiante", lemmas = listOf(lemma("estudiante", lexeme("ru/e1"))), remarks = remarks))).remarks

        assertEquals(
            listOf(
                WordRemark.ComponentUnclear(componentId = 4L),
                WordRemark.NoTranslation(lemma = "estudiante", lexemeKey = "enwiktionary/2.adjective.1"),
            ),
            mapped,
        )
    }

    @Test
    fun `LFL-90_8 an unknown remark code or a remark without its fields is dropped, the answer stays`() {
        val remarks = listOf(
            WordRemarkApiEntity(code = "something_new", componentId = 4L, lemma = "a", lexemeKey = "b"),
            WordRemarkApiEntity(code = "component_unclear", componentId = null, lemma = null, lexemeKey = null),
            WordRemarkApiEntity(code = "no_translation", componentId = null, lemma = "estudiante", lexemeKey = null),
        )

        val word = found(success(word("estudiante", lemmas = listOf(lemma("estudiante", lexeme("ru/e1"))), remarks = remarks)))

        assertEquals("estudiante", word.text)
        assertTrue(word.remarks.isEmpty())
    }

    @Test
    fun `LFL-89_1 recommendation keeps name, template and options of a choice`() {
        val recommendations = listOf(
            ComponentRecommendationApiEntity(name = "Род", template = "choice", options = listOf("м.р.", "ж.р.")),
            ComponentRecommendationApiEntity(name = "Пример", template = "captioned_text", options = emptyList()),
        )

        val outcome = success(word("perro", lemmas = listOf(lemma("perro", lexeme("ru/p1")))), recommendations).toDomain() as LexemesOutcome.Found

        assertEquals(
            listOf(
                ComponentRecommendation("Род", ComponentTemplate.CHOICE, listOf("м.р.", "ж.р.")),
                ComponentRecommendation("Пример", ComponentTemplate.CAPTIONED_TEXT, emptyList()),
            ),
            outcome.recommendations,
        )
    }

    @Test
    fun `LFL-89 a recommendation of a template the app does not know is dropped, the answer stays`() {
        val recommendations = listOf(ComponentRecommendationApiEntity(name = "Звук", template = "audio", options = emptyList()))

        val outcome = success(word("perro", lemmas = listOf(lemma("perro", lexeme("ru/p1")))), recommendations).toDomain() as LexemesOutcome.Found

        assertEquals("perro", outcome.word.text)
        assertTrue(outcome.recommendations.isEmpty())
    }
}
