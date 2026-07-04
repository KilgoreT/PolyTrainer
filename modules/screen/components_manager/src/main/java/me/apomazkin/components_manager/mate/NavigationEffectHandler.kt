package me.apomazkin.components_manager.mate

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.apomazkin.components_manager.ComponentsManagerNavigator
import me.apomazkin.mate.MateNavigationEffectHandler
import me.apomazkin.mate.NavigationEffect

/**
 * Navigation handler — `Back` уже обработан в [MateNavigationEffectHandler],
 * screen собственных переходов не имеет (см. [ComponentsManagerNavigationEffect]).
 */
class NavigationEffectHandler @AssistedInject constructor(
    @Assisted navigator: ComponentsManagerNavigator,
) : MateNavigationEffectHandler<Msg>(navigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        // no-op: собственных переходов нет — Back уже обработан super-классом
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: ComponentsManagerNavigator): NavigationEffectHandler
    }
}
