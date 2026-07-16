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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Строка компонента словаря (Figma 5027:1548). Карточка r18 + бордер + лёгкая тень:
 * иконка типа, название, «Значений: N», чип шаблона, кнопки edit/delete.
 *
 * IS485: бейджи охвата/«Несколько»/словарей не отображаются (терминология не финализирована);
 * параметры [isMultiple]/[isGlobal]/[dictionaryNames] остаются в сигнатуре — данные живут.
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
) {
    ComponentRowCard(
        name = name,
        template = template,
        valueCount = valueCount,
        onEdit = { onEdit(typeId) },
        onDelete = { onDelete(typeId) },
    )
}

/** Общий облик строки компонента для обоих экранов (per-dict + Manager). */
@Composable
internal fun ComponentRowCard(
    name: String,
    template: ComponentTemplate,
    valueCount: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
                .padding(18.dp),
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
            Spacer(modifier = Modifier.padding(top = 8.dp))
            TemplateChip(template = template)
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
