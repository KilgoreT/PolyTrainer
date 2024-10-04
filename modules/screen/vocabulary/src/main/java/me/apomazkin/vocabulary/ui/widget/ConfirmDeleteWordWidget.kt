package me.apomazkin.vocabulary.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.btn.AlarmButtonWidget
import me.apomazkin.ui.btn.CancelButtonWidget
import me.apomazkin.ui.preview.PreviewScreenEn
import me.apomazkin.ui.preview.PreviewScreenRu
import me.apomazkin.vocabulary.R
import me.apomazkin.vocabulary.logic.ConfirmWordDeleteDialogState
import me.apomazkin.vocabulary.logic.Msg

@Composable
fun ConfirmDeleteWordWidget(
    state: ConfirmWordDeleteDialogState,
    sendMessage: (Msg) -> Unit,
) {
    Dialog(
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = false,
        ),
        onDismissRequest = {
            sendMessage(
                Msg.ConfirmDeleteWordDialog(
                    isOpen = false,
                    wordIds = state.wordIds,
                )
            )
        }
    ) {
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
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
                )
                Row(
                    modifier = Modifier
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CancelButtonWidget {
                        sendMessage(
                            Msg.ConfirmDeleteWordDialog(
                                isOpen = false,
                                wordIds = state.wordIds
                            )
                        )
                    }
                    AlarmButtonWidget(titleRes = R.string.button_delete) {
                        sendMessage(Msg.DeleteWord(wordIds = state.wordIds))
                    }
                }
            }
        }
    }
}

@PreviewScreenRu
@PreviewScreenEn
@Composable
private fun Preview() {
    AppTheme {
        ConfirmDeleteWordWidget(
            state = ConfirmWordDeleteDialogState(),
            sendMessage = {},
        )
    }
}