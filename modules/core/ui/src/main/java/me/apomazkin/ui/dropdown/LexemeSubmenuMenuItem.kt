package me.apomazkin.ui.dropdown

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.apomazkin.theme.LexemeStyle

/**
 * Подменю внутри `DropdownMenu`: заголовок с шевроном, по клику
 * раскрывает `content` inline-колонкой под собой (не отдельным меню).
 *
 * Чисто презентационный, состояние раскрытия — локальное. При
 * `enabled = false` заголовок показан, клики игнорируются.
 *
 * @param title заголовок (caller уже вызвал `stringResource()`).
 * @param subtitle мелкая подпись под заголовком (например, правило
 *   набора «минимум одно»); `null` — без подписи.
 * @param content пункты подменю (галки, radio и т.п.).
 */
@Composable
fun LexemeSubmenuMenuItem(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    DropdownMenuItem(
        text = {
            Column {
                Text(text = title)
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = LexemeStyle.BodyS,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
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
