@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.ui.btn.base

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.White
import me.apomazkin.theme.gradientPrimary
import me.apomazkin.ui.R
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

private const val DefaultCornerRadius = 16

/**
 * FAB Widget with gradient background.
 */
@Composable
fun GradientFabWidget(
    @DrawableRes iconRes: Int,
    gradient: Brush,
    enabled: Boolean = true,
    iconColor: Color = White,
    cornerRadius: Int = DefaultCornerRadius,
    contentDescription: String = "Floating action button",
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(56.dp)
            .background(
                brush = gradient,
                shape = RoundedCornerShape(cornerRadius.dp)
            ),
        enabled = enabled,
        shape = RoundedCornerShape(cornerRadius.dp),
        color = Color.Transparent,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                painter = painterResource(id = iconRes),
                tint = iconColor,
                contentDescription = contentDescription,
            )
        }
    }
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun Preview() {
    AppTheme {
        GradientFabWidget(
            iconRes = R.drawable.ic_send,
            gradient = gradientPrimary
        ) {}
    }
}