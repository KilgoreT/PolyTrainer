package me.apomazkin.icondropdowned

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.enableIconColor
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

const val DEFAULT_CORNER_SHAPE = 12
const val DEFAULT_ICON_SIZE = 44

@Composable
fun IconDropdownWidget(
        isDropDownOpen: Boolean,
        onClickDropDown: () -> Unit,
        onDismissRequest: () -> Unit,
        cornerRounded: Int = DEFAULT_CORNER_SHAPE,
        backgroundColor: Color = MaterialTheme.colorScheme.onPrimary,
        icon: @Composable (() -> Unit)? = null,
        items: @Composable ColumnScope.() -> Unit,
) {
    IconButton(
            onClick = onClickDropDown
    ) {
        icon?.invoke() ?: run {
            IconBoxed(
                    iconRes = R.drawable.ic_more,
                    enabled = true,
                    colorEnabled = enableIconColor,
                    size = DEFAULT_ICON_SIZE,
            )
        }
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

@PreviewWidget
@Composable
private fun PreviewOpen(
        @PreviewParameter(BoolParam::class) isOpen: Boolean,
) {
    AppTheme {
        IconDropdownWidget(
                isDropDownOpen = isOpen,
                onClickDropDown = {},
                onDismissRequest = {},
        ) {
            MenuItem
                    .withIcon(
                            icon = DeleteIcon,
                            title = StringSource.fromRes(resId = R.string.menu_item_delete),
                            onClick = {}
                    ).Widget()
        }
    }
}