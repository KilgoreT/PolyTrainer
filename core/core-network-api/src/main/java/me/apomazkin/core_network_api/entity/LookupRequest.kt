package me.apomazkin.core_network_api.entity

/**
 * Запрос `lookupWords` (Ш6, WFT-20): токены как распознаны, без нормализации
 * (её делает сервер, Ш8), языки словаря в BCP-47.
 */
data class LookupRequest(
        val learningLanguage: String,
        val translationLanguage: String,
        val tokens: List<TokenQuery>,
)

/**
 * Токен для сервера: [text] как распознан, [confidence] 0…1, [language] по
 * мнению распознавания или null (WFT-20.2).
 */
data class TokenQuery(
        val text: String,
        val confidence: Float,
        val language: String?,
)
