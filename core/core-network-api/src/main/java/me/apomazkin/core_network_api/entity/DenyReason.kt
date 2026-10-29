package me.apomazkin.core_network_api.entity

/** Причина отказа сервера, общая для операций; устройство выбирает текст по ней (WCL-11.2, AWP-61). */
enum class DenyReason {
    /** Ключ доступа на устройстве не задан, запрос не отправлялся (ACC-19). */
    NO_ACCESS_KEY,

    /** 401 `unauthorized` (ACC-26). */
    UNAUTHORIZED,

    /** 429 `rate_limited` (LIM-29, LIM-96). */
    RATE_LIMITED,

    /** 413 `request_too_large` (LFL-106). */
    REQUEST_TOO_LARGE,

    /** 400 `unsupported_language` (LFL-30). */
    UNSUPPORTED_LANGUAGE,

    /** 503 `llm_unavailable` (LFL-99). */
    LLM_UNAVAILABLE,
}
