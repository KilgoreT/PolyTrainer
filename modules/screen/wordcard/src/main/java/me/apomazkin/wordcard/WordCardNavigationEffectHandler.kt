package me.apomazkin.wordcard

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.apomazkin.mate.MateNavigationEffectHandler
import me.apomazkin.mate.NavigationEffect
import me.apomazkin.wordcard.mate.Msg

class WordCardNavigationEffectHandler @AssistedInject constructor(
    @Assisted navigator: WordCardNavigator,
) : MateNavigationEffectHandler<Msg>(navigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        // экран не имеет специфичных эффектов кроме базового Back
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: WordCardNavigator): WordCardNavigationEffectHandler
    }
}
