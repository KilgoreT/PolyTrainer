@file:OptIn(ExperimentalLayoutApi::class)

package me.apomazkin.component_widgets.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.theme.enableIconColor
import me.apomazkin.ui.IconBoxed

/**
 * IS481 phase 2 — per-dict CRUD row.
 *
 * Layout (см. `docs/features/IS481_component_constructor_phase2/bugs/layout_component_item.md`):
 *
 * Column:
 *   FlowRow [Text(name) + chip(global if isGlobal) + chip(одно/много)]
 *   Row     [icon(24) + chip(template) + Spacer(weight=1) + edit(44) + trash(44)]
 *   Text    «Значений: N»
 *
 * Чипы — кастомный [BlueAssistChip] (pill, мелкий шрифт).
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = name,
                    style = LexemeStyle.BodyL,
                    color = blackColor,
                )
                if (isGlobal) {
                    BlueAssistChip(textRes = R.string.components_chip_global)
                }
                BlueAssistChip(
                    textRes = if (isMultiple) {
                        R.string.components_chip_multi
                    } else {
                        R.string.components_chip_single
                    },
                )
            }
            if (!isGlobal && dictionaryNames.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    dictionaryNames.forEach { dictName ->
                        BlueAssistChipText(text = dictName)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconBoxed(
                    iconRes = R.drawable.ic_components,
                    contentDescriptionRes = R.string.content_description_icon,
                    enabled = true,
                    colorEnabled = enableIconColor,
                    size = 24,
                )
                BlueAssistChip(textRes = template.labelRes())
                Row(modifier = Modifier.weight(1f)) {}
                IconBoxed(
                    iconRes = R.drawable.ic_edit,
                    contentDescriptionRes = R.string.content_description_button,
                    enabled = true,
                    colorEnabled = enableIconColor,
                    size = 44,
                    onClick = { onEdit(typeId) },
                )
                IconBoxed(
                    iconRes = R.drawable.ic_trash,
                    contentDescriptionRes = R.string.button_delete,
                    enabled = true,
                    colorEnabled = enableIconColor,
                    size = 44,
                    onClick = { onDelete(typeId) },
                )
            }
            Text(
                text = stringResource(
                    id = R.string.per_dict_row_value_count,
                    valueCount,
                ),
                style = LexemeStyle.BodyS,
                color = blackColor,
            )
        }
    }
}
