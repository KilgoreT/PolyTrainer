package me.apomazkin.ui.btn

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.base.OutlineButtonWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun ErrorButtonWidget(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    @StringRes contentDescription: Int = titleRes,
    enabled: Boolean = false,
    onClick: () -> Unit,
) {
    OutlineButtonWidget(
        iconRes = iconRes,
        titleRes = titleRes,
        enabledColor = MaterialTheme.colorScheme.error,
        contentDescription = contentDescription,
        enabled = enabled,
        onClick = onClick,
    )
}

@PreviewWidget
@Composable
private fun PreviewEnable() {
    AppTheme {
        ErrorButtonWidget(
            iconRes = R.drawable.ic_delete,
            titleRes = R.string.logo_title,
            enabled = true,
        ) {}
    }
}

@PreviewWidget
@Composable
private fun PreviewDisable() {
    AppTheme {
        ErrorButtonWidget(
            iconRes = R.drawable.ic_delete,
            titleRes = R.string.logo_title,
            enabled = false,
        ) {}
    }
}