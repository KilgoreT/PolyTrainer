package me.apomazkin.ui.dropdown

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * IS481 Tier 1 primitive — generic radio-вариант `MenuItem`.
 *
 * По аналогии с `MenuItemWithCheckbox` из widget/iconDropDowned, но через
 * `RadioButton` и без shared sealed-hierarchy (отдельная top-level composable,
 * не enum-ветвь в `MenuItem.kt`).
 *
 * @param title raw-строка (caller вызывает `stringResource()`).
 * @param isSelected текущий radio-state.
 * @param enabled `false` → визуально disabled, клики игнорируются.
 * @param onSelect клик-callback (caller — single-selection radio-group).
 */
@Composable
fun LexemeRadioMenuItem(
    isSelected: Boolean,
    title: String,
    enabled: Boolean = true,
    onSelect: () -> Unit,
) {
    DropdownMenuItem(
        leadingIcon = {
            RadioButton(
                selected = isSelected,
                onClick = null,
                enabled = enabled,
            )
        },
        text = { Text(text = title) },
        onClick = onSelect,
        enabled = enabled,
    )
}
