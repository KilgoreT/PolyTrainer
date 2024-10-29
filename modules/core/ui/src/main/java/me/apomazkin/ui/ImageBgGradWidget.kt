package me.apomazkin.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.gradientSecondaryVertical
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Widget for background with Image and gradient.
 */
@Deprecated(message = "Not used")
@Composable
fun ImageBgGradWidget(
    @DrawableRes imageRes: Int,
    brush: Brush,
    content: @Composable BoxScope.() -> Unit,
) {
    SystemBarsWidget()
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Image(
                painter = painterResource(id = imageRes),
                contentScale = ContentScale.FillWidth,
                contentDescription = ""
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
                    .background(
                        brush = brush
                    )
            )
        }
        content()
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        ImageBgGradWidget(
            imageRes = R.drawable.example_ic_flag_gb,
            brush = gradientSecondaryVertical
        ) {}
    }
}