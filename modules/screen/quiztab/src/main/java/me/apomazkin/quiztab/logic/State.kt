package me.apomazkin.quiztab.logic

import androidx.compose.runtime.Immutable
import me.apomazkin.dictionarypicker.state.LangPickerState
import me.apomazkin.mate.EMPTY_STRING

/**
 * State
 */
@Immutable
data class QuizTabState(
    val topBarState: TopBarState = TopBarState(),
    val snackbarState: SnackbarState = SnackbarState(),
)

@Immutable
data class TopBarState(
    val langPickerState: LangPickerState? = null,
)

@Immutable
data class SnackbarState(
    val title: String = EMPTY_STRING,
    val show: Boolean = false,
)