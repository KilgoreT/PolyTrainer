package me.apomazkin.dictionary.list

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.apomazkin.mate.MateNavigationEffectHandler
import me.apomazkin.mate.NavigationEffect

class ListNavigationEffectHandler @AssistedInject constructor(
    @Assisted private val listNavigator: ListNavigator,
) : MateNavigationEffectHandler<DictionaryListMsg>(listNavigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        when (effect) {
            is ListNavigationEffect.ExitApp -> listNavigator.exit()
            is ListNavigationEffect.OpenCreate -> listNavigator.openCreate()
            is ListNavigationEffect.OpenEdit -> listNavigator.openEdit(effect.id)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: ListNavigator): ListNavigationEffectHandler
    }
}
