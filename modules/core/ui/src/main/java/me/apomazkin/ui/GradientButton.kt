package me.apomazkin.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.*
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

private const val defaultPadding = 24
private const val defaultShape = 16

@Composable
fun GradientButton(
    @StringRes titleRes: Int,
    enabled: Boolean = true,
    gradient: Brush,
    titleColorEnabled: Color = White,
    titleColorDisabled: Color = BlackDisabled,
    titleStyle: TextStyle = Typography.labelLarge,
    horizontalPadding: Dp = defaultPadding.dp,
    shape: Shape = RoundedCornerShape(defaultShape.dp),
    onClick: () -> Unit,
) {
    val modifier = if (enabled) {
        Modifier
            .padding(horizontal = horizontalPadding)
            .height(ButtonDefaults.MinHeight)
            .background(
                brush = gradient,
                shape = shape
            )
    } else {
        Modifier
            .padding(horizontal = horizontalPadding)
            .height(ButtonDefaults.MinHeight)
            .background(
                color = clr1F10324C,
                shape = shape
            )
    }
    val titleColor = if (enabled) titleColorEnabled else titleColorDisabled
    Button(
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        contentPadding = PaddingValues(),
        shape = shape,
        enabled = enabled,
        onClick = onClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = titleRes),
                style = titleStyle,
                color = titleColor
            )
        }
    }
}

@PreviewWidgetRu
@PreviewWidgetEn
@Composable
private fun Preview() {
    AppTheme {
        GradientButton(
            titleRes = R.string.logo_title,
            gradient = gradientPrimary,
        ) {}
    }
}