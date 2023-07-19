package me.apomazkin.ui.btn

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.gradientSecondaryHorizontal
import me.apomazkin.ui.R
import me.apomazkin.ui.btn.base.GradientFabWidget
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

/**
 * FAB with secondary horizontal gradient.
 */
@Composable
fun SecondaryFabWidget(
    @DrawableRes iconRes: Int,
    enabled: Boolean = true,
    gradient: Brush = gradientSecondaryHorizontal,
    onClick: () -> Unit,
) {
    GradientFabWidget(
        iconRes = iconRes,
        enabled = enabled,
        gradient = gradient,
        onClick = onClick,
    )
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun Preview() {
    AppTheme {
        SecondaryFabWidget(
            iconRes = R.drawable.ic_add,
            gradient = gradientSecondaryHorizontal
        ) {}
    }
}