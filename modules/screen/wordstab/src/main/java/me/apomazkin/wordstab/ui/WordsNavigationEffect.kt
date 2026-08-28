package me.apomazkin.wordstab.ui

import me.apomazkin.mate.NavigationEffect

sealed interface WordsNavigationEffect : NavigationEffect {
    data class OpenWordCard(val wordId: Long) : WordsNavigationEffect
}
