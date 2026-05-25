package me.apomazkin.ui.text.base

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

private val defaultTextStyle = LexemeStyle.BodyL

/**
 * Inline editable text: тап по тексту → edit-mode, потеря фокуса → [onFocusLost].
 *
 * @param originValue текст в view-mode.
 * @param changedValue текст в edit-mode.
 * @param isEditMode переключает view/edit.
 * @param onTextChange изменение текста в edit-mode.
 * @param onOpenEditMode тап по тексту в view-mode.
 * @param textColor цвет текста.
 * @param textStyle стиль текста.
 * @param onFocusLost потеря фокуса в edit-mode с текущим значением.
 */
@Composable
fun LexemeEditableText(
    originValue: String,
    changedValue: String,
    isEditMode: Boolean,
    onTextChange: (String) -> Unit,
    onOpenEditMode: () -> Unit,
    onFocusLost: (currentValue: String) -> Unit,
    textColor: Color,
    textStyle: TextStyle = defaultTextStyle,
) {

    val focusRequester = remember { FocusRequester() }
    DisposableEffect(isEditMode) {
        if (isEditMode) focusRequester.requestFocus()
        onDispose {}
    }

    if (isEditMode) {
        var fieldValue by remember {
            mutableStateOf(TextFieldValue(changedValue, selection = TextRange(changedValue.length)))
        }
        LaunchedEffect(changedValue) {
            if (changedValue != fieldValue.text) {
                val oldOffset = fieldValue.selection.start
                fieldValue = TextFieldValue(
                    text = changedValue,
                    selection = TextRange(minOf(oldOffset, changedValue.length))
                )
            }
        }
        var hadFocus by remember { mutableStateOf(false) }

        BasicTextField(
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        hadFocus = true
                    } else if (hadFocus) {
                        onFocusLost(fieldValue.text)
                    }
                },
            value = fieldValue,
            onValueChange = { newValue ->
                fieldValue = newValue
                if (newValue.text != changedValue) onTextChange(newValue.text)
            },
            textStyle = textStyle.copy(color = textColor),
        )
    } else {
        Text(
            modifier = Modifier.clickable { onOpenEditMode.invoke() },
            text = originValue,
            color = textColor,
            style = textStyle,
            lineHeight = TextUnit(value = 280f, TextUnitType.Unspecified),
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewLexemeEditableText(
    @PreviewParameter(BoolParam::class) isEditMode: Boolean,
) {
    AppTheme {
        Box(
            modifier = Modifier
                .background(color = whiteColor),
            contentAlignment = Alignment.Center,
        ) {
            LexemeEditableText(
                originValue = "Lexeme text",
                changedValue = "Lexeme text 111",
                isEditMode = isEditMode,
                textColor = blackColor,
                onTextChange = {},
                onOpenEditMode = {},
                onFocusLost = {},
            )
        }
    }
}
