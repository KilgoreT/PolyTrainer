package me.apomazkin.ui.btn.base

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.R
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

private const val DEFAULT_HORIZONTAL_PADDING = 16
private const val DEFAULT_ROUNDED_CORNER = 12

@Composable
fun BrandButtonWidget(
    @StringRes titleRes: Int,
    horizontalPadding: Int = DEFAULT_HORIZONTAL_PADDING,
    enabledColor: Color = MaterialTheme.colorScheme.primary,
    disabledColor: Color = MaterialTheme.colorScheme.secondary,
    titleTextColor: Color = MaterialTheme.colorScheme.onPrimary,
    titleTextStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    enabled: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding.dp)
            .height(ButtonDefaults.MinHeight),
        colors = ButtonDefaults.buttonColors(
            containerColor = enabledColor,
            disabledContainerColor = disabledColor,
        ),
        contentPadding = PaddingValues(),
        shape = RoundedCornerShape(DEFAULT_ROUNDED_CORNER.dp),
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
                style = titleTextStyle,
                color = titleTextColor,
            )
        }
    }
}

@PreviewWidgetRu
@PreviewWidgetEn
@Composable
private fun Preview() {
    AppTheme {
        BrandButtonWidget(
            titleRes = R.string.logo_title,
            enabled = true,
        ) {}
    }
}