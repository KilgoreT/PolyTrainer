package me.apomazkin.core_network_api.entity

/**
 * Запрос `lexemesFromLemma` (LFL-103, LFL-104): языки словаря в BCP-47,
 * [lemma] как прислал экран — словарная форма из списков снимка или текст
 * слова карточки без краёв, [context] — текст снимка с переводом, null у
 * карточки слова, [components] — дерево компонентов словаря (LFL-78); пустое
 * дерево — поля в запросе нет (LFL-78.7).
 */
data class LexemesRequestApiEntity(
    val learningLanguage: String,
    val translationLanguage: String,
    val lemma: String,
    val context: LemmaContextApiEntity?,
    val components: List<ComponentNodeApiEntity>,
)

/** Контекст леммы: исправленный [text] снимка и его [translation] из ответа `lemmasFromTokens` (LFT-14). */
data class LemmaContextApiEntity(
    val text: String,
    val translation: String,
)
