package me.apomazkin.wordcard.mate

import androidx.annotation.StringRes
import me.apomazkin.mate.Effect

/**
 * One-shot UI side-effects.
 */
sealed interface UiEffect : Effect {

    /** Snackbar c action-кнопкой; при нажатии action отправляется [undoMsg]. */
    data class ShowSnackbarWithUndo(
        @StringRes val messageRes: Int,
        @StringRes val actionLabelRes: Int,
        val undoMsg: Msg,
    ) : UiEffect

    /** Snackbar без action для ошибок. */
    data class ShowErrorSnackbar(
        @StringRes val messageRes: Int,
    ) : UiEffect

    /** G4: Snackbar с retry-action; при нажатии отправляется [retryMsg]. */
    data class ShowSnackbarWithRetry(
        @StringRes val messageRes: Int,
        @StringRes val actionLabelRes: Int,
        val retryMsg: Msg,
    ) : UiEffect
}
