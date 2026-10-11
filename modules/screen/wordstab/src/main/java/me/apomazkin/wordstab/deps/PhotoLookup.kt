package me.apomazkin.wordstab.deps

/**
 * IS529: исход запроса слов на сервер глазами экрана — ровно то, что
 * диалог показывает. Сущности слоя сети сюда не доходят: маппинг в `app`,
 * как у `TermUiItem` и `DictUiEntity`. Порядок вариантов по важности.
 */
sealed interface PhotoLookupOutcome {

    /** Ответ сервера получен: слова в порядке ответа (Ш11). */
    data class Words(val words: List<PhotoWord>) : PhotoLookupOutcome

    /** Отказ сервера или ключ не задан; текст по причине (В5, WFT-63). */
    data class Denied(val reason: PhotoDenyReason) : PhotoLookupOutcome

    /** Сети нет, таймаут, ответ не по контракту → «сервер не ответил» (В3). */
    data object Unavailable : PhotoLookupOutcome
}

/**
 * Слово ответа для списка (WFT-56): [text] как вернул сервер, [status], у
 * найденного — [lemma] и [partOfSpeech] первого кандидата (Д4), [meaning] —
 * его первое значение на языке перевода (Д6, WFT-56.10) и [gloss] — первое
 * толкование на языке источника (Д5, WFT-56.9); иначе null.
 */
data class PhotoWord(
        val text: String,
        val status: PhotoWordStatus,
        val lemma: String?,
        val partOfSpeech: String?,
        val meaning: String?,
        val gloss: String?,
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
