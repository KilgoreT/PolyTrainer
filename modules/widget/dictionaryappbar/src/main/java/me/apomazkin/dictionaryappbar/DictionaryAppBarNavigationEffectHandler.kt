package me.apomazkin.dictionaryappbar

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.apomazkin.dictionaryappbar.mate.Msg
import me.apomazkin.mate.MateNavigationEffectHandler
import me.apomazkin.mate.NavigationEffect

class DictionaryAppBarNavigationEffectHandler @AssistedInject constructor(
    @Assisted private val barNavigator: DictionaryAppBarNavigator,
) : MateNavigationEffectHandler<Msg>(barNavigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        when (effect) {
            is DictionaryAppBarNavigationEffect.OpenDictionaryCreate -> barNavigator.openDictionaryCreate()
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: DictionaryAppBarNavigator): DictionaryAppBarNavigationEffectHandler
    }
}
