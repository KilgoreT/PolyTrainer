package me.apomazkin.polytrainer.mapper

import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_network_api.entity.LemmasRequestApiEntity
import me.apomazkin.core_network_api.entity.LemmasResult
import me.apomazkin.core_network_api.entity.TokenApiEntity
import me.apomazkin.lexeme.LemmasOutcome
import me.apomazkin.lexeme.LookupToken
import me.apomazkin.lexeme.TextLemmas
import me.apomazkin.textrecognition.RecognizedWord

/*
 * IS537: граница «слой сети → домен» для операции `lemmas/from-tokens` (Д57).
 * Зеркала `*ApiEntity` из `core-network-api` становятся доменом лемм текста
 * (`modules/domain/lexeme`) только здесь; экран зеркал не видит.
 */

/** Токен распознавания → токен запроса: текст, уверенность и язык как отдало распознавание. */
fun RecognizedWord.toDomain(): LookupToken = LookupToken(
    text = text,
    confidence = confidence,
    language = language,
)

/**
 * Запрос `lemmas/from-tokens` (LFT-02): языки словаря, токены как есть и в
 * том же порядке; дерева компонентов нет — значений эта операция не даёт
 * (LFT-02.3).
 */
fun lemmasRequestOf(
    dictionary: DictionaryApiEntity,
    tokens: List<LookupToken>,
): LemmasRequestApiEntity = LemmasRequestApiEntity(
    learningLanguage = dictionary.learningLanguage,
    translationLanguage = dictionary.translationLanguage,
    tokens = tokens.map { it.toApiEntity() },
)

/** Токен запроса: текст, уверенность, язык. */
internal fun LookupToken.toApiEntity(): TokenApiEntity = TokenApiEntity(
    text = text,
    confidence = confidence,
    language = language,
)

/**
 * Исход слоя сети → доменный исход (LFT-14, LFL-63). Четыре пустых списка —
 * «слов нет» (TFP-19.2); [dictionaryId] — словарь снимка (AWP-01).
 */
fun LemmasResult.toDomain(dictionaryId: Long): LemmasOutcome = when (this) {
    is LemmasResult.Success -> when {
        isEmpty() -> LemmasOutcome.NoWords
        else -> LemmasOutcome.Found(dictionaryId = dictionaryId, lemmas = toTextLemmas())
    }
    is LemmasResult.Denied -> LemmasOutcome.Denied(reason.toDomain())
    is LemmasResult.Unavailable -> LemmasOutcome.Unavailable
}

/** Все четыре списка пусты: ни словарной формы, ни «не разобрано» (TFP-19.2). */
private fun LemmasResult.Success.isEmpty(): Boolean =
    lexical.isEmpty() && proper.isEmpty() && function.isEmpty() && unparsed.isEmpty()

/** Поля ответа в домен один в один (LFT-14). */
private fun LemmasResult.Success.toTextLemmas(): TextLemmas = TextLemmas(
    text = text,
    translation = translation,
    lexical = lexical,
    proper = proper,
    function = function,
    unparsed = unparsed,
)
