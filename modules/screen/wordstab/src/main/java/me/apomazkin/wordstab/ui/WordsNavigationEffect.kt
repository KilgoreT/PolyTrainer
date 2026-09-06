package me.apomazkin.wordstab.ui

import io.github.kilgoret.mate.NavigationEffect

sealed interface WordsNavigationEffect : NavigationEffect {
    data class OpenWordCard(val wordId: Long) : WordsNavigationEffect
}
