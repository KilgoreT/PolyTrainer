package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_network_api.entity.CandidateLexemeApiEntity
import me.apomazkin.core_network_api.entity.ComponentMatchApiEntity
import me.apomazkin.core_network_api.entity.ComponentNodeApiEntity
import me.apomazkin.core_network_api.entity.ComponentRecommendationApiEntity
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.LemmaApiEntity
import me.apomazkin.core_network_api.entity.LookupRequestApiEntity
import me.apomazkin.core_network_api.entity.LookupResult
import me.apomazkin.core_network_api.entity.TokenApiEntity
import me.apomazkin.core_network_api.entity.WordApiEntity
import me.apomazkin.core_network_api.entity.WordRemarkApiEntity
import me.apomazkin.core_network_api.entity.WordStatus
import me.apomazkin.lexeme.ComponentMatch
import me.apomazkin.lexeme.ComponentRecommendation
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.LookupDenyReason
import me.apomazkin.lexeme.LookupLemma
import me.apomazkin.lexeme.LookupLexeme
import me.apomazkin.lexeme.LookupOutcome
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.LookupWord
import me.apomazkin.lexeme.LookupWordStatus
import me.apomazkin.lexeme.WordRemark
import me.apomazkin.textrecognition.RecognizedWord

/*
 * IS536: граница «слой сети → домен» (Д57). Зеркала `*ApiEntity` из
 * `core-network-api` становятся доменом ответа сервера слов
 * (`modules/domain/lexeme`) только здесь; экран и виджет зеркал не видят.
 */

private const val REMARK_COMPONENT_UNCLEAR = "component_unclear"
private const val REMARK_NO_TRANSLATION = "no_translation"

/** Токен распознавания → токен запроса: текст, уверенность и язык как отдало распознавание. */
fun RecognizedWord.toDomain(): LookupToken = LookupToken(
    text = text,
    confidence = confidence,
    language = language,
)

/**
 * Запрос `lookupWords` (WFT-20): языки словаря, токены как есть и в том же
 * порядке, дерево компонентов словаря ([components], WFT-78).
 */
fun lookupRequestOf(
    dictionary: DictionaryApiEntity,
    tokens: List<LookupToken>,
    components: List<ComponentNodeApiEntity>,
): LookupRequestApiEntity = LookupRequestApiEntity(
    learningLanguage = dictionary.learningLanguage,
    translationLanguage = dictionary.translationLanguage,
    tokens = tokens.map { it.toApiEntity() },
    components = components,
)

private fun LookupToken.toApiEntity(): TokenApiEntity = TokenApiEntity(
    text = text,
    confidence = confidence,
    language = language,
)

/**
 * Исход слоя сети → доменный исход (WFT-50, WFT-63). Ответ без слов — «нет
 * слов» (WFP-19.2); [dictionaryId] — словарь запроса (AWP-01).
 */
fun LookupResult.toDomain(dictionaryId: Long): LookupOutcome = when (this) {
    is LookupResult.Success -> when {
        words.isEmpty() -> LookupOutcome.NoWords
        else -> LookupOutcome.Words(
            dictionaryId = dictionaryId,
            words = words.map { it.toDomain() },
            recommendations = recommendations.mapNotNull { it.toDomain() },
        )
    }
    is LookupResult.Denied -> LookupOutcome.Denied(reason.toDomain())
    is LookupResult.Unavailable -> LookupOutcome.Unavailable
}

/** Слово ответа со статусом, леммами и замечаниями; у «не найдено» и «ошибка» лемм нет (WFT-50). */
fun WordApiEntity.toDomain(): LookupWord = LookupWord(
    text = text,
    status = status.toDomain(),
    lemmas = lemmas.map { it.toDomain() },
    remarks = remarks.mapNotNull { it.toDomain() },
)

fun LemmaApiEntity.toDomain(): LookupLemma = LookupLemma(
    text = text,
    formNote = formNote,
    pronunciations = pronunciations,
    lexemes = lexemes.map { it.toDomain() },
)

fun CandidateLexemeApiEntity.toDomain(): LookupLexeme = LookupLexeme(
    key = key,
    partOfSpeechKey = partOfSpeechKey,
    source = source,
    translations = translations,
    gloss = gloss,
    caption = caption,
    notes = notes,
    examples = examples,
    grammar = grammar,
    level = level,
    synonyms = synonyms,
    antonyms = antonyms,
    componentMatches = componentMatches.map { it.toDomain() },
)

fun ComponentMatchApiEntity.toDomain(): ComponentMatch = ComponentMatch(
    componentId = componentId,
    optionId = optionId,
    field = field,
)

/**
 * Замечание по слову по коду (WFT-90.8): `component_unclear` с компонентом,
 * `no_translation` с леммой и ключом лексемы. Незнакомый код и замечание без
 * своих полей — null: раздел «Замечания» его не покажет, ответ остаётся
 * годным.
 */
fun WordRemarkApiEntity.toDomain(): WordRemark? {
    val remarkComponentId = componentId
    val remarkLemma = lemma
    val remarkLexemeKey = lexemeKey
    return when {
        code == REMARK_COMPONENT_UNCLEAR && remarkComponentId != null ->
            WordRemark.ComponentUnclear(remarkComponentId)
        code == REMARK_NO_TRANSLATION && remarkLemma != null && remarkLexemeKey != null ->
            WordRemark.NoTranslation(lemma = remarkLemma, lexemeKey = remarkLexemeKey)
        else -> null
    }
}

/**
 * Рекомендация завести компонент (WFT-89). Шаблон, которого приложение не
 * знает, — null: завести такой компонент пользователь всё равно не сможет.
 */
fun ComponentRecommendationApiEntity.toDomain(): ComponentRecommendation? =
    ComponentTemplate.fromKey(template)?.let { known ->
        ComponentRecommendation(
            name = name,
            template = known,
            options = options,
        )
    }

fun WordStatus.toDomain(): LookupWordStatus = when (this) {
    WordStatus.FOUND -> LookupWordStatus.FOUND
    WordStatus.NOT_FOUND -> LookupWordStatus.NOT_FOUND
    WordStatus.ERROR -> LookupWordStatus.ERROR
}

fun DenyReason.toDomain(): LookupDenyReason = when (this) {
    DenyReason.NO_ACCESS_KEY -> LookupDenyReason.NO_ACCESS_KEY
    DenyReason.UNAUTHORIZED -> LookupDenyReason.UNAUTHORIZED
    DenyReason.RATE_LIMITED -> LookupDenyReason.RATE_LIMITED
    DenyReason.REQUEST_TOO_LARGE -> LookupDenyReason.REQUEST_TOO_LARGE
    DenyReason.UNSUPPORTED_LANGUAGE -> LookupDenyReason.UNSUPPORTED_LANGUAGE
    DenyReason.LLM_UNAVAILABLE -> LookupDenyReason.LLM_UNAVAILABLE
}
