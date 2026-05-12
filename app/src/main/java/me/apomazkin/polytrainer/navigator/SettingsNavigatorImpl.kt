package me.apomazkin.polytrainer.navigator

import me.apomazkin.settingstab.SettingsNavigator

class SettingsNavigatorImpl(
    private val onOpenLangManagement: () -> Unit,
    private val onOpenAboutApp: () -> Unit,
    private val onOpenWebView: (String) -> Unit,
) : SettingsNavigator {
    override fun back() {
        // таб остаётся открытым
    }

    override fun openLangManagement() = onOpenLangManagement()
    override fun openAboutApp() = onOpenAboutApp()
    override fun openWebView(pageKey: String) = onOpenWebView(pageKey)
}
