package me.apomazkin.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

private const val DEFAULT_WIDTH = 180

@Composable
fun PrimaryButtonWidget(
    label: String,
    enabled: Boolean = true,
    width: Int = DEFAULT_WIDTH,
    shape: Shape = MaterialTheme.shapes.small,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier
            .width(width.dp),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface
                .copy(alpha = 0.12F),
            disabledContentColor = MaterialTheme.colorScheme.onBackground
                .copy(alpha = 0.12F),
        ),
        onClick = onClick
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
@PreviewWidget
private fun PreviewEnable() {
    AppTheme {
        PrimaryButtonWidget(
            label = "Enable"
        ) {}
    }
}

@Composable
@PreviewWidget
private fun PreviewDisable() {
    AppTheme {
        PrimaryButtonWidget(
            label = "Disable",
            enabled = false,
        ) {}
    }
}