package me.apomazkin.quiztab

import me.apomazkin.mate.NavigationEffect

sealed interface QuizTabNavigationEffect : NavigationEffect {
    data class OpenChat(val quizType: String) : QuizTabNavigationEffect
}
