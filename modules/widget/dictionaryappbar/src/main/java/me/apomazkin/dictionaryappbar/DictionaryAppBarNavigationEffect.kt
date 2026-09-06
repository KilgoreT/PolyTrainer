package me.apomazkin.dictionaryappbar

import io.github.kilgoret.mate.NavigationEffect

sealed interface DictionaryAppBarNavigationEffect : NavigationEffect {
    data object OpenDictionaryCreate : DictionaryAppBarNavigationEffect
    data class OpenPerDictionaryComponents(val dictionaryId: Long) : DictionaryAppBarNavigationEffect
}
