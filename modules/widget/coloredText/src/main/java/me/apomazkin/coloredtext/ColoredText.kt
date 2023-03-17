package me.apomazkin.coloredtext

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.FFD9E3

private const val DEFAULT_HORIZONTAL_PADDING = 8
private const val DEFAULT_CORNER_RADIUS = 8

@Composable
fun ColoredText(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.titleLarge,
    textColor: Color = FFD9E3,
    horizontalPadding: Int = DEFAULT_HORIZONTAL_PADDING,
    cornerRadius: Int = DEFAULT_CORNER_RADIUS,
) {
    Text(
        modifier = Modifier
            .then(modifier)
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(textColor)
            .padding(horizontal = horizontalPadding.dp),
        text = text,
        style = textStyle,
    )
}