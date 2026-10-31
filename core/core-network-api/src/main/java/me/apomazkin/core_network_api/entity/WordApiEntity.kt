package me.apomazkin.core_network_api.entity

/**
 * Слово ответа (LFL-50): [text] после нормализации сервером, [status],
 * [remarks] — замечания по слову (LFL-91), [lemmas] — пусто при «не
 * найдено», «ошибка» и когда нейронка ничего не выбрала (LFL-100.2).
 */
data class WordApiEntity(
    val text: String,
    val status: WordStatus,
    val lemmas: List<LemmaApiEntity>,
    val remarks: List<WordRemarkApiEntity>,
)

/**
 * Лемма ответа (LFL-76): [formNote] — чем слово является относительно
 * леммы или null, [pronunciations] — транскрипции записи (LFL-81),
 * [lexemes] — отобранные лексемы-кандидаты в порядке отбора (LFL-90).
 */
data class LemmaApiEntity(
    val text: String,
    val formNote: String?,
    val pronunciations: List<String>,
    val lexemes: List<CandidateLexemeApiEntity>,
)

/**
 * Лексема-кандидат (LFL-71): данные записи целиком (LFL-81) и
 * сопоставление с деревом компонентов запроса (LFL-87). [source] и
 * [partOfSpeechKey] — строки как у сервера, незнакомые значения остаются.
 */
data class CandidateLexemeApiEntity(
    val key: String,
    val partOfSpeechKey: String?,
    val source: String,
    val translations: List<String>,
    val gloss: String?,
    val caption: String?,
    val notes: List<String>,
    val examples: List<String>,
    val grammar: Map<String, String>,
    val level: String?,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val componentMatches: List<ComponentMatchApiEntity>,
    /** Употреблена в тексте снимка (LFL-113): `true` только у первой по тапу, без контекста `false`. */
    val inContext: Boolean = false,
)

/** Строка сопоставления: компонент из дерева запроса и опция «Выбора» или поле лексемы. */
data class ComponentMatchApiEntity(
    val componentId: Long,
    val optionId: Long?,
    val field: String?,
)

/**
 * Замечание по слову, плоско как на проводе: [code] —
 * `component_unclear` с [componentId] или `no_translation` с [lemma] и
 * [lexemeKey]; незнакомый код остаётся строкой (LFL-90.8).
 */
data class WordRemarkApiEntity(
    val code: String,
    val componentId: Long?,
    val lemma: String?,
    val lexemeKey: String?,
)

/**
 * Рекомендация завести компонент (LFL-89): [template] — ключ шаблона, [options] —
 * подписи опций у `choice`.
 */
data class ComponentRecommendationApiEntity(
    val name: String,
    val template: String,
    val options: List<String>,
)

/** Статус слова в ответе, как в контракте: `found`, `not_found`, `error`. */
enum class WordStatus {
    FOUND,
    NOT_FOUND,
    ERROR,
}
