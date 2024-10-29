package me.apomazkin.ui.btn

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.disableButtonTitleColor
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.base.LexemeTextButton
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun PrimaryTextButtonWidget(
    @StringRes title: Int,
    enabled: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    disabledContentColor: Color = disableButtonTitleColor,
    onClick: () -> Unit
) {
    LexemeTextButton(
        title = title,
        enabled = enabled,
        contentColor = contentColor,
        disabledContentColor = disabledContentColor,
        onClick = onClick,
    )
}

@Composable
@PreviewWidget
private fun PreviewEnabled() {
    AppTheme {
        PrimaryTextButtonWidget(
            title = R.string.logo_title,
            enabled = true,
        ) {}
    }
}

@Composable
@PreviewWidget
private fun PreviewDisabled() {
    AppTheme {
        PrimaryTextButtonWidget(
            title = R.string.logo_title,
            enabled = false,
        ) {}
    }
}