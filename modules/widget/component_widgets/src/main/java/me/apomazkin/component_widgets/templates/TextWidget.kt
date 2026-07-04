package me.apomazkin.component_widgets.templates

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.input.base.LexemeTextFieldWidget

/**
 * IS481 phase 2 — per-template Tier-2 composable для [ComponentTemplate.TEXT].
 *
 * Modes:
 * - `editable = false` (default) — read-only `Text` рендер.
 * - `editable = true` — input `LexemeTextFieldWidget` с upstream `onValueChange`.
 *
 * Per `concept/typed_views.md`: каждый ComponentTemplate имеет свой Tier-2 widget;
 * IMAGE template — backlog (MVP only TEXT).
 */
@Composable
fun TextWidget(
    value: String,
    editable: Boolean = false,
    onValueChange: (String) -> Unit = {},
) {
    if (editable) {
        LexemeTextFieldWidget(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            placeHolder = null,
            onValueChange = onValueChange,
            onKeyboardActions = {},
        )
    } else {
        Text(
            text = value,
            style = LexemeStyle.BodyL,
        )
    }
}
