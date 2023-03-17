package me.apomazkin.icondropdowned

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.res.stringResource
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

@Composable
fun IconDropDowned(
    data: DropData,
    isDropDownOpen: Boolean,
    onClickDropDown: () -> Unit,
    onDismissRequest: () -> Unit,
    onItemClick: (DropDataItem) -> Unit,
) {
    IconButton(
        onClick = onClickDropDown
    ) {
        Icon(
            imageVector = data.icon,
            contentDescription = stringResource(id = data.contentDescription)
        )
        DropdownMenu(
            expanded = isDropDownOpen,
            onDismissRequest = onDismissRequest
        ) {
            data.items.forEach {
                key(it.titleRes) {
                    Item(item = it) {
                        onItemClick.invoke(it)
                    }
                }
            }
        }
    }
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun PreviewOpen() {
    AppTheme {
        IconDropDowned(
            data = dataHelper,
            isDropDownOpen = true,
            onClickDropDown = {},
            onDismissRequest = {},
            onItemClick = {},
        )
    }
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun PreviewClose() {
    AppTheme {
        IconDropDowned(
            data = dataHelper,
            isDropDownOpen = false,
            onClickDropDown = {},
            onDismissRequest = {},
            onItemClick = {}
        )
    }
}