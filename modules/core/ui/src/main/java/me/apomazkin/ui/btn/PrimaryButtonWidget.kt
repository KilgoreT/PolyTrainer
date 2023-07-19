package me.apomazkin.ui.btn

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.gradientPrimary
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.base.GradientButtonWidget
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

@Composable
fun PrimaryButtonWidget(
    @StringRes titleRes: Int,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    GradientButtonWidget(
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
        PrimaryButtonWidget(
            titleRes = R.string.logo_title
        ) {}
    }
}