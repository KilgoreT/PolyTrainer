package me.apomazkin.vocabulary.ui.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.dialog.AlarmDialogWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.vocabulary.R
import me.apomazkin.vocabulary.logic.ConfirmWordDeleteDialogState
import me.apomazkin.vocabulary.logic.Msg

@Composable
fun ConfirmDeleteWordWidget(
    state: ConfirmWordDeleteDialogState,
    sendMessage: (Msg) -> Unit,
) {
    AlarmDialogWidget(
        alarmButtonText = R.string.button_delete,
        onAlarmClick = { sendMessage(Msg.DeleteWord(wordIds = state.wordIds)) },
        onDismissRequest = {
            sendMessage(
                Msg.ConfirmDeleteWordDialog(
                    isOpen = false,
                    wordIds = state.wordIds
                )
            )
        }
    ) {
        Text(
            text = stringResource(
                id = if (state.wordIds.size > 1) {
                    R.string.vocabulary_delete_words
                } else {
                    R.string.vocabulary_delete_word
                }
            ),
            style = LexemeStyle.H6,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        ConfirmDeleteWordWidget(
            state = ConfirmWordDeleteDialogState(),
            sendMessage = {},
        )
    }
}