package me.apomazkin.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

private const val DefaultSize = 48

@Composable
fun IconBoxed(
    @DrawableRes iconRes: Int,
    enabled: Boolean = false,
    @StringRes contentDescriptionRes: Int = R.string.content_description_icon,
    size: Int = DefaultSize,
    color: Color = MaterialTheme.colorScheme.onPrimary,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(size.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier
                .clickable(enabled) { onClick.invoke() },
            painter = painterResource(id = iconRes),
            tint = color,
            contentDescription = stringResource(id = contentDescriptionRes)
        )
    }
}