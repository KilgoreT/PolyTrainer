package me.apomazkin.ui.btn.base

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.Typography
import me.apomazkin.theme.blackDisabledColor
import me.apomazkin.theme.clr1F10324C
import me.apomazkin.theme.gradientPrimary
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.R
import me.apomazkin.ui.preview.PreviewWidget

private const val DefaultPadding = 24
private const val DefaultShape = 12

@Composable
fun GradientButtonWidget(
    @StringRes titleRes: Int,
    enabled: Boolean = false,
    gradient: Brush,
    titleColorEnabled: Color = whiteColor,
    titleColorDisabled: Color = blackDisabledColor,
    titleStyle: TextStyle = Typography.labelLarge,
    horizontalPadding: Dp = DefaultPadding.dp,
    shape: Shape = RoundedCornerShape(DefaultShape.dp),
    onClick: () -> Unit,
) {
    val bgModifier = if (enabled) {
        Modifier.background(brush = gradient, shape = shape)
    } else {
        Modifier.background(color = clr1F10324C, shape = shape)
    }
    val titleColor = if (enabled) titleColorEnabled else titleColorDisabled
    Button(
        modifier = Modifier
            .then(bgModifier)
            .width(intrinsicSize = IntrinsicSize.Min)
            .height(ButtonDefaults.MinHeight),
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
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
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

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        GradientButtonWidget(
            titleRes = R.string.logo_title,
            gradient = gradientPrimary,
        ) {}
    }
}