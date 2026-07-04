package me.apomazkin.component_widgets.widgets

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * IS481 phase 2 bugfix (см. `bugs/layout_component_item.md`).
 *
 * Read-only chip с синим фоном (primary) и белым текстом (onPrimary).
 *
 * Два варианта:
 * - [BlueAssistChip] — для статичных лейблов через `@StringRes`.
 * - [BlueAssistChipText] — для динамических строк (имена словарей).
 *
 * НЕ выносить в `:modules:core:ui` — других callsite'ов нет.
 */
@Composable
fun BlueAssistChip(@StringRes textRes: Int) {
    BlueChipContainer {
        Text(
            text = stringResource(id = textRes),
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        )
    }
}

@Composable
fun BlueAssistChipText(text: String) {
    BlueChipContainer {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        )
    }
}

@Composable
private fun BlueChipContainer(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.primary,
    ) {
        content()
    }
}
