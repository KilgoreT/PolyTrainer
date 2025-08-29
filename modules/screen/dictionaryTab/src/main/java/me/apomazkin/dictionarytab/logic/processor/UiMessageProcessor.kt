package me.apomazkin.dictionarytab.logic.processor

import androidx.lifecycle.Lifecycle
import me.apomazkin.dictionarytab.logic.DictionaryTabState
import me.apomazkin.dictionarytab.logic.UiMsg
import me.apomazkin.mate.Effect
import me.apomazkin.mate.ReducerResult

internal fun processUiMessage(
    state: DictionaryTabState,
    message: UiMsg
): ReducerResult<DictionaryTabState, Effect> {
    return when (message) {
        is UiMsg.ShowNotification -> state
            .copy(
                snackbarState = state.snackbarState
                    .copy(title = message.message, show = message.show)
            ) to setOf()

        is UiMsg.LifecycleEvent -> {
            state to setOf()
        }
    }
}

fun Lifecycle.Event.toMateEvent(): UiMsg.LifecycleEvent.Lifecycle {
    return when (this) {
        Lifecycle.Event.ON_CREATE -> UiMsg.LifecycleEvent.Lifecycle.ON_CREATE
        Lifecycle.Event.ON_START -> UiMsg.LifecycleEvent.Lifecycle.ON_START
        Lifecycle.Event.ON_RESUME -> UiMsg.LifecycleEvent.Lifecycle.ON_RESUME
        Lifecycle.Event.ON_PAUSE -> UiMsg.LifecycleEvent.Lifecycle.ON_PAUSE
        Lifecycle.Event.ON_STOP -> UiMsg.LifecycleEvent.Lifecycle.ON_STOP
        Lifecycle.Event.ON_DESTROY -> UiMsg.LifecycleEvent.Lifecycle.ON_DESTROY
        Lifecycle.Event.ON_ANY -> UiMsg.LifecycleEvent.Lifecycle.ON_ANY
    }
}