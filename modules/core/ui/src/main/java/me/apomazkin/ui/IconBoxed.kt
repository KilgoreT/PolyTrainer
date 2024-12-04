package me.apomazkin.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

private const val DEFAULT_SIZE = 56

@Composable
fun IconBoxed(
    @DrawableRes iconRes: Int,
    enabled: Boolean = false,
    @StringRes contentDescriptionRes: Int = R.string.content_description_icon,
    modifier: Modifier = Modifier,
    size: Int = DEFAULT_SIZE,
    colorEnabled: Color = MaterialTheme.colorScheme.onPrimary,
    colorDisabled: Color = MaterialTheme.colorScheme.onSecondary,
    clipShape: Shape = CircleShape,
    onClick: (() -> Unit)? = null,
) {
    val clicker = if (onClick != null) {
        Modifier
            .clip(clipShape)
            .clickable(
                enabled = enabled
            ) { onClick.invoke() }
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .size(size.dp)
            .then(clicker),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            tint = if (enabled) colorEnabled else colorDisabled,
            contentDescription = stringResource(id = contentDescriptionRes)
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewEnabled() {
    AppTheme {
        IconBoxed(
            iconRes = R.drawable.ic_send,
            enabled = true,
            colorEnabled = MaterialTheme.colorScheme.primary,
        ) {}
    }
}

@PreviewWidget
@Composable
private fun PreviewDisabled() {
    AppTheme {
        IconBoxed(
            iconRes = R.drawable.ic_send,
            enabled = false,
            colorDisabled = Color.Gray
        ) {}
    }
}