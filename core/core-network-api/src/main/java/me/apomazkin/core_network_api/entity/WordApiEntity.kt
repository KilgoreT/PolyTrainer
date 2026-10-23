package me.apomazkin.core_network_api.entity

/**
 * Слово ответа (WFT-50): [text] после нормализации сервером, [status],
 * [remarks] — замечания по слову (WFT-91), [lemmas] — пусто при «не
 * найдено», «ошибка» и когда нейронка ничего не выбрала (WFT-100.2).
 */
data class WordApiEntity(
    val text: String,
    val status: WordStatus,
    val lemmas: List<LemmaApiEntity>,
    val remarks: List<WordRemarkApiEntity>,
)

/**
 * Лемма ответа (WFT-76): [formNote] — чем слово является относительно
 * леммы или null, [pronunciations] — транскрипции записи (WFT-81),
 * [lexemes] — отобранные лексемы-кандидаты в порядке отбора (WFT-90).
 */
data class LemmaApiEntity(
    val text: String,
    val formNote: String?,
    val pronunciations: List<String>,
    val lexemes: List<CandidateLexemeApiEntity>,
)

/**
 * Лексема-кандидат (WFT-71): данные записи целиком (WFT-81) и
 * сопоставление с деревом компонентов запроса (WFT-87). [source] и
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
 * [lexemeKey]; незнакомый код остаётся строкой (WFT-90.8).
 */
data class WordRemarkApiEntity(
    val code: String,
    val componentId: Long?,
    val lemma: String?,
    val lexemeKey: String?,
)

/** Рекомендация завести компонент (WFT-89): [template] — ключ шаблона, [options] — подписи опций у `choice`. */
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
