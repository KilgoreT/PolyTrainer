package me.apomazkin.ui.input

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
import me.apomazkin.ui.R
import me.apomazkin.ui.input.base.LexemeTextFieldWidget
import me.apomazkin.ui.preview.PreviewWidget

private val DEFAULT_SEND_ICON = R.drawable.ic_send

@Composable
fun PrimaryTextFieldWidget(
    modifier: Modifier = Modifier,
    @StringRes placeHolder: Int? = null,
    isSendEnabled: Boolean,
    @DrawableRes sendIconRes: Int = DEFAULT_SEND_ICON,
    isInputEnabled: Boolean = true,
    value: String,
    onValueChange: (String) -> Unit,
    onSendAction: () -> Unit,
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
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        LexemeTextFieldWidget(
            modifier = Modifier
                .weight(1f, true)
                .focusRequester(focusRequester),
            placeHolder = placeHolder,
            isInputEnabled = isInputEnabled,
            value = value,
            onValueChange = onValueChange,
            imeAction = ImeAction.Send,
            onKeyboardActions = onSendAction,
        )
        IconBoxed(
            iconRes = sendIconRes,
            colorEnabled = MaterialTheme.colorScheme.primary,
            enabled = isSendEnabled,
            onClick = onSendAction
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewText() {
    AppTheme {
        PrimaryTextFieldWidget(
            value = "this is answer",
            isSendEnabled = true,
            onValueChange = {},
            onSendAction = {},
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewEmptyText() {
    AppTheme {
        PrimaryTextFieldWidget(
            placeHolder = R.string.chat_quiz_placeholder_text,
            value = "",
            isSendEnabled = false,
            onValueChange = {},
            onSendAction = {},
        )
    }
}