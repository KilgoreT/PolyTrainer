package me.apomazkin.polytrainer.navigator

import me.apomazkin.quiztab.QuizTabNavigator

class QuizTabNavigatorImpl(
    private val onOpenChat: (String) -> Unit,
) : QuizTabNavigator {
    override fun back() {
        // таб остаётся открытым
    }

    override fun openChat(quizType: String) = onOpenChat(quizType)
}
