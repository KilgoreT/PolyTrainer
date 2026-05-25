package me.apomazkin.wordcard.widget.internal

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import me.apomazkin.wordcard.deps.UiHost

internal class UiHostImpl(
    private val snackbarHostState: SnackbarHostState,
    private val context: Context,
) : UiHost {

    override suspend fun showSnackbar(@StringRes messageRes: Int) {
        snackbarHostState.showSnackbar(context.getString(messageRes))
    }

    override suspend fun showSnackbarWithAction(
        @StringRes messageRes: Int,
        @StringRes actionLabelRes: Int,
    ): Boolean {
        // M3 default для snackbar с action — Indefinite; перебиваем на Short.
        val result = snackbarHostState.showSnackbar(
            message = context.getString(messageRes),
            actionLabel = context.getString(actionLabelRes),
            duration = SnackbarDuration.Short,
        )
        return result == SnackbarResult.ActionPerformed
    }
}
