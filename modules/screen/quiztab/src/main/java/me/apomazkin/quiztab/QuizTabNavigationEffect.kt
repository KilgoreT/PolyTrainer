package me.apomazkin.quiztab

import io.github.kilgoret.mate.NavigationEffect

sealed interface QuizTabNavigationEffect : NavigationEffect {
    data class OpenChat(val quizType: String) : QuizTabNavigationEffect
}
