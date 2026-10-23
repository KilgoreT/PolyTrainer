package me.apomazkin.core_network_impl.mapper

import me.apomazkin.core_network_api.entity.CandidateLexemeApiEntity
import me.apomazkin.core_network_api.entity.ComponentMatchApiEntity
import me.apomazkin.core_network_api.entity.ComponentNodeApiEntity
import me.apomazkin.core_network_api.entity.ComponentOptionNodeApiEntity
import me.apomazkin.core_network_api.entity.ComponentRecommendationApiEntity
import me.apomazkin.core_network_api.entity.LemmaApiEntity
import me.apomazkin.core_network_api.entity.LookupRequestApiEntity
import me.apomazkin.core_network_api.entity.LookupResult
import me.apomazkin.core_network_api.entity.TokenApiEntity
import me.apomazkin.core_network_api.entity.WordApiEntity
import me.apomazkin.core_network_api.entity.WordRemarkApiEntity
import me.apomazkin.core_network_api.entity.WordStatus
import me.apomazkin.core_network_impl.net.CandidateLexemeNet
import me.apomazkin.core_network_impl.net.ComponentMatchNet
import me.apomazkin.core_network_impl.net.ComponentNet
import me.apomazkin.core_network_impl.net.ComponentOptionNet
import me.apomazkin.core_network_impl.net.ComponentRecommendationNet
import me.apomazkin.core_network_impl.net.LemmaNet
import me.apomazkin.core_network_impl.net.LookupRequestNet
import me.apomazkin.core_network_impl.net.LookupResponseNet
import me.apomazkin.core_network_impl.net.TokenNet
import me.apomazkin.core_network_impl.net.WordRemarkNet
import me.apomazkin.core_network_impl.net.WordResultNet

/* Транспорт ↔ зеркало, один в один: запрос — toNet(), ответ — toApiEntity(). */

/** Запрос на провод; пустое дерево — поля `components` нет (WFT-78.7). */
internal fun LookupRequestApiEntity.toNet(): LookupRequestNet = LookupRequestNet(
    learningLanguage = learningLanguage,
    translationLanguage = translationLanguage,
    tokens = tokens.map { it.toNet() },
    components = components.takeIf { it.isNotEmpty() }?.map { it.toNet() },
)

private fun TokenApiEntity.toNet(): TokenNet = TokenNet(
    text = text,
    confidence = confidence,
    language = language,
)

private fun ComponentNodeApiEntity.toNet(): ComponentNet = ComponentNet(
    id = id,
    key = key,
    name = name,
    template = template,
    core = core,
    multiple = multiple,
    options = options.map { it.toNet() },
    children = children.map { it.toNet() },
)

private fun ComponentOptionNodeApiEntity.toNet(): ComponentOptionNet = ComponentOptionNet(
    id = id,
    key = key,
    label = label,
    children = children.map { it.toNet() },
)

internal fun LookupResponseNet.toApiEntity(): LookupResult.Success = LookupResult.Success(
    attribution = attribution,
    words = words.map { it.toApiEntity() },
    recommendations = recommendations.map { it.toApiEntity() },
)

private fun WordResultNet.toApiEntity(): WordApiEntity = WordApiEntity(
    text = text,
    status = statusOf(status),
    lemmas = lemmas.map { it.toApiEntity() },
    remarks = remarks.map { it.toApiEntity() },
)

/** Транскрипции леммы — только `ipa` каждого произношения (AWP-29.10). */
private fun LemmaNet.toApiEntity(): LemmaApiEntity = LemmaApiEntity(
    text = text,
    formNote = formNote,
    pronunciations = pronunciations.map { it.ipa },
    lexemes = lexemes.map { it.toApiEntity() },
)

/** Источник и ключ части речи — строки как есть: сверка с опциями словаря — у приложения. */
private fun CandidateLexemeNet.toApiEntity(): CandidateLexemeApiEntity = CandidateLexemeApiEntity(
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
    componentMatches = componentMatches.map { it.toApiEntity() },
)

private fun ComponentMatchNet.toApiEntity(): ComponentMatchApiEntity = ComponentMatchApiEntity(
    componentId = componentId,
    optionId = optionId,
    field = field,
)

private fun WordRemarkNet.toApiEntity(): WordRemarkApiEntity = WordRemarkApiEntity(
    code = code,
    componentId = componentId,
    lemma = lemma,
    lexemeKey = lexemeKey,
)

private fun ComponentRecommendationNet.toApiEntity(): ComponentRecommendationApiEntity =
    ComponentRecommendationApiEntity(
        name = name,
        template = template,
        options = options,
    )

/** Статус как в контракте; неизвестная строка — «ошибка источника», не падение. */
private fun statusOf(raw: String): WordStatus = when (raw) {
    "found" -> WordStatus.FOUND
    "not_found" -> WordStatus.NOT_FOUND
    else -> WordStatus.ERROR
}
