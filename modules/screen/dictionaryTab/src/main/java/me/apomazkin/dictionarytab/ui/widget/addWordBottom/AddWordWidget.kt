package me.apomazkin.dictionarytab.ui.widget.addWordBottom

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.input.PrimaryTextFieldWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun AddWordWidget(
    wordValue: String,
    isActionEnable: Boolean,
    onWordValueChange: (String) -> Unit,
    onAddWord: (String) -> Unit,
) {
    PrimaryTextFieldWidget(
        modifier = Modifier,
        isSendEnabled = isActionEnable,
        value = wordValue,
        onValueChange = onWordValueChange,
        onSendAction = { onAddWord.invoke(wordValue) },
    )
}

@PreviewWidget
@Composable
private fun PreviewDisabled() {
    AppTheme {
        AddWordWidget(
            wordValue = "value",
            isActionEnable = false,
            onWordValueChange = {},
            onAddWord = {}
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewEnabled() {
    AppTheme {
        AddWordWidget(
            wordValue = "value",
            isActionEnable = true,
            onWordValueChange = {},
            onAddWord = {}
        )
    }
}