package me.apomazkin.quiz.chat.widget.appbar.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.toRef
import me.apomazkin.quiz.chat.R
import me.apomazkin.ui.dropdown.LexemeRadioMenuItem

/**
 * IS481 quiz picker. Per-type wrapper над `LexemeRadioMenuItem`.
 *
 * Резолвит title:
 * - built-in TRANSLATION → `R.string.chat_menu_item_component_translation`.
 * - UserDefined → `name` raw (user-defined types не локализуются).
 *
 * `enabled` приходит сверху (`isPickerEnabled`).
 */
@Composable
internal fun ComponentChoiceItem(
    type: ComponentType,
    isSelected: Boolean,
    enabled: Boolean,
    onSelect: (ref: ComponentTypeRef) -> Unit,
) {
    val ref = type.toRef()
    val title = when (ref) {
        is ComponentTypeRef.BuiltIn -> when (ref.key) {
            BuiltInComponent.TRANSLATION -> stringResource(
                id = R.string.chat_menu_item_component_translation,
            )
        }
        is ComponentTypeRef.UserDefined -> ref.name
    }
    LexemeRadioMenuItem(
        isSelected = isSelected,
        title = title,
        enabled = enabled,
        onSelect = { onSelect(ref) },
    )
}
