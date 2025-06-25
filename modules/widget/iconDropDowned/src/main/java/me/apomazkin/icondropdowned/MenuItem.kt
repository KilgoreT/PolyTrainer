package me.apomazkin.icondropdowned

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget


sealed interface MenuItem {

    val title: StringSource

    @Composable
    fun Widget()

    companion object {

        @Composable
        fun withIcon(
                icon: IconSource,
                title: StringSource,
                onClick: () -> Unit,
        ) = MenuItemWithIcon(
                icon = icon,
                title = title,
                onClick = onClick
        )

        @Composable
        fun text(
                title: StringSource,
                onClick: () -> Unit,
        ) = MenuItemTextOnly(
                title = title,
                onClick = onClick
        )

        @Composable
        fun withCheckbox(
                isChecked: Boolean,
                title: StringSource,
                onCheckedChange: (Boolean) -> Unit,
        ) = MenuItemWithCheckbox(
                isChecked = isChecked,
                title = title,
                onCheckedChange = onCheckedChange,
        )
    }
}

data class MenuItemWithIcon(
        val icon: IconSource,
        override val title: StringSource,
        val onClick: () -> Unit,
) : MenuItem {

    @Composable
    override fun Widget(
    ) = MenuItem(
            icon = icon,
            title = title,
            onClick = onClick
    )
}

data class MenuItemTextOnly(
        override val title: StringSource,
        val onClick: () -> Unit,
) : MenuItem {

    @Composable
    override fun Widget(
    ) = MenuItem(
            title = title,
            onClick = onClick
    )
}

data class MenuItemWithCheckbox(
        val isChecked: Boolean,
        override val title: StringSource,
        val onCheckedChange: (Boolean) -> Unit,
) : MenuItem {

    @Composable
    override fun Widget() = MenuItem(
            isChecked = isChecked,
            title = title,
            onCheckedChange = onCheckedChange,
    )
}


@Composable
internal fun MenuItem(
        icon: IconSource? = null,
        title: StringSource,
        onClick: () -> Unit,
) {
    DropdownMenuItem(
            leadingIcon = if (icon != null) {
                { icon.updateContentDescription(title).Widget() }
            } else null,
            text = {
                Text(
                        text = title.asString(),
                        style = title.textStyle(),
                        color = title.color ?: Color.Unspecified,
                )
            },
            onClick = onClick,
    )
}

//TODO kilg 21.06.2025 20:32 Checkbox приносит свои паддинги,
// поэтому слегка выбиатся из общего ряда.
// Либо сделать свою реализацию по стилям,
// либо в дропдаун добавлять только однотипные.
@Composable
internal fun MenuItem(
        isChecked: Boolean,
        title: StringSource,
        onCheckedChange: (Boolean) -> Unit,
) {
    DropdownMenuItem(
            leadingIcon = {
                Checkbox(
                        checked = isChecked,
                        onCheckedChange = onCheckedChange,
                        enabled = true,
                        colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.onSurface
                        )
                )
            },
            text = {
                Text(
                        text = title.asString(),
                        style = title.textStyle(),
                        color = title.color ?: Color.Unspecified,
                )
            },
            onClick = { onCheckedChange.invoke(!isChecked) },
    )
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Column {
            MenuItem(
                    icon = DeleteIcon,
                    title = StringSource.fromRaw("Опасно"),
            ) {}
            DividerMenuItem()
            MenuItem.withIcon(
                    icon = DeleteIcon,
                    title = StringSource.fromRes(R.string.menu_item_delete),
                    onClick = {}
            ).Widget()
            DividerMenuItem()
            MenuItem
                    .text(
                            title = StringSource.fromRaw("Убивает"),
                            onClick = {},
                    )
                    .Widget()
            DividerMenuItem()
            MenuItem.withCheckbox(
                    isChecked = true,
                    title = StringSource.fromRaw("Проверить"),
                    onCheckedChange = {}
            ).Widget()
        }
    }
}