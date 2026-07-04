package me.apomazkin.quiz.chat.widget.appbar.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.toRef
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.logic.ItemsState
import me.apomazkin.quiz.chat.logic.isPickerEnabled
import me.apomazkin.ui.dropdown.LexemeSubmenuMenuItem

/**
 * IS481 quiz picker. Top-level submenu: header «Quiz component» +
 * inline radio-список `ComponentChoiceItem` per `availableTypes`.
 *
 * - `availableTypes.isEmpty()` → composable рендерит ничего (invariant 3 — UI guard).
 * - Header всегда раскрывается (degenerate-case `size == 1` тоже): юзер видит,
 *   что выбрано. Disabled-state остаётся на `ComponentChoiceItem.enabled`.
 */
@Composable
internal fun QuizComponentMenuItem(
    state: ItemsState.QuizComponent,
    onSelect: (ref: ComponentTypeRef) -> Unit,
) {
    if (state.availableTypes.isEmpty()) return
    LexemeSubmenuMenuItem(
        title = stringResource(id = R.string.chat_menu_item_quiz_component),
    ) {
        state.availableTypes.forEach { type ->
            ComponentChoiceItem(
                type = type,
                isSelected = type.toRef() == state.selectedRef,
                enabled = state.isPickerEnabled,
                onSelect = onSelect,
            )
        }
    }
}
