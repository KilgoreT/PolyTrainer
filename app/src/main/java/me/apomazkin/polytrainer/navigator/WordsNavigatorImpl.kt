package me.apomazkin.polytrainer.navigator

import me.apomazkin.wordstab.ui.WordsNavigator

class WordsNavigatorImpl(
    private val onOpenWordCard: (Long) -> Unit,
) : WordsNavigator {
    override fun back() {
        // таб остаётся открытым — back не нужен
    }

    override fun openWordCard(wordId: Long) = onOpenWordCard(wordId)
}
