package me.apomazkin.settingstab.widgets

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.apomazkin.settingstab.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.enableIconColor
import me.apomazkin.theme.unselectedGreyColor
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

private const val DEFAULT_ICON_SIZE = 24
private const val DEFAULT_PADDING_HORIZONTAL = 8
private const val DEFAULT_PADDING_VERTICAL = 12
private const val DEFAULT_SPACE_BETWEEN = 12

@Composable
fun SettingsItemWidget(
    @DrawableRes iconRes: Int,
    @StringRes titleRes: Int,
    showNextIcon: Boolean = false,
    onClick: (() -> Unit?)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = DEFAULT_PADDING_HORIZONTAL.dp,
                vertical = DEFAULT_PADDING_VERTICAL.dp
            )
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        horizontalArrangement = Arrangement.spacedBy(
            space = DEFAULT_SPACE_BETWEEN.dp,
            alignment = Alignment.Start,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBoxed(
            iconRes = iconRes,
            size = DEFAULT_ICON_SIZE,
            colorEnabled = enableIconColor,
            enabled = true,
            contentDescriptionRes = titleRes,
        )
        Text(
            modifier = Modifier
                .weight(1f),
            text = stringResource(id = titleRes),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            style = LexemeStyle.BodyL
                .copy(color = MaterialTheme.colorScheme.secondary),
        )
        if (showNextIcon) {
            IconBoxed(
                iconRes = R.drawable.ic_next,
                size = DEFAULT_ICON_SIZE,
                colorEnabled = unselectedGreyColor,
                enabled = true,
                contentDescriptionRes = titleRes
            )
        }
    }
}

@Composable
@PreviewWidget
private fun Preview(
    @PreviewParameter(BoolParam::class) showNextIcon: Boolean
) {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            SettingsItemWidget(
                iconRes = R.drawable.ic_tab_settings,
                titleRes = R.string.quiz_item_subtitle_write,
                showNextIcon = showNextIcon,
                onClick = {}
            )
        }
    }
}