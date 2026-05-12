package me.apomazkin.polytrainer.navigator

import me.apomazkin.splash.SplashNavigator

class SplashNavigatorImpl(
    private val onOpenDictionarySetup: () -> Unit,
    private val onOpenMainScreen: () -> Unit,
) : SplashNavigator {
    override fun back() {
        // нет смысла на splash
    }

    override fun openDictionarySetup() = onOpenDictionarySetup()
    override fun openMainScreen() = onOpenMainScreen()
}
