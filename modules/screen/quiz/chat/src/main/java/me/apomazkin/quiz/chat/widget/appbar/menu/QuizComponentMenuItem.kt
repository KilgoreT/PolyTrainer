package me.apomazkin.quiz.chat.widget.appbar.menu

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.toRef
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.logic.ItemsState
import me.apomazkin.quiz.chat.logic.isPickerVisible
import me.apomazkin.ui.dropdown.LexemeSubmenuMenuItem

/**
 * Подменю «Компонент квиза»: заголовок с подписью «минимум одно» и галка
 * на каждое ядро словаря. Показывается только при двух и более ядрах —
 * с одним выбирать нечего. Единственная включённая галка задизейблена:
 * чтобы оставить одно другое ядро, сначала включают его.
 */
@Composable
internal fun QuizComponentMenuItem(
    state: ItemsState.QuizComponent,
    onToggle: (ref: ComponentTypeRef, checked: Boolean) -> Unit,
) {
    if (!state.isPickerVisible) return
    LexemeSubmenuMenuItem(
        title = stringResource(id = R.string.chat_menu_item_quiz_component),
        subtitle = stringResource(id = R.string.chat_menu_quiz_component_hint),
    ) {
        state.availableTypes.forEach { type ->
            val ref = type.toRef()
            val isChecked = ref in state.selectedRefs
            ComponentChoiceItem(
                type = type,
                isChecked = isChecked,
                enabled = !(isChecked && state.selectedRefs.size == 1),
                onToggle = { checked -> onToggle(ref, checked) },
            )
        }
    }
}
