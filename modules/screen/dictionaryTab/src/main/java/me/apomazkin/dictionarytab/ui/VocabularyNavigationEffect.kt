package me.apomazkin.dictionarytab.ui

import me.apomazkin.mate.NavigationEffect

sealed interface VocabularyNavigationEffect : NavigationEffect {
    data class OpenWordCard(val wordId: Long) : VocabularyNavigationEffect
}
