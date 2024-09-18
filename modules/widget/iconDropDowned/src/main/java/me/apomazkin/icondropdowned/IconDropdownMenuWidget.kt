package me.apomazkin.icondropdowned

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

const val DEFAULT_CORNER_SHAPE = 12

@Composable
fun IconDropdownMenuWidget(
    isDropDownOpen: Boolean,
    onClickDropDown: () -> Unit,
    onDismissRequest: () -> Unit,
    cornerRounded: Int = DEFAULT_CORNER_SHAPE,
    backgroundColor: Color = MaterialTheme.colorScheme.onPrimary,
    icon: @Composable () -> Unit,
    items: @Composable ColumnScope.() -> Unit,
) {
    IconButton(
        onClick = onClickDropDown
    ) {
        icon.invoke()
        MaterialTheme(
            shapes = MaterialTheme.shapes
                .copy(extraSmall = RoundedCornerShape(cornerRounded.dp))
        ) {
            DropdownMenu(
                modifier = Modifier
                    .background(color = backgroundColor),
                expanded = isDropDownOpen,
                onDismissRequest = onDismissRequest,
            ) {
                items.invoke(this)
            }
        }
    }
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun PreviewOpen() {
    AppTheme {
        IconDropdownMenuWidget(
            isDropDownOpen = true,
            onClickDropDown = {},
            onDismissRequest = {},
            icon = {
                Icon(
                    imageVector = dataHelper.icon,
                    contentDescription = stringResource(id = dataHelper.contentDescription)
                )
            },
        ) {
            dataHelper.items.forEach {
                MenuItem(item = it) {}
            }
        }
    }
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun PreviewClose() {
    AppTheme {
        IconDropdownMenuWidget(
            isDropDownOpen = false,
            onClickDropDown = {},
            onDismissRequest = {},
            icon = {
                Icon(
                    imageVector = dataHelper.icon,
                    contentDescription = stringResource(id = dataHelper.contentDescription)
                )
            },
        ) {
            dataHelper.items.forEach {
                MenuItem(item = it) {}
            }
        }
    }
}