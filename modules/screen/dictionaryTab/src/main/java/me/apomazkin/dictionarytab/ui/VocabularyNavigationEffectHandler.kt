package me.apomazkin.dictionarytab.ui

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import me.apomazkin.dictionarytab.logic.Msg
import me.apomazkin.mate.MateNavigationEffectHandler
import me.apomazkin.mate.NavigationEffect

class VocabularyNavigationEffectHandler @AssistedInject constructor(
    @Assisted private val vocabularyNavigator: VocabularyNavigator,
) : MateNavigationEffectHandler<Msg>(vocabularyNavigator) {

    override suspend fun onScreenEffect(effect: NavigationEffect) {
        when (effect) {
            is VocabularyNavigationEffect.OpenWordCard -> vocabularyNavigator.openWordCard(effect.wordId)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(navigator: VocabularyNavigator): VocabularyNavigationEffectHandler
    }
}
