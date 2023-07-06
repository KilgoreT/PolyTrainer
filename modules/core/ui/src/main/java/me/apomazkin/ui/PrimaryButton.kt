package me.apomazkin.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.gradientPrimary
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

@Composable
fun PrimaryButton(
    @StringRes titleRes: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    GradientButton(
        titleRes = titleRes,
        gradient = gradientPrimary,
        enabled = enabled,
        onClick = onClick
    )
}

@PreviewWidgetRu
@PreviewWidgetEn
@Composable
private fun Preview() {
    AppTheme {
        PrimaryButton(
            titleRes = R.string.logo_title
        ) {}
    }
}