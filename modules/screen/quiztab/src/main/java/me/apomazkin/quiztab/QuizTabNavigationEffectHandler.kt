package me.apomazkin.quiztab

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.kilgoret.mate.MateNavigationEffectHandler
import io.github.kilgoret.mate.NavigationEffect
import me.apomazkin.quiztab.logic.Msg

class QuizTabNavigationEffectHandler @AssistedInject constructor(
    @Assisted private val quizTabNavigator: QuizTabNavigator,
) : MateNavigationEffectHandler<Msg>(quizTabNavigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        when (effect) {
            is QuizTabNavigationEffect.OpenChat -> quizTabNavigator.openChat(effect.quizType)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: QuizTabNavigator): QuizTabNavigationEffectHandler
    }
}
