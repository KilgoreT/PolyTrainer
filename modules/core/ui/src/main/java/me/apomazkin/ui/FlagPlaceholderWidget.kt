package me.apomazkin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.grayTextColor

@Composable
fun FlagPlaceholderWidget(
    letter: String,
    modifier: Modifier = Modifier.size(24.dp),
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(grayTextColor.copy(alpha = 0.2f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = letter.uppercase(),
            style = LexemeStyle.BodyL,
            color = grayTextColor,
        )
    }
}
