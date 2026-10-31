package me.apomazkin.core_network_impl.mapper

import me.apomazkin.core_network_api.entity.LemmaContextApiEntity
import me.apomazkin.core_network_api.entity.LexemesRequestApiEntity
import me.apomazkin.core_network_api.entity.LexemesResult
import me.apomazkin.core_network_impl.net.LemmaContextNet
import me.apomazkin.core_network_impl.net.LexemesRequestNet
import me.apomazkin.core_network_impl.net.LexemesResponseNet

/*
 * Транспорт ↔ зеркало операции `lexemesFromLemma`, один в один: запрос —
 * toNet(), ответ — toApiEntity(). Слово ответа, дерево и рекомендации —
 * мапперы `WordMappers.kt`.
 */

/** Запрос на провод: контекста нет — поля нет (контракт 1.1); пустое дерево — поля `components` нет (LFL-78.7). */
internal fun LexemesRequestApiEntity.toNet(): LexemesRequestNet = LexemesRequestNet(
    learningLanguage = learningLanguage,
    translationLanguage = translationLanguage,
    lemma = lemma,
    context = context?.toNet(),
    components = components.takeIf { it.isNotEmpty() }?.map { it.toNet() },
)

private fun LemmaContextApiEntity.toNet(): LemmaContextNet = LemmaContextNet(
    text = text,
    translation = translation,
)

internal fun LexemesResponseNet.toApiEntity(): LexemesResult.Success = LexemesResult.Success(
    attribution = attribution,
    word = word.toApiEntity(),
    recommendations = recommendations.map { it.toApiEntity() },
)
