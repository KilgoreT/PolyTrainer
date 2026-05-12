package me.apomazkin.stattab

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.apomazkin.mate.MateNavigationEffectHandler
import me.apomazkin.mate.NavigationEffect
import me.apomazkin.stattab.mate.Msg

class StatisticNavigationEffectHandler @AssistedInject constructor(
    @Assisted navigator: StatisticNavigator,
) : MateNavigationEffectHandler<Msg>(navigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        // нет специфичных эффектов кроме базового Back
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: StatisticNavigator): StatisticNavigationEffectHandler
    }
}
