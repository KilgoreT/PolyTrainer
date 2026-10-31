package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_network_api.entity.CandidateLexemeApiEntity
import me.apomazkin.core_network_api.entity.ComponentMatchApiEntity
import me.apomazkin.core_network_api.entity.ComponentNodeApiEntity
import me.apomazkin.core_network_api.entity.ComponentRecommendationApiEntity
import me.apomazkin.core_network_api.entity.DenyReason
import me.apomazkin.core_network_api.entity.LemmaApiEntity
import me.apomazkin.core_network_api.entity.LemmaContextApiEntity
import me.apomazkin.core_network_api.entity.LexemesRequestApiEntity
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
import me.apomazkin.lexeme.LookupLemma
import me.apomazkin.lexeme.LookupLexeme
import me.apomazkin.lexeme.LookupWord
import me.apomazkin.lexeme.LookupWordStatus
import me.apomazkin.lexeme.WordRemark

/*
 * IS536, IS538: граница «слой сети → домен» для операции `lexemes/from-lemma`
 * (Д57, Д114). Зеркала `*ApiEntity` из `core-network-api` становятся доменом
 * ответа сервера слов (`modules/domain/lexeme`) только здесь; экран снимка и
 * диалог карточки слова зеркал не видят.
 */

private const val REMARK_COMPONENT_UNCLEAR = "component_unclear"
private const val REMARK_NO_TRANSLATION = "no_translation"

/**
 * Запрос `lexemes/from-lemma` (LFL-103, LFL-104): языки словаря, [lemma] как
 * есть, [context] снимка или null у карточки слова, дерево компонентов
 * словаря ([components], LFL-78).
 */
fun lexemesRequestOf(
    dictionary: DictionaryApiEntity,
    lemma: String,
    context: LemmaContext?,
    components: List<ComponentNodeApiEntity>,
): LexemesRequestApiEntity = LexemesRequestApiEntity(
    learningLanguage = dictionary.learningLanguage,
    translationLanguage = dictionary.translationLanguage,
    lemma = lemma,
    context = context?.toApiEntity(),
    components = components,
)

private fun LemmaContext.toApiEntity(): LemmaContextApiEntity = LemmaContextApiEntity(
    text = text,
    translation = translation,
)

/**
 * Исход слоя сети → исход значений леммы (LFL-50, LFL-23): `not_found` — не
 * найдено; `error` — слово «ошибка» (LFL-97); `found` без лексем ни у одной
 * леммы — значений нет (LFL-100.2, AWP-55.7); иначе найдено. Леммы словаря с
 * текстом леммы ответа ([LexemesOutcome.Found.existing]) добавляет юзкейс.
 */
fun LexemesResult.toDomain(): LexemesOutcome = when (this) {
    is LexemesResult.Success -> word.toOutcome(recommendations.mapNotNull { it.toDomain() })
    is LexemesResult.Denied -> LexemesOutcome.Denied(reason.toDomain())
    is LexemesResult.Unavailable -> LexemesOutcome.Unavailable
}

private fun WordApiEntity.toOutcome(
    recommendations: List<ComponentRecommendation>,
): LexemesOutcome = when {
    status == WordStatus.NOT_FOUND -> LexemesOutcome.NotFound
    status == WordStatus.ERROR -> LexemesOutcome.WordFailed
    lemmas.none { it.lexemes.isNotEmpty() } -> LexemesOutcome.NoLexemes
    else -> LexemesOutcome.Found(word = toDomain(), recommendations = recommendations)
}

/** Слово ответа со статусом, леммами и замечаниями; у «не найдено» и «ошибка» лемм нет (LFL-50). */
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
    inContext = inContext,
)

fun ComponentMatchApiEntity.toDomain(): ComponentMatch = ComponentMatch(
    componentId = componentId,
    optionId = optionId,
    field = field,
)

/**
 * Замечание по слову по коду (LFL-90.8): `component_unclear` с компонентом,
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
 * Рекомендация завести компонент (LFL-89). Шаблон, которого приложение не
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
