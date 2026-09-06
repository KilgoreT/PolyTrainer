package me.apomazkin.dictionaryappbar

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.apomazkin.dictionaryappbar.mate.Msg
import io.github.kilgoret.mate.MateNavigationEffectHandler
import io.github.kilgoret.mate.NavigationEffect

class DictionaryAppBarNavigationEffectHandler @AssistedInject constructor(
    @Assisted private val barNavigator: DictionaryAppBarNavigator,
) : MateNavigationEffectHandler<Msg>(barNavigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        when (effect) {
            is DictionaryAppBarNavigationEffect.OpenDictionaryCreate -> barNavigator.openDictionaryCreate()
            is DictionaryAppBarNavigationEffect.OpenPerDictionaryComponents ->
                barNavigator.openPerDictionaryComponents(effect.dictionaryId)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: DictionaryAppBarNavigator): DictionaryAppBarNavigationEffectHandler
    }
}
