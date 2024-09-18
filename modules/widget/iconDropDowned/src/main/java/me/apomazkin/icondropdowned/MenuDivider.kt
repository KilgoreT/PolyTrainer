package me.apomazkin.icondropdowned

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.dividerColor

private const val DEFAULT_PADDING_HORIZONTAL = 12
private const val DEFAULT_PADDING_VERTICAL = 12
private const val DEFAULT_HEIGHT = 1

@Composable
fun MenuDivider(
    height: Int = DEFAULT_HEIGHT,
    paddingHorizontal: Int = DEFAULT_PADDING_HORIZONTAL,
    paddingVertical: Int = DEFAULT_PADDING_VERTICAL,
    color: Color = dividerColor,
) {
    DropdownMenuItem(
        modifier = Modifier
            .padding(horizontal = paddingHorizontal.dp, vertical = paddingVertical.dp)
            .height(height.dp)
            .background(color = color),
        text = {},
        onClick = {}
    )
}