package me.apomazkin.wordcard.widget

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import me.apomazkin.wordcard.mate.SnackbarState

@Composable
fun SnackbarLaunchEffect(
    snackState: SnackbarState,
    host: SnackbarHostState,
    onResetState: () -> Unit
) {
    LaunchedEffect(snackState.show) {
        if (snackState.show) {
            host.showSnackbar(snackState.title).also { onResetState.invoke() }
        }
    }
}