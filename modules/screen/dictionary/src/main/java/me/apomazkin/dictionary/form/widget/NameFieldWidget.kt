package me.apomazkin.dictionary.form.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.apomazkin.dictionary.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Material-поле имени словаря: плавающий uppercase-лейбл, underline 2dp,
 * акцентный курсор (Figma 5027:1131-1134).
 */
@Composable
internal fun NameFieldWidget(
    value: String,
    onValueChange: (String) -> Unit,
    @StringRes labelRes: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = stringResource(id = labelRes).uppercase(),
            style = LexemeStyle.BodySBold.copy(letterSpacing = 0.3.sp),
            color = LexemeColor.primary,
        )
        Spacer(modifier = Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LexemeStyle.H6.copy(color = LexemeColor.secondary),
            cursorBrush = SolidColor(LexemeColor.primary),
        )
        Spacer(modifier = Modifier.height(6.dp))
        HorizontalDivider(
            thickness = 2.dp,
            color = LexemeColor.primary,
        )
    }
}

@Composable
@PreviewWidget
private fun PreviewFilled() {
    AppTheme {
        NameFieldWidget(
            value = "Английский",
            onValueChange = {},
            labelRes = R.string.dictionary_name_hint,
        )
    }
}

@Composable
@PreviewWidget
private fun PreviewEmpty() {
    AppTheme {
        NameFieldWidget(
            value = "",
            onValueChange = {},
            labelRes = R.string.dictionary_name_hint,
        )
    }
}
