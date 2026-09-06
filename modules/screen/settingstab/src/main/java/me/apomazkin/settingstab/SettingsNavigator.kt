package me.apomazkin.settingstab

import io.github.kilgoret.mate.Navigator

interface SettingsNavigator : Navigator {
    fun openLangManagement()
    fun openAboutApp()
    fun openWebView(pageKey: String)
}
