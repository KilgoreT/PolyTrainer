package me.apomazkin.dictionary.list

import io.github.kilgoret.mate.NavigationEffect

sealed interface ListNavigationEffect : NavigationEffect {
    data object ExitApp : ListNavigationEffect
    data object OpenCreate : ListNavigationEffect
    data class OpenEdit(val id: Long) : ListNavigationEffect
}
