package me.apomazkin.wordstab.ui.widget.addWordBottom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.panel.InputDockedPanelWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordstab.logic.AddWordDialogState
import me.apomazkin.wordstab.logic.Msg

/**
 * Панель добавления/правки слова. Вёрстка — общий [InputDockedPanelWidget]
 * (IS496: немодальная, фон живой — фильтр скроллится, слова кликабельны);
 * Msg-логика — здесь.
 */
@Composable
internal fun AddWordPanelWidget(
    state: AddWordDialogState,
    sendMessage: (Msg) -> Unit,
    modifier: Modifier = Modifier,
) {
    InputDockedPanelWidget(
        modifier = modifier,
        value = state.wordValue,
        isSendEnabled = state.wordValue.isNotBlank(),
        onValueChange = { sendMessage(Msg.UpdateWordInput(it)) },
        onSendAction = {
            val value = state.wordValue
            if (value.trim().isBlank()) {
                sendMessage(Msg.CloseAddWordDialog)
                return@InputDockedPanelWidget
            }
            if (state.wordId != null) {
                sendMessage(Msg.UpdateWord(state.wordId, value.trim()))
            } else {
                sendMessage(Msg.CreateWord(value.trim()))
            }
        },
        onDismissRequest = { sendMessage(Msg.CloseAddWordDialog) },
    )
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        AddWordPanelWidget(
            state = AddWordDialogState(
                isOpen = true
            ),
            sendMessage = {},
        )
    }
}
