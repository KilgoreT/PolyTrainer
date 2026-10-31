package me.apomazkin.core_network_api.entity

/**
 * Запрос `lemmas/from-tokens` (LFT-02): токены как распознаны, без
 * нормализации, с повторами; языки словаря в BCP-47. Дерева компонентов
 * нет — значений эта операция не даёт (LFT-02.3).
 */
data class LemmasRequestApiEntity(
    val learningLanguage: String,
    val translationLanguage: String,
    val tokens: List<TokenApiEntity>,
)
