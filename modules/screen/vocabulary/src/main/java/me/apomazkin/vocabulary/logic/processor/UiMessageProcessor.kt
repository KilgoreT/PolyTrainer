package me.apomazkin.vocabulary.logic.processor

import me.apomazkin.mate.Effect
import me.apomazkin.mate.ReducerResult
import me.apomazkin.vocabulary.logic.UiMsg
import me.apomazkin.vocabulary.logic.VocabularyTabState

internal fun processUiMessage(
    state: VocabularyTabState,
    message: UiMsg
): ReducerResult<VocabularyTabState, Effect> {
    return when (message) {
        is UiMsg.Snackbar -> state
            .copy(
                snackbarState = state.snackbarState
                    .copy(title = message.message, show = message.show)
            ) to setOf()
    }
}