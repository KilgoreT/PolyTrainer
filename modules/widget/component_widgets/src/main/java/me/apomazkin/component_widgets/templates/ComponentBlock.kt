package me.apomazkin.component_widgets.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor

/**
 * IS481 phase 2 — structural wrapper: name-label + content slot.
 *
 * Pure layout — name from `type.name` (fallback empty), content от caller.
 * Используется в [ComponentByTemplate] для рендера всех ComponentTemplate variants.
 *
 * Per `concept/typed_views.md` Tier-2: per-template widget оборачивается ComponentBlock
 * для consistency name-label стиля.
 */
@Composable
fun ComponentBlock(
    type: ComponentType,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = type.name.orEmpty(),
            style = LexemeStyle.BodyS,
            color = blackColor,
        )
        content()
    }
}
