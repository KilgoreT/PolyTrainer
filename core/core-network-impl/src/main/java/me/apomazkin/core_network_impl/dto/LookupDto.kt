package me.apomazkin.core_network_impl.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Формат провода `lookupWords` один в один с `backend/api/openapi.yaml`.
 * Наружу слоя эти классы не выходят: маппинг в сущности api-модуля —
 * `mapper/LookupMapper.kt`. Поля каталога параметров (`parameters`)
 * сервер пока отдаёт пустыми, здесь их нет: разбор их пропускает
 * (`ignoreUnknownKeys`).
 */

@Serializable
internal data class LookupRequestDto(
        @SerialName("learningLanguage") val learningLanguage: String,
        @SerialName("translationLanguage") val translationLanguage: String,
        @SerialName("tokens") val tokens: List<TokenQueryDto>,
)

@Serializable
internal data class TokenQueryDto(
        @SerialName("text") val text: String,
        @SerialName("confidence") val confidence: Float,
        @SerialName("language") val language: String?,
)

@Serializable
internal data class LookupResponseDto(
        @SerialName("attribution") val attribution: String,
        @SerialName("words") val words: List<WordResultDto>,
)

@Serializable
internal data class WordResultDto(
        @SerialName("text") val text: String,
        @SerialName("status") val status: String,
        @SerialName("candidates") val candidates: List<CandidateDto>,
        /** IS532: леммы ответа с лексемами-кандидатами; старый сервер поля не шлёт — пусто. */
        @SerialName("lemmas") val lemmas: List<LemmaDto> = emptyList(),
)

/** Лемма ответа (WFT-76): текст, пометка формы, лексемы-кандидаты в порядке сервера. */
@Serializable
internal data class LemmaDto(
        @SerialName("text") val text: String,
        @SerialName("formNote") val formNote: String? = null,
        @SerialName("lexemes") val lexemes: List<CandidateLexemeDto> = emptyList(),
)

/**
 * Лексема-кандидат (WFT-71): всё, что сервер знает о значении. Умолчания
 * у каждого поля, кроме ключа и источника, — новый источник или часть
 * речи не должны ронять разбор ответа; незнакомые значения остаются
 * строками.
 */
@Serializable
internal data class CandidateLexemeDto(
        @SerialName("key") val key: String,
        @SerialName("partOfSpeechKey") val partOfSpeechKey: String? = null,
        @SerialName("source") val source: String,
        @SerialName("translations") val translations: List<String> = emptyList(),
        @SerialName("gloss") val gloss: String? = null,
        @SerialName("caption") val caption: String? = null,
        @SerialName("notes") val notes: List<String> = emptyList(),
        @SerialName("examples") val examples: List<String> = emptyList(),
        @SerialName("parent") val parent: Boolean = false,
        @SerialName("parentKey") val parentKey: String? = null,
)

@Serializable
internal data class CandidateDto(
        @SerialName("lemma") val lemma: String,
        @SerialName("partOfSpeech") val partOfSpeech: String,
        @SerialName("formNote") val formNote: String? = null,
        @SerialName("senses") val senses: List<SenseDto> = emptyList(),
        @SerialName("meanings") val meanings: List<String> = emptyList(),
)

@Serializable
internal data class SenseDto(
        @SerialName("gloss") val gloss: String,
        @SerialName("examples") val examples: List<String> = emptyList(),
)

/** Тело 400/401/413/429: машинный код причины и пояснение для логов. */
@Serializable
internal data class ErrorDto(
        @SerialName("code") val code: String,
        @SerialName("message") val message: String = "",
)
