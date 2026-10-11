package me.apomazkin.core_network_api.entity

/**
 * Слово ответа (Ш10): [text] после нормализации сервером, [status] и
 * [candidates] — пусто при «не найдено» и «ошибка источника» (WFT-50).
 */
data class WordResult(
        val text: String,
        val status: WordStatus,
        val candidates: List<Candidate>,
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
