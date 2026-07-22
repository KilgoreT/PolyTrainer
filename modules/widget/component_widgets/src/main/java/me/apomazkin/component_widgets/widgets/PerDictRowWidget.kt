package me.apomazkin.component_widgets.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.componentCardBorder
import me.apomazkin.theme.destructiveRed
import me.apomazkin.theme.formTextHint
import me.apomazkin.theme.templateChipBg
import me.apomazkin.theme.templateChipText
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Строка компонента словаря (Figma 5027:1548). Карточка r18 + бордер + лёгкая тень:
 * иконка типа, название, «Значений: N», чип шаблона, кнопки edit/delete.
 *
 * IS485: бейджи охвата/«Несколько»/словарей не отображаются (терминология не финализирована);
 * параметры [isMultiple]/[isGlobal]/[dictionaryNames] остаются в сигнатуре — данные живут.
 *
 * IS486 (В3): одна лента builtin + кастомных. Builtin ([isBuiltIn]) — без edit/delete,
 * только свитч [enabled]. Выключенный — приглушение + чип «Выключен»; деградировавший
 * ([degraded], цель мертва) — чип «Не работает».
 */
@Composable
fun PerDictRowWidget(
    typeId: ComponentTypeId,
    name: String,
    template: ComponentTemplate,
    isMultiple: Boolean,
    isGlobal: Boolean,
    valueCount: Int,
    onEdit: (ComponentTypeId) -> Unit,
    onDelete: (ComponentTypeId) -> Unit,
    dictionaryNames: List<String> = emptyList(),
    isBuiltIn: Boolean = false,
    enabled: Boolean = true,
    degraded: Boolean = false,
    enabledTogglePending: Boolean = false,
    onToggleEnabled: (ComponentTypeId, Boolean) -> Unit = { _, _ -> },
) {
    ComponentRowCard(
        name = name,
        template = template,
        valueCount = valueCount,
        showActions = !isBuiltIn,
        enabled = enabled,
        degraded = degraded,
        enabledTogglePending = enabledTogglePending,
        onEdit = { onEdit(typeId) },
        onDelete = { onDelete(typeId) },
        onToggleEnabled = { onToggleEnabled(typeId, it) },
    )
}

/** Чип состояния строки (IS486 В3): «Выключен» / «Не работает». */
@Composable
private fun RowStateChip(textRes: Int, destructive: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = templateChipBg,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            text = stringResource(id = textRes),
            style = LexemeStyle.BodyS,
            color = if (destructive) destructiveRed else templateChipText,
        )
    }
}

/** Общий облик строки компонента для обоих экранов (per-dict + Manager). */
@Composable
internal fun ComponentRowCard(
    name: String,
    template: ComponentTemplate,
    valueCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showActions: Boolean = true,
    enabled: Boolean = true,
    degraded: Boolean = false,
    enabledTogglePending: Boolean = false,
    onToggleEnabled: ((Boolean) -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = whiteColor,
        border = BorderStroke(1.dp, componentCardBorder),
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
                // IS486 (В3): выключенный компонент приглушён (контент, не свитч).
                .alpha(if (enabled) 1f else 0.5f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ComponentTypeIcon()
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = LexemeStyle.BodyLBold,
                        color = LexemeColor.secondary,
                    )
                    Text(
                        text = stringResource(id = R.string.per_dict_row_value_count, valueCount),
                        style = LexemeStyle.BodyS,
                        color = formTextHint,
                    )
                }
                if (showActions) {
                    ComponentIconButton(
                        iconRes = R.drawable.ic_edit,
                        tint = LexemeColor.secondary,
                        onClick = onEdit,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    ComponentIconButton(
                        iconRes = R.drawable.ic_trash,
                        tint = destructiveRed,
                        onClick = onDelete,
                    )
                }
                if (onToggleEnabled != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = enabled,
                        enabled = !enabledTogglePending,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = LexemeColor.primary,
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.padding(top = 8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TemplateChip(template = template)
                if (!enabled) {
                    Spacer(modifier = Modifier.width(8.dp))
                    RowStateChip(textRes = R.string.components_row_disabled, destructive = false)
                }
                if (degraded) {
                    Spacer(modifier = Modifier.width(8.dp))
                    RowStateChip(textRes = R.string.components_row_degraded, destructive = true)
                }
            }
        }
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    ComponentRowCard(
        name = "Определение",
        template = ComponentTemplate.TEXT,
        valueCount = 3,
        onEdit = {},
        onDelete = {},
    )
}

@Composable
@PreviewWidget
private fun PreviewBuiltInDisabled() {
    ComponentRowCard(
        name = "Часть речи",
        template = ComponentTemplate.CHOICE,
        valueCount = 5,
        showActions = false,
        enabled = false,
        onEdit = {},
        onDelete = {},
        onToggleEnabled = {},
    )
}
