package me.apomazkin.settingstab

import me.apomazkin.mate.Navigator

interface SettingsNavigator : Navigator {
    fun openLangManagement()
    fun openAboutApp()
    fun openWebView(pageKey: String)
    fun openComponentsManager()
}
