package me.apomazkin.wordstab.ui

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.apomazkin.wordstab.logic.Msg
import io.github.kilgoret.mate.MateNavigationEffectHandler
import io.github.kilgoret.mate.NavigationEffect

class WordsNavigationEffectHandler @AssistedInject constructor(
    @Assisted private val vocabularyNavigator: WordsNavigator,
) : MateNavigationEffectHandler<Msg>(vocabularyNavigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        when (effect) {
            is WordsNavigationEffect.OpenWordCard -> vocabularyNavigator.openWordCard(effect.wordId)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: WordsNavigator): WordsNavigationEffectHandler
    }
}
