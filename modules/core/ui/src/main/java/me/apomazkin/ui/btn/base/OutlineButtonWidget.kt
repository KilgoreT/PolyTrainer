package me.apomazkin.ui.btn.base

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.blackColor
import me.apomazkin.ui.R
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

private const val DefaultPadding = 24
private const val DefaultCornerRadius = 12
private const val DefaultBorderStroke = 1

@Composable
fun OutlineButtonWidget(
    @DrawableRes iconRes: Int? = null,
    @StringRes titleRes: Int,
    enabledColor: Color,
    disabledColor: Color = blackColor,
    enabled: Boolean = false,
    @StringRes contentDescription: Int = R.string.content_description_button,
    borderStroke: Int = DefaultBorderStroke,
    cornerRadius: Int = DefaultCornerRadius,
    horizontalPadding: Dp = DefaultPadding.dp,
    onClick: () -> Unit
) {
    val currentColor by remember(enabled) {
        derivedStateOf {
            if (enabled) enabledColor
            else disabledColor.copy(alpha = 0.3F)
        }
    }
    OutlinedButton(
        modifier = Modifier
            .height(ButtonDefaults.MinHeight),
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(cornerRadius.dp),
        border = BorderStroke(
            width = borderStroke.dp,
            color = currentColor
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = enabledColor,
        ),
        contentPadding = PaddingValues(horizontal = horizontalPadding),
    ) {
        iconRes?.let { resId ->
            Icon(
                painter = painterResource(id = resId),
                tint = currentColor,
                contentDescription = stringResource(id = contentDescription)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Box(
            contentAlignment = Alignment.Center,
        ) {

            Text(
                modifier = Modifier,
                text = stringResource(id = titleRes),
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = currentColor,
            )
        }
    }
}

@PreviewWidgetRu
@PreviewWidgetEn
@Composable
private fun Preview() {
    AppTheme {
        OutlineButtonWidget(
            iconRes = R.drawable.ic_delete,
            titleRes = R.string.logo_title,
            enabledColor = MaterialTheme.colorScheme.error
        ) {}
    }
}