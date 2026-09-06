package me.apomazkin.dictionary.list

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.kilgoret.mate.MateNavigationEffectHandler
import io.github.kilgoret.mate.NavigationEffect

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
