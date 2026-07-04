package me.apomazkin.components_manager.mate

import me.apomazkin.mate.Effect

/**
 * One-shot UI side-effects (snackbar messages). IS481 MVP — plain text strings;
 * локализация через ResourceKey/StringRes — задача UI sub-flow.
 */
sealed interface UiEffect : Effect {
    data class Snackbar(val text: String) : UiEffect
}
