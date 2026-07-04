package me.apomazkin.per_dictionary_components.mate

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.apomazkin.mate.MateNavigationEffectHandler
import me.apomazkin.mate.NavigationEffect
import me.apomazkin.per_dictionary_components.PerDictionaryComponentsNavigator

/**
 * Navigation handler — `Back` уже обработан в [MateNavigationEffectHandler],
 * screen собственных переходов не имеет (см. [PerDictionaryComponentsNavigationEffect]).
 */
class NavigationEffectHandler @AssistedInject constructor(
    @Assisted navigator: PerDictionaryComponentsNavigator,
) : MateNavigationEffectHandler<Msg>(navigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        // no-op: собственных переходов нет — Back уже обработан super-классом
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: PerDictionaryComponentsNavigator): NavigationEffectHandler
    }
}
