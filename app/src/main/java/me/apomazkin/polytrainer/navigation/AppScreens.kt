package me.apomazkin.polytrainer.navigation

import io.github.kilgoret.mate.navigation.Screen

/**
 * Экраны приложения — пункты назначения [appNavTable], только данные
 * (аргументы перехода внутри). Как эти объекты превращаются в вызовы
 * NavController'ов — знает единственно [AppNavigationExecutor]:
 * root-стек (словари, main) и tabs-стек (карточка, квиз, настройки)
 * разводятся там же.
 */
sealed interface AppScreen : Screen {

    // ===== root-стек =====

    /** Первичная настройка: форма первого словаря вместо сплэша. */
    data object DictionarySetup : AppScreen

    /** Форма словаря; [editId] null — создание, иначе — правка. */
    data class DictionaryCreate(val editId: Long? = null) : AppScreen

    /** Список словарей (управление языками). */
    data object DictionaryList : AppScreen

    /** Главный экран с вкладками; вход схлопывает стек до себя. */
    data object Main : AppScreen

    /** Псевдоэкран «выйти из приложения» — исполняется как finish(). */
    data object ExitApp : AppScreen

    // ===== tabs-стек =====

    /** Карточка слова. */
    data class WordCard(val wordId: Long) : AppScreen

    /** Компоненты конкретного словаря. */
    data class PerDictionaryComponents(val dictionaryId: Long) : AppScreen

    /** Чат-квиз выбранного типа. */
    data class ChatQuiz(val quizType: String) : AppScreen

    /** «О приложении». */
    data object AboutApp : AppScreen

    /** Встроенный WebView (политика конфиденциальности и т.п.). */
    data class WebView(val pageKey: String) : AppScreen
}
