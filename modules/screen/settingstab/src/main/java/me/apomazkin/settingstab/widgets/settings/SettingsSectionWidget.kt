package me.apomazkin.settingstab.widgets.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.dividerColor

private const val DEFAULT_CONTENT_PADDING = 8
private const val DEFAULT_VERTICAL_ARRANGEMENT = 4
private const val DEFAULT_BORDER_RADIUS = 16
private const val DEFAULT_BORDER_WIDTH = 1

@Composable
fun SettingsSectionWidget(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(DEFAULT_BORDER_RADIUS.dp),
        border = BorderStroke(
            width = DEFAULT_BORDER_WIDTH.dp,
            color = dividerColor,
        ),
    ) {
        Column(
            modifier = Modifier.padding(DEFAULT_CONTENT_PADDING.dp),
            verticalArrangement = Arrangement
                .spacedBy(DEFAULT_VERTICAL_ARRANGEMENT.dp)
        ) {
            content.invoke(this)
        }
    }
}