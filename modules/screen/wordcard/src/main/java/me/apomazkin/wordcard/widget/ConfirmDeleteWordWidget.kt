package me.apomazkin.wordcard.widget

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor
import me.apomazkin.ui.dialog.AlarmDialogWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordcard.R
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.WordState

@Composable
fun ConfirmDeleteWordWidget(
    state: WordState,
    sendMessage: (Msg) -> Unit,
) {
    AlarmDialogWidget(
        alarmButtonText = R.string.button_delete,
        onAlarmClick = {
            sendMessage.invoke(Msg.DeleteWord(state.id))
        },
        onDismissRequest = {
            sendMessage.invoke(Msg.HideDeleteWordDialog)
        }
    ) {
        Text(
            text = stringResource(
                id = R.string.vocabulary_delete_word
            ),
            style = LexemeStyle.H6,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.word_card_delete_word_subheading),
            style = LexemeStyle.BodyL,
            color = grayTextColor
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        ConfirmDeleteWordWidget(
            state = WordState(),
            sendMessage = {},
        )
    }
}