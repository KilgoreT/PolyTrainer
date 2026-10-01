package me.apomazkin.quiz.chat.widget.appbar.menu

import androidx.compose.runtime.Composable
import me.apomazkin.icondropdowned.MenuItem
import me.apomazkin.icondropdowned.StringSource
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.toRef
import me.apomazkin.quiz.chat.R
import me.apomazkin.theme.LexemeStyle

/**
 * Галка одного ядра в подменю «Компонент квиза».
 *
 * Подпись: встроенный перевод — ресурс (тот же, что имя ядра в вопросе,
 * имена обязаны совпадать); пользовательское ядро — его имя как есть.
 * Стиль — как у соседних галок меню.
 */
@Composable
internal fun ComponentChoiceItem(
    type: ComponentType,
    isChecked: Boolean,
    enabled: Boolean,
    onToggle: (checked: Boolean) -> Unit,
) {
    val title = when (val ref = type.toRef()) {
        is ComponentTypeRef.BuiltIn -> when (ref.key) {
            BuiltInComponent.TRANSLATION -> StringSource.fromRes(
                resId = R.string.chat_menu_item_component_translation,
                style = LexemeStyle.BodyL,
            )
            // CHOICE и CAPTIONED_TEXT в кандидаты не попадают (белый список
            // шаблонов) — ветки для exhaustive-when, подпись из общего ресурса.
            BuiltInComponent.PART_OF_SPEECH -> StringSource.fromRes(
                resId = me.apomazkin.core_resources.R.string.builtin_component_part_of_speech,
                style = LexemeStyle.BodyL,
            )
            BuiltInComponent.EXAMPLE -> StringSource.fromRes(
                resId = me.apomazkin.core_resources.R.string.builtin_component_example,
                style = LexemeStyle.BodyL,
            )
        }
        is ComponentTypeRef.UserDefined -> StringSource.fromRaw(
            value = ref.name,
            style = LexemeStyle.BodyL,
        )
    }
    MenuItem
        .withCheckbox(
            isChecked = isChecked,
            title = title,
            enabled = enabled,
            onCheckedChange = onToggle,
        )
        .Widget()
}
