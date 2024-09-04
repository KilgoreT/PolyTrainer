package me.apomazkin.ui.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun LifecycleEventHandler(
    action: (phase: Event) -> Unit,
) {
    val lifecycleOwnerState = rememberUpdatedState(LocalLifecycleOwner.current)
    DisposableEffect(lifecycleOwnerState.value) {
        val lifecycle = lifecycleOwnerState.value.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            action.invoke(event)
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }
}