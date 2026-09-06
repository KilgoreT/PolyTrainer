package me.apomazkin.settingstab

import io.github.kilgoret.mate.NavigationEffect

sealed interface SettingsNavigationEffect : NavigationEffect {
    data object OpenLangManagement : SettingsNavigationEffect
    data object OpenAboutApp : SettingsNavigationEffect
    data class OpenWebView(val pageKey: String) : SettingsNavigationEffect
}
