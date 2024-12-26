package me.apomazkin.dictionarytab.logic.processor

import androidx.lifecycle.Lifecycle
import me.apomazkin.dictionarytab.logic.DatasourceEffect
import me.apomazkin.dictionarytab.logic.DictionaryTabState
import me.apomazkin.dictionarytab.logic.UiMsg
import me.apomazkin.mate.Effect
import me.apomazkin.mate.ReducerResult

internal fun processUiMessage(
    state: DictionaryTabState,
    message: UiMsg
): ReducerResult<DictionaryTabState, Effect> {
    return when (message) {
        is UiMsg.Snackbar -> state
            .copy(
                snackbarState = state.snackbarState
                    .copy(title = message.message, show = message.show)
            ) to setOf()

        is UiMsg.LifeCycleEvent -> {
            if (message.lifeCycle == UiMsg.LifeCycleEvent.LifeCycle.ON_START) {
                state to setOf(DatasourceEffect.LoadCurrentDict)
            } else {
                state to emptySet()
            }
        }
    }
}

fun Lifecycle.Event.toMateEvent(): UiMsg.LifeCycleEvent.LifeCycle {
    return when (this) {
        Lifecycle.Event.ON_CREATE -> UiMsg.LifeCycleEvent.LifeCycle.ON_CREATE
        Lifecycle.Event.ON_START -> UiMsg.LifeCycleEvent.LifeCycle.ON_START
        Lifecycle.Event.ON_RESUME -> UiMsg.LifeCycleEvent.LifeCycle.ON_RESUME
        Lifecycle.Event.ON_PAUSE -> UiMsg.LifeCycleEvent.LifeCycle.ON_PAUSE
        Lifecycle.Event.ON_STOP -> UiMsg.LifeCycleEvent.LifeCycle.ON_STOP
        Lifecycle.Event.ON_DESTROY -> UiMsg.LifeCycleEvent.LifeCycle.ON_DESTROY
        Lifecycle.Event.ON_ANY -> UiMsg.LifeCycleEvent.LifeCycle.ON_ANY
    }
}