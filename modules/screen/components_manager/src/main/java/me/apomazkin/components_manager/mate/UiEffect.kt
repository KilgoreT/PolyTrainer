package me.apomazkin.components_manager.mate

import io.github.kilgoret.mate.Effect

/**
 * One-shot UI side-effects (snackbar messages). IS481 MVP — plain text strings;
 * локализация через ResourceKey/StringRes — задача UI sub-flow.
 */
sealed interface UiEffect : Effect {
    data class Snackbar(
        val text: String,
    ) : UiEffect
}
