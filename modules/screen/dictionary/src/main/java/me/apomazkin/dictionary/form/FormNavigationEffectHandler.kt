package me.apomazkin.dictionary.form

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.apomazkin.mate.MateNavigationEffectHandler
import me.apomazkin.mate.NavigationEffect

class FormNavigationEffectHandler @AssistedInject constructor(
    @Assisted navigator: FormNavigator,
) : MateNavigationEffectHandler<DictionaryFormMsg>(navigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        // экран не имеет специфичных эффектов кроме базового Back
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: FormNavigator): FormNavigationEffectHandler
    }
}
