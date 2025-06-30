package me.apomazkin.stattab.mate

import androidx.compose.runtime.Immutable
import me.apomazkin.mate.EMPTY_STRING

/**
 * State
 */
@Immutable
data class StatisticState(
    val snackbarState: SnackbarState = SnackbarState(),
)

@Immutable
data class SnackbarState(
    val title: String = EMPTY_STRING,
    val show: Boolean = false,
)