package me.apomazkin.stattab

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.kilgoret.mate.MateNavigationEffectHandler
import io.github.kilgoret.mate.NavigationEffect
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
