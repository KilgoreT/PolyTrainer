package me.apomazkin.polytrainer.navigator

import me.apomazkin.wordcard.WordCardNavigator

class WordCardNavigatorImpl(
    private val onBack: () -> Unit,
) : WordCardNavigator {
    override fun back() = onBack()
}
