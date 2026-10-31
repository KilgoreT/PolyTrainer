package me.apomazkin.core_network_api.entity

/**
 * Исход запроса `lemmas/from-tokens` — ровно один из трёх. Порядок по
 * важности: успех, отказ сервера с причиной, сервер недоступен.
 */
sealed interface LemmasResult {

    /**
     * 200 (LFT-14): [text] из токенов с исправленными ошибками
     * распознавания, [translation] на язык перевода, [lexical] — смысловые
     * словарные формы в порядке текста, [proper] — имена собственные,
     * [function] — служебные слова, [unparsed] — токены без пары и обрывки.
     */
    data class Success(
        val text: String,
        val translation: String,
        val lexical: List<String>,
        val proper: List<String>,
        val function: List<String>,
        val unparsed: List<String>,
    ) : LemmasResult

    /**
     * 401, 429, 413, 400 и 503 с кодом в теле или ключ не задан (ACC-19); причины
     * как у `lookup`.
     */
    data class Denied(
        val reason: DenyReason,
    ) : LemmasResult

    /**
     * Сети нет, соединение отклонено, ответа нет 180 секунд, код вне контракта
     * или тело не разбирается (LFL-22, LFL-23). [cause] — для лога, не для
     * пользователя.
     */
    data class Unavailable(
        val cause: String,
    ) : LemmasResult
}
