package me.apomazkin.settingstab

import me.apomazkin.mate.NavigationEffect

sealed interface SettingsNavigationEffect : NavigationEffect {
    data object OpenLangManagement : SettingsNavigationEffect
    data object OpenAboutApp : SettingsNavigationEffect
    data class OpenWebView(val pageKey: String) : SettingsNavigationEffect
    data object OpenComponentsManager : SettingsNavigationEffect
}
