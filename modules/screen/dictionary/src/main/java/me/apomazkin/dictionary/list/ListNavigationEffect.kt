package me.apomazkin.dictionary.list

import me.apomazkin.mate.NavigationEffect

sealed interface ListNavigationEffect : NavigationEffect {
    data class OpenEdit(val id: Long) : ListNavigationEffect
}
