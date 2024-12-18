package me.apomazkin.ui.text.base

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.R
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

private const val DEFAULT_SPACE_BEFORE_ICON = 8
private const val DEFAULT_ICON_SIZE = 12
private val defaultTextStyle = LexemeStyle.BodyL

@Composable
fun LexemeEditableText(
    originValue: String,
    changedValue: String,
    isEditMode: Boolean,
    onTextChange: (String) -> Unit,
    onOpenEditMode: () -> Unit,
    onCloseEditMode: () -> Unit,
    textColor: Color,
    textStyle: TextStyle = defaultTextStyle,
    @DrawableRes iconClose: Int = R.drawable.ic_close,
    @DrawableRes iconOpen: Int? = null,
    iconSize: Int = DEFAULT_ICON_SIZE,
    spaceBeforeIcon: Int = DEFAULT_SPACE_BEFORE_ICON,
) {

    val focusRequester = remember { FocusRequester() }
    DisposableEffect(isEditMode) {
        if (isEditMode) focusRequester.requestFocus()
        onDispose {}
    }

    Box(
        modifier = Modifier,
    ) {
        if (isEditMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement
                    .spacedBy(
                        space = spaceBeforeIcon.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
            ) {
                BasicTextField(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .width(IntrinsicSize.Min)
                        .focusRequester(focusRequester),
                    value = changedValue,
                    onValueChange = onTextChange,
                    textStyle = textStyle.copy(color = textColor),
                )
                //TODO kilg 04.11.2024 08:00 если понадобится работа с курсором
//                BasicTextField(
//                    modifier = Modifier
//                        .weight(1f, fill = false)
//                        .width(IntrinsicSize.Min)
//                        .focusRequester(focusRequester),
//                    value = TextFieldValue(changedValue, selection = TextRange.Zero),
//                    onValueChange = { value: TextFieldValue -> onTextChange(value.text) },
//                    textStyle = textStyle.copy(color = textColor),
//                )
                IconBoxed(
                    modifier = Modifier,
                    iconRes = iconClose,
                    size = iconSize,
                    enabled = true,
                    colorEnabled = blackColor,
                ) { onCloseEditMode.invoke() }
            }
        } else {
            Row(
                modifier = Modifier
                    .clickable { onOpenEditMode.invoke() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = DEFAULT_SPACE_BEFORE_ICON.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                Text(
                    modifier = Modifier,
                    text = originValue,
                    color = textColor,
                    style = textStyle,
                    lineHeight = TextUnit(value = 280f, TextUnitType.Unspecified),
                )
                iconOpen?.let { icon ->
                    IconBoxed(
                        iconRes = icon,
                        size = DEFAULT_ICON_SIZE,
                        enabled = true,
                        colorEnabled = blackColor,
                    ) {}
                }
            }
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewPrimaryLongFabWidget(
    @PreviewParameter(BoolParam::class) enabled: Boolean
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
                isEditMode = enabled,
                textColor = blackColor,
                onTextChange = {},
                onOpenEditMode = {},
                onCloseEditMode = {},
            )
        }
    }
}