package me.apomazkin.wordstab.deps

/**
 * IS529/IS532: исход запроса слов на сервер глазами экрана. Сущности слоя
 * сети сюда не доходят: маппинг в `app`, как у `TermUiItem` и
 * `DictUiEntity`. Порядок вариантов по важности.
 */
sealed interface PhotoLookupOutcome {

    /**
     * Ответ сервера получен: слова в порядке ответа (Ш10) и словарь
     * снимка — тот, на вкладке которого сделан снимок (AWP-01).
     */
    data class Words(
            val dictionaryId: Long,
            val words: List<PhotoWord>,
    ) : PhotoLookupOutcome

    /** Отказ сервера или ключ не задан; текст по причине (В5, WFT-63). */
    data class Denied(val reason: PhotoDenyReason) : PhotoLookupOutcome

    /** Сети нет, таймаут, ответ не по контракту → «сервер не ответил» (В3). */
    data object Unavailable : PhotoLookupOutcome
}

/**
 * Слово ответа (WFT-50): [text] как вернул сервер, [status]; у найденного
 * — [lemmas] с лексемами-кандидатами (WFT-76), у остальных пусто.
 */
data class PhotoWord(
        val text: String,
        val status: PhotoWordStatus,
        val lemmas: List<PhotoLemma>,
)

/** Лемма ответа (WFT-76): текст, пометка формы, лексемы-кандидаты в порядке сервера. */
data class PhotoLemma(
        val text: String,
        val formNote: String?,
        val lexemes: List<PhotoLexeme>,
)

/**
 * Лексема-кандидат (WFT-71): [key] уникален у леммы, [partOfSpeechKey] —
 * ключ опции «Части речи» или null, [source] — строка источника,
 * [translations] на языке перевода, [gloss] и [examples] — у английского
 * Викисловаря, [caption] — у непривязанных переводов, [notes] — пометы,
 * [parent] и [parentKey] — родительское значение и ссылка на него.
 */
data class PhotoLexeme(
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

enum class PhotoWordStatus {
    FOUND,
    NOT_FOUND,
    ERROR,
}

enum class PhotoDenyReason {
    NO_ACCESS_KEY,
    UNAUTHORIZED,
    RATE_LIMITED,
    REQUEST_TOO_LARGE,
    UNSUPPORTED_LANGUAGE,
}
