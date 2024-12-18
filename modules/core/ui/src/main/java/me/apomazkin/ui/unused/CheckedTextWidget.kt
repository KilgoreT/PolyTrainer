package me.apomazkin.ui.unused

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun CheckedTextWidget(
    title: String,
    checkValue: () -> Boolean,
    onCheckValueChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onCheckValueChange.invoke(!checkValue.invoke()) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            modifier = Modifier,
            checked = checkValue.invoke(),
            onCheckedChange = onCheckValueChange
        )
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
        ) {
            CheckedTextWidget(
                title = "CheckTitled",
                checkValue = { true },
                onCheckValueChange = { }
            )
        }
    }
}