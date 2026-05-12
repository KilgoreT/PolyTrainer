package me.apomazkin.polytrainer.navigator

import me.apomazkin.quiz.chat.ChatNavigator

class ChatNavigatorImpl(
    private val onBack: () -> Unit,
) : ChatNavigator {
    override fun back() = onBack()
}
