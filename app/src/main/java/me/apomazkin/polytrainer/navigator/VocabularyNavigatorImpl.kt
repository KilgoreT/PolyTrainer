package me.apomazkin.polytrainer.navigator

import me.apomazkin.dictionarytab.ui.VocabularyNavigator

class VocabularyNavigatorImpl(
    private val onOpenWordCard: (Long) -> Unit,
) : VocabularyNavigator {
    override fun back() {
        // таб остаётся открытым — back не нужен
    }

    override fun openWordCard(wordId: Long) = onOpenWordCard(wordId)
}
