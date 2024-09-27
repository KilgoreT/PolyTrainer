package me.apomazkin.wordcard.widget

import androidx.compose.runtime.Composable
import me.apomazkin.ui.TextFieldWidget
import me.apomazkin.wordcard.R
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.WordState

@Composable
fun EditWordDialogWidget(
    state: WordState,
    sendMessage: (Msg) -> Unit,
) {
    if (state.isEdit) {
        DialogWidget(
            titleRes = R.string.word_card_edit_word,
            onClick = { sendMessage(Msg.SaveWordValue) },
            onClickEnabled = state.edited.isNotBlank(),
            onDismiss = { sendMessage(Msg.CloseEditWord) },
            onDismissEnabled = true
        ) {
            TextFieldWidget(
                value = state.edited,
                onValueChange = { sendMessage(Msg.ChangeWordValue(it)) },
                onKeyboardActions = { sendMessage(Msg.SaveWordValue) }
            )
        }
    }
}