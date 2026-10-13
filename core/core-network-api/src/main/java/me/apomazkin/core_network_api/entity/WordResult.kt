package me.apomazkin.core_network_api.entity

/**
 * Слово ответа (Ш10): [text] после нормализации сервером, [status],
 * [candidates] и [lemmas] — пусто при «не найдено» и «ошибка источника»
 * (WFT-50). Окно добавления читает [lemmas] (WFT-76); [candidates] —
 * старый список для просмотра.
 */
data class WordResult(
        val text: String,
        val status: WordStatus,
        val candidates: List<Candidate>,
        val lemmas: List<Lemma> = emptyList(),
)

/**
 * Лемма ответа (WFT-76): леммы-кандидаты одного текста склеены сервером,
 * [formNote] — от первой из них или null, если слово — сама лемма,
 * [lexemes] — в порядке сервера (WFT-75).
 */
data class Lemma(
        val text: String,
        val formNote: String?,
        val lexemes: List<CandidateLexeme>,
)

/**
 * Лексема-кандидат (WFT-71): [key] уникален у леммы, [partOfSpeechKey] —
 * ключ опции «Части речи» словаря или null (WFT-72), [source] — строка
 * источника как у сервера (`ruwiktionary`, `enwiktionary`, новые
 * остаются строками), [translations] на языке перевода без повторов,
 * [gloss] и [examples] — у английского Викисловаря, [caption] — у
 * непривязанных переводов, [notes] — пометы, [parent] с [parentKey] —
 * родительское значение и ссылка уточнения на него.
 */
data class CandidateLexeme(
        val key: String,
        val partOfSpeechKey: String?,
        val source: String,
        val translations: List<String>,
        val gloss: String?,
        val caption: String?,
        val notes: List<String>,
        val examples: List<String>,
        val parent: Boolean,
        val parentKey: String?,
)

/** Статус слова в ответе, как в контракте: `found`, `not_found`, `error`. */
enum class WordStatus {
    FOUND,
    NOT_FOUND,
    ERROR,
}

/**
 * Кандидат леммы (WFT-44, WFT-45): [lemma] — словарная форма, [partOfSpeech]
 * как в источнике, [formNote] — чем слово является относительно леммы или
 * null, если слово — сама лемма, [senses] — значения без разметки на языке
 * источника, [meanings] — значения на языке перевода из Викисловаря этого
 * языка (WFT-66), пусто, если их нет.
 */
data class Candidate(
        val lemma: String,
        val partOfSpeech: String,
        val formNote: String?,
        val senses: List<Sense>,
        val meanings: List<String>,
)

/** Значение: толкование на английском и примеры (WFT-51). */
data class Sense(
        val gloss: String,
        val examples: List<String>,
)
