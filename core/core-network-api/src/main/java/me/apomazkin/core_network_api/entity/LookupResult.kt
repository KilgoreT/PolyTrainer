package me.apomazkin.core_network_api.entity

/**
 * Исход запроса `lookupWords` — ровно один из трёх. Порядок по важности:
 * успех, отказ сервера с причиной, сервер недоступен.
 */
sealed interface LookupResult {

    /**
     * 200: [attribution] источника и лицензии (WFT-54), [words] в порядке
     * ответа, [recommendations] — замечания по запросу (WFT-89).
     */
    data class Success(
        val attribution: String,
        val words: List<WordApiEntity>,
        val recommendations: List<ComponentRecommendationApiEntity>,
    ) : LookupResult

    /** 401, 429, 413, 400 и 503 с кодом в теле (WFT-63) или ключ не задан (WFT-19). */
    data class Denied(
        val reason: DenyReason,
    ) : LookupResult

    /**
     * Сети нет, соединение отклонено, ответа нет 180 секунд, код вне контракта
     * или тело не разбирается (WFT-22, WFT-23). [cause] — для лога, не для
     * пользователя.
     */
    data class Unavailable(
        val cause: String,
    ) : LookupResult
}

/** Причина отказа; устройство выбирает текст по ней (WFT-63). */
enum class DenyReason {
    /** Ключ доступа на устройстве не задан, запрос не отправлялся (WFT-19). */
    NO_ACCESS_KEY,

    /** 401 `unauthorized` (WFT-26). */
    UNAUTHORIZED,

    /** 429 `rate_limited` (WFT-29, WFT-96). */
    RATE_LIMITED,

    /** 413 `request_too_large` (WFT-28). */
    REQUEST_TOO_LARGE,

    /** 400 `unsupported_language` (WFT-30). */
    UNSUPPORTED_LANGUAGE,

    /** 503 `llm_unavailable` (WFT-99, WFT-63.5). */
    LLM_UNAVAILABLE,
}
