package me.apomazkin.polytrainer.navigation

import io.github.kilgoret.mate.NavigationEffect
import io.github.kilgoret.mate.navigation.NavGraph
import io.github.kilgoret.mate.navigation.navGraph
import me.apomazkin.dictionary.list.ListNavigationEffect
import me.apomazkin.dictionaryappbar.DictionaryAppBarNavigationEffect
import me.apomazkin.groupstab.logic.GroupsNavigationEffect
import me.apomazkin.quiztab.QuizTabNavigationEffect
import me.apomazkin.settingstab.SettingsNavigationEffect
import me.apomazkin.splash.SplashNavigationEffect
import me.apomazkin.wordstab.ui.WordsNavigationEffect

/**
 * ВСЯ навигация приложения — одна таблица «эффект → экран». Читается
 * сверху вниз как документация переходов; новый переход = одна строка
 * здесь (плюс сам эффект в экране).
 *
 * Что стоит за push/pop (стек, popUpTo, singleTop, finish) — знает
 * [AppNavigationExecutor].
 */
val appNavGraph: NavGraph = navGraph {
    on<NavigationEffect.Back> { pop() }

    // Сплэш: первичный выбор маршрута. Back формы первичной настройки
    // тоже ведёт на Main — это контекстная политика pop в executor'е.
    on<SplashNavigationEffect.OpenDictionarySetup> { push(AppScreen.DictionarySetup) }
    on<SplashNavigationEffect.OpenMainScreen> { push(AppScreen.Main) }

    // App bar словарей (вкладки Слова/Квиз/Статистика).
    on<DictionaryAppBarNavigationEffect.OpenDictionaryCreate> { push(AppScreen.DictionaryCreate()) }
    on<DictionaryAppBarNavigationEffect.OpenPerDictionaryComponents> {
        push(AppScreen.PerDictionaryComponents(it.dictionaryId))
    }

    // Слово → карточка (вкладки «Слова» и «Группы»).
    on<WordsNavigationEffect.OpenWordCard> { push(AppScreen.WordCard(it.wordId)) }
    on<GroupsNavigationEffect.OpenWordCard> { push(AppScreen.WordCard(it.wordId)) }

    // Квиз.
    on<QuizTabNavigationEffect.OpenChat> { push(AppScreen.ChatQuiz(it.quizType)) }

    // Настройки.
    on<SettingsNavigationEffect.OpenLangManagement> { push(AppScreen.DictionaryList) }
    on<SettingsNavigationEffect.OpenAboutApp> { push(AppScreen.AboutApp) }
    on<SettingsNavigationEffect.OpenWebView> { push(AppScreen.WebView(it.pageKey)) }

    // Список словарей.
    on<ListNavigationEffect.OpenCreate> { push(AppScreen.DictionaryCreate()) }
    on<ListNavigationEffect.OpenEdit> { push(AppScreen.DictionaryCreate(editId = it.id)) }
    on<ListNavigationEffect.ExitApp> { push(AppScreen.ExitApp) }
}
