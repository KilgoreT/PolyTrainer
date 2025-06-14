@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.dictionarytab.ui.widget.addWordBottom

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionarytab.logic.AddWordDialogState
import me.apomazkin.dictionarytab.logic.Msg
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun AddWordBottomSheetWidget(
        state: AddWordDialogState,
        sendMessage: (Msg) -> Unit,
) {

    ModalBottomSheet(
            onDismissRequest = { sendMessage(Msg.HideAddWordDialog) },
            modifier = Modifier,
            containerColor = MaterialTheme.colorScheme.onPrimary,
            dragHandle = {},
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
    ) {
        AddWordWidget(
                wordValue = state.wordValue,
                isActionEnable = state.wordValue.isNotBlank(),
                onWordValueChange = { sendMessage(Msg.WordValueChange(it)) },
                onAddWord = {
                    if (it.trim().isBlank()) {
                        sendMessage(Msg.HideAddWordDialog)
                        return@AddWordWidget
                    }
                    if (state.wordId != null) {
                        sendMessage(Msg.ChangeWord(state.wordId, state.wordValue.trim()))
                    } else {
                        sendMessage(Msg.AddWord(state.wordValue.trim()))
                    }
                },
        )
    }
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