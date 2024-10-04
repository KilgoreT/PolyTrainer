@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.vocabulary.ui.widget

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu
import me.apomazkin.vocabulary.logic.AddWordDialogState
import me.apomazkin.vocabulary.logic.Msg

@Composable
internal fun AddWordBottomSheetWidget(
    state: AddWordDialogState,
    sendMessage: (Msg) -> Unit,
) {

    ModalBottomSheet(
        onDismissRequest = { sendMessage(Msg.StartAddWord(show = false)) },
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
                    sendMessage(Msg.StartAddWord(show = false))
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

@PreviewWidgetEn
@PreviewWidgetRu
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