package me.apomazkin.core_network_impl.net

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Формат провода, общий для операций: токен запроса лемм, дерево компонентов
 * запроса значений, слово ответа с леммами и лексемами, рекомендации и тело
 * отказа — один в один с `backend/api/openapi.yaml` 0.5.0. Наружу слоя эти
 * классы не выходят: маппинг в зеркала api-модуля — `mapper/WordMappers.kt`.
 * Поля ответа с умолчаниями: разбор не падает, если сервер поле не прислал;
 * незнакомые поля пропускаются (`ignoreUnknownKeys`).
 */

@Serializable
internal data class TokenNet(
    @SerialName("text") val text: String,
    @SerialName("confidence") val confidence: Float,
    @SerialName("language") val language: String?,
)

/** Узел дерева компонентов (контракт, «Дерево компонентов»); пустые `options` и `children` не пишутся. */
@Serializable
internal data class ComponentNet(
    @SerialName("id") val id: Long,
    @SerialName("key") val key: String?,
    @SerialName("name") val name: String,
    @SerialName("template") val template: String,
    @SerialName("core") val core: Boolean,
    @SerialName("multiple") val multiple: Boolean,
    @SerialName("options") val options: List<ComponentOptionNet> = emptyList(),
    @SerialName("children") val children: List<ComponentNet> = emptyList(),
)

@Serializable
internal data class ComponentOptionNet(
    @SerialName("id") val id: Long,
    @SerialName("key") val key: String?,
    @SerialName("label") val label: String,
    @SerialName("children") val children: List<ComponentNet> = emptyList(),
)

@Serializable
internal data class WordResultNet(
    @SerialName("text") val text: String,
    @SerialName("status") val status: String,
    @SerialName("lemmas") val lemmas: List<LemmaNet> = emptyList(),
    @SerialName("remarks") val remarks: List<WordRemarkNet> = emptyList(),
)

@Serializable
internal data class LemmaNet(
    @SerialName("text") val text: String,
    @SerialName("formNote") val formNote: String? = null,
    @SerialName("pronunciations") val pronunciations: List<PronunciationNet> = emptyList(),
    @SerialName("lexemes") val lexemes: List<CandidateLexemeNet> = emptyList(),
)

@Serializable
internal data class PronunciationNet(
    @SerialName("ipa") val ipa: String,
    @SerialName("accent") val accent: List<String> = emptyList(),
    @SerialName("form") val form: String? = null,
)

/**
 * Лексема-кандидат (LFL-71): умолчания у каждого поля, кроме ключа и
 * источника, — новый источник или часть речи не должны ронять разбор.
 */
@Serializable
internal data class CandidateLexemeNet(
    @SerialName("key") val key: String,
    @SerialName("partOfSpeechKey") val partOfSpeechKey: String? = null,
    @SerialName("source") val source: String,
    @SerialName("translations") val translations: List<String> = emptyList(),
    @SerialName("gloss") val gloss: String? = null,
    @SerialName("caption") val caption: String? = null,
    @SerialName("notes") val notes: List<String> = emptyList(),
    @SerialName("examples") val examples: List<String> = emptyList(),
    @SerialName("grammar") val grammar: Map<String, String> = emptyMap(),
    @SerialName("level") val level: String? = null,
    @SerialName("synonyms") val synonyms: List<String> = emptyList(),
    @SerialName("antonyms") val antonyms: List<String> = emptyList(),
    @SerialName("componentMatches") val componentMatches: List<ComponentMatchNet> = emptyList(),
    /** «В тексте» (LFL-113): поля нет — `false`. */
    @SerialName("inContext") val inContext: Boolean = false,
)

@Serializable
internal data class ComponentMatchNet(
    @SerialName("componentId") val componentId: Long,
    @SerialName("optionId") val optionId: Long? = null,
    @SerialName("field") val field: String? = null,
)

/** Замечание по слову плоско: у `component_unclear` — компонент, у `no_translation` — лемма и ключ. */
@Serializable
internal data class WordRemarkNet(
    @SerialName("code") val code: String,
    @SerialName("componentId") val componentId: Long? = null,
    @SerialName("lemma") val lemma: String? = null,
    @SerialName("lexemeKey") val lexemeKey: String? = null,
)

@Serializable
internal data class ComponentRecommendationNet(
    @SerialName("code") val code: String = "",
    @SerialName("name") val name: String,
    @SerialName("template") val template: String,
    @SerialName("options") val options: List<String> = emptyList(),
)

/** Тело 400/401/413/429/503: машинный код причины и пояснение для логов. */
@Serializable
internal data class ErrorNet(
    @SerialName("code") val code: String,
    @SerialName("message") val message: String = "",
)
