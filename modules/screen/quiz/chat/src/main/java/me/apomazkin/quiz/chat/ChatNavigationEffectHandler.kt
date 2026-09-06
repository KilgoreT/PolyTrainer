package me.apomazkin.quiz.chat

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.kilgoret.mate.MateNavigationEffectHandler
import io.github.kilgoret.mate.NavigationEffect
import me.apomazkin.quiz.chat.logic.Msg

class ChatNavigationEffectHandler @AssistedInject constructor(
    @Assisted navigator: ChatNavigator,
) : MateNavigationEffectHandler<Msg>(navigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        // нет специфичных эффектов кроме базового Back
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: ChatNavigator): ChatNavigationEffectHandler
    }
}
