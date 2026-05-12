package me.apomazkin.quiztab

import me.apomazkin.mate.Navigator

interface QuizTabNavigator : Navigator {
    fun openChat(quizType: String)
}
