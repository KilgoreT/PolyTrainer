package me.apomazkin.vocabulary.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.TextFieldWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.vocabulary.R

@Composable
fun AddWordWidget(
    wordValue: String,
    isActionEnable: Boolean,
    onWordValueChange: (String) -> Unit,
    onAddWord: (String) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    DisposableEffect(Unit) {
        onDispose {
            keyboardController?.hide()
        }
    }
    Row(
        modifier = Modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        TextFieldWidget(
            modifier = Modifier
                .weight(1f, true)
                .focusRequester(focusRequester),
            value = wordValue,
            onValueChange = onWordValueChange,
            imeAction = ImeAction.Send,
            onKeyboardActions = { onAddWord.invoke(wordValue) },
        )
        IconBoxed(
            iconRes = R.drawable.ic_send,
            colorEnabled = MaterialTheme.colorScheme.primary,
            enabled = isActionEnable,
        ) {
            onAddWord.invoke(wordValue)
        }
    }
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