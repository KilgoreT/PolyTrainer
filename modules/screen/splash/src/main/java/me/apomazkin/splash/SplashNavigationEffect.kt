package me.apomazkin.splash

import io.github.kilgoret.mate.NavigationEffect

/** Навигация сплэша: первичный выбор маршрута по состоянию базы. */
sealed interface SplashNavigationEffect : NavigationEffect {

    /** Словарей нет — на форму первичной настройки. */
    data object OpenDictionarySetup : SplashNavigationEffect

    /** Словари есть — на главный экран. */
    data object OpenMainScreen : SplashNavigationEffect
}
