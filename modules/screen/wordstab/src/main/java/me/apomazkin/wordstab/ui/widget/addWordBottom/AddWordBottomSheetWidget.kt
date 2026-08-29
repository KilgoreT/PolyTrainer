package me.apomazkin.wordstab.ui.widget.addWordBottom

import androidx.compose.runtime.Composable
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.ui.sheet.InputBottomSheetWidget
import me.apomazkin.wordstab.logic.AddWordDialogState
import me.apomazkin.wordstab.logic.Msg

/**
 * Шторка добавления/правки слова. Вёрстка — общий [InputBottomSheetWidget]
 * (IS493 Э3: единая шторка words/groups); Msg-логика — здесь.
 */
@Composable
internal fun AddWordBottomSheetWidget(
        state: AddWordDialogState,
        sendMessage: (Msg) -> Unit,
) {
    InputBottomSheetWidget(
            value = state.wordValue,
            isSendEnabled = state.wordValue.isNotBlank(),
            onValueChange = { sendMessage(Msg.UpdateWordInput(it)) },
            onSendAction = {
                val value = state.wordValue
                if (value.trim().isBlank()) {
                    sendMessage(Msg.CloseAddWordDialog)
                    return@InputBottomSheetWidget
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
        AddWordBottomSheetWidget(
                state = AddWordDialogState(
                        isOpen = true
                ),
        ) {}
    }
}
