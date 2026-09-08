package me.apomazkin.main

/**
 * Route-строки tabs-стека — единственный источник и для графа
 * ([vocabulary]/[quiz]/[settings]), и для app-интерпретатора
 * навигационных команд (navigate по этим же билдерам).
 */
object MainRoutes {
    internal const val WORD_CARD_PATTERN = "wordCard/{wordId}"
    internal const val PER_DICT_COMPONENTS_PATTERN = "per_dict_components/{dictionaryId}"
    internal const val QUIZ_CHAT_PATTERN = "quiz/{quizType}"
    internal const val WEBVIEW_PATTERN = "webview/{pageKey}"
    const val ABOUT_APP = "about_app"

    fun wordCard(wordId: Long): String = "wordCard/$wordId"

    fun perDictionaryComponents(dictionaryId: Long): String = "per_dict_components/$dictionaryId"

    fun quizChat(quizType: String): String = "quiz/$quizType"

    fun webView(pageKey: String): String = "webview/$pageKey"
}
