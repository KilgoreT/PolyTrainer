package me.apomazkin.wordstab.ui

import io.github.kilgoret.mate.NavigationEffect

sealed interface WordsNavigationEffect : NavigationEffect {
    data class OpenWordCard(val wordId: Long) : WordsNavigationEffect

    /** Открыть экран лексем-кандидатов со снимка; [uri] — файл снимка строкой. */
    data class OpenScanCandidates(val uri: String) : WordsNavigationEffect
}
