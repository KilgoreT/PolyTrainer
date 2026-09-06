package me.apomazkin.dictionary.form

import io.github.kilgoret.mate.Effect

sealed interface FlagFilterEffect : Effect {
    data class FilterFlags(val query: String) : FlagFilterEffect
}
