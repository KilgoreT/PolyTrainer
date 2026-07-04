package me.apomazkin.settingstab

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.apomazkin.mate.MateNavigationEffectHandler
import me.apomazkin.mate.NavigationEffect
import me.apomazkin.settingstab.logic.Msg

class SettingsNavigationEffectHandler @AssistedInject constructor(
    @Assisted private val settingsNavigator: SettingsNavigator,
) : MateNavigationEffectHandler<Msg>(settingsNavigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        when (effect) {
            is SettingsNavigationEffect.OpenLangManagement -> settingsNavigator.openLangManagement()
            is SettingsNavigationEffect.OpenAboutApp -> settingsNavigator.openAboutApp()
            is SettingsNavigationEffect.OpenWebView -> settingsNavigator.openWebView(effect.pageKey)
            is SettingsNavigationEffect.OpenComponentsManager -> settingsNavigator.openComponentsManager()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: SettingsNavigator): SettingsNavigationEffectHandler
    }
}
