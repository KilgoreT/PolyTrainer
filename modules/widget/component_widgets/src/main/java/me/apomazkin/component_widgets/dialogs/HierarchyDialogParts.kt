package me.apomazkin.component_widgets.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.DependencyTarget
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.destructiveRed
import me.apomazkin.theme.dialogFieldBg
import me.apomazkin.theme.radioBorderInactive
import me.apomazkin.theme.radioSelectedBg
import me.apomazkin.theme.templateChipText
import me.apomazkin.theme.whiteColor

/**
 * IS486 (В1): элемент пикера цели зависимости. Host собирает список кандидатов
 * (компоненты словаря без самого редактируемого + вложенные опции CHOICE);
 * «Самостоятельный» (цель-лексема) рендерится самим пикером первым рядом.
 */
data class TargetPickerItem(
    val target: DependencyTarget,
    val label: String,
    /** true — вложенная опция CHOICE (отступ под родителем). */
    val indent: Boolean = false,
    /** Решение 2026-07-21 (C2): цель, создающая цикл, — задизейблена в пикере. */
    val enabled: Boolean = true,
    /** Подпись «нельзя — цикл» под рядом (у компонент-ряда; опции просто приглушены). */
    val showCycleHint: Boolean = false,
)

/** Радио-ряд с динамическим текстом (имена компонентов/опций) — стиль [ComponentRadioRow]. */
@Composable
private fun TargetRadioRow(
    text: String,
    selected: Boolean,
    indent: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 24.dp else 0.dp)
            .height(42.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (selected) radioSelectedBg else whiteColor,
        border = BorderStroke(1.dp, if (selected) LexemeColor.primary else radioBorderInactive),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(
                        width = 2.dp,
                        color = if (selected) LexemeColor.primary else radioBorderInactive,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color = LexemeColor.primary, shape = RoundedCornerShape(5.dp)),
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = if (selected) LexemeStyle.BodyMBold else LexemeStyle.BodyM,
                color = if (selected) LexemeColor.secondary else templateChipText,
            )
        }
    }
}

/**
 * IS486 (В1): пикер цели зависимости — радио-ряды «Самостоятельный» + компоненты
 * словаря + вложенные опции CHOICE; галка «Ядро» показана только при цели-лексеме.
 */
@Composable
internal fun ComponentTargetPicker(
    items: List<TargetPickerItem>,
    selected: DependencyTarget,
    core: Boolean,
    onTargetSelect: (DependencyTarget) -> Unit,
    onCoreToggle: (Boolean) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ComponentDialogLabel(textRes = R.string.components_field_dependency, accent = false)
        TargetRadioRow(
            text = stringResource(id = R.string.components_target_lexeme),
            selected = selected is DependencyTarget.Lexeme,
            indent = false,
            enabled = true,
            onClick = { onTargetSelect(DependencyTarget.Lexeme) },
        )
        items.forEach { item ->
            TargetRadioRow(
                text = item.label,
                selected = item.target == selected,
                indent = item.indent,
                enabled = item.enabled,
                onClick = { onTargetSelect(item.target) },
            )
            if (item.showCycleHint) {
                Text(
                    modifier = Modifier.padding(start = if (item.indent) 24.dp else 0.dp),
                    text = stringResource(id = R.string.components_target_cycle_hint),
                    style = LexemeStyle.BodyS,
                    color = destructiveRed,
                )
            }
        }
        if (selected is DependencyTarget.Lexeme) {
            ComponentMultiToggle(
                textRes = R.string.components_field_core,
                checked = core,
                onToggle = onCoreToggle,
            )
        }
    }
}

/** Текст-поле варианта CHOICE с крестиком удаления (В2). */
@Composable
private fun OptionFieldRow(
    value: String,
    onValueChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(46.dp),
            shape = RoundedCornerShape(13.dp),
            color = dialogFieldBg,
            border = BorderStroke(1.dp, radioBorderInactive),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 15.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = LexemeStyle.BodyL.copy(color = LexemeColor.secondary),
                    cursorBrush = SolidColor(LexemeColor.primary),
                )
            }
        }
        Icon(
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onRemove),
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = null,
            tint = destructiveRed,
        )
    }
}

/**
 * IS486 (В2): редактор вариантов CHOICE — существующие опции ([existing]: удаление
 * через конфирм у host'а), черновики новых ([drafts]: убираются крестиком сразу),
 * кнопка «Добавить вариант», строка ошибки «нужен хотя бы один» ([showError]).
 */
@Composable
internal fun OptionListEditor(
    existing: List<Pair<Long, String>>,
    drafts: List<String>,
    showError: Boolean,
    onExistingChange: (Long, String) -> Unit,
    onExistingDelete: (Long) -> Unit,
    onDraftChange: (Int, String) -> Unit,
    onDraftRemove: (Int) -> Unit,
    onDraftAdd: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ComponentDialogLabel(textRes = R.string.components_field_options, accent = false)
        existing.forEach { (optionId, label) ->
            OptionFieldRow(
                value = label,
                onValueChange = { onExistingChange(optionId, it) },
                onRemove = { onExistingDelete(optionId) },
            )
        }
        drafts.forEachIndexed { index, draft ->
            OptionFieldRow(
                value = draft,
                onValueChange = { onDraftChange(index, it) },
                onRemove = { onDraftRemove(index) },
            )
        }
        if (showError) {
            Text(
                text = stringResource(id = R.string.components_options_error),
                style = LexemeStyle.BodyS,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            modifier = Modifier
                .clickable(onClick = onDraftAdd)
                .padding(vertical = 4.dp),
            text = stringResource(id = R.string.components_option_add),
            style = LexemeStyle.BodyMBold,
            color = LexemeColor.primary,
        )
    }
}
