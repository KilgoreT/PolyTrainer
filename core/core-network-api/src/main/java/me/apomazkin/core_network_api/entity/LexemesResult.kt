package me.apomazkin.core_network_api.entity

/**
 * Исход запроса `lexemesFromLemma` — ровно один из трёх. Порядок по важности:
 * успех с одним словом, отказ сервера с причиной, сервер недоступен.
 */
sealed interface LexemesResult {

    /**
     * 200: [attribution] источника и лицензии (LFL-54), [word] — слово ответа
     * со статусом, леммами и замечаниями, [recommendations] — по запросу
     * (LFL-89).
     */
    data class Success(
        val attribution: String,
        val word: WordApiEntity,
        val recommendations: List<ComponentRecommendationApiEntity>,
    ) : LexemesResult

    /** 401, 429, 413, 400 и 503 с кодом в теле (LFL-63) или ключ не задан (ACC-19). */
    data class Denied(
        val reason: DenyReason,
    ) : LexemesResult

    /**
     * Сети нет, соединение отклонено, ответа нет 180 секунд, код вне контракта
     * или тело не разбирается (LFL-22, LFL-23). [cause] — для лога, не для
     * пользователя.
     */
    data class Unavailable(
        val cause: String,
    ) : LexemesResult
}
