package me.apomazkin.ui.dropdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * IS481 Tier 1 primitive — generic submenu wrapper над `DropdownMenuItem` для
 * radio-группы. Заголовок + trailing chevron + раскрывающаяся inner-Column
 * с radio-пунктами.
 *
 * Pure presentational, без logic. Internal state `isExpanded`. При
 * `enabled=false` — header показан, клики игнорируются (degenerate case).
 *
 * @param title raw-строка заголовка (caller вызывает `stringResource()`).
 * @param content radio-пункты, рендерятся в inline `Column` под header'ом
 *   когда `isExpanded`. Не отдельный `DropdownMenu` — раскрытие inline.
 */
@Composable
fun LexemeSubmenuMenuItem(
    title: String,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    DropdownMenuItem(
        text = { Text(text = title) },
        trailingIcon = {
            Icon(
                imageVector = if (isExpanded) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = null,
            )
        },
        onClick = { if (enabled) isExpanded = !isExpanded },
        enabled = enabled,
    )
    if (isExpanded) {
        Column(content = content)
    }
}
