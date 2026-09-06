package me.apomazkin.dictionary.form

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.kilgoret.mate.MateNavigationEffectHandler
import io.github.kilgoret.mate.NavigationEffect

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
