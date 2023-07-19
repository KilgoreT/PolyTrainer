package me.apomazkin.wordcard.widget

import androidx.compose.runtime.Composable
import me.apomazkin.wordcard.R
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.WordState

@Composable
fun DeleteWordWarningDialog(
    state: WordState,
    sendMessage: (Msg) -> Unit,
) {
    if (state.showWarningDialog) {
        DialogWidget(
            titleRes = R.string.word_card_delete_warning_dialog_title,
            onClickEnabled = true,
            onDismissEnabled = true,
            onClick = { sendMessage.invoke(Msg.DeleteWord(state.id)) },
            onDismiss = { sendMessage.invoke(Msg.HideDeleteWordDialog) },
        )
    }
}