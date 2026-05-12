package me.apomazkin.dictionary.form

import me.apomazkin.mate.Effect

sealed interface FlagFilterEffect : Effect {
    data class FilterFlags(val query: String) : FlagFilterEffect
}
