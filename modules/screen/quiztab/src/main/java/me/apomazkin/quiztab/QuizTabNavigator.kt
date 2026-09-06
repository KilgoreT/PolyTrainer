package me.apomazkin.quiztab

import io.github.kilgoret.mate.Navigator

interface QuizTabNavigator : Navigator {
    fun openChat(quizType: String)
}
