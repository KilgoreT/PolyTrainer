package me.apomazkin.component_widgets.widgets

import androidx.compose.runtime.Composable
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId

/**
 * Строка компонента в менеджере (Figma 5027:1548). Визуально идентична per-dict строке
 * (общий [ComponentRowCard]).
 *
 * IS485: бейджи охвата/«Несколько»/словарей не отображаются (терминология не финализирована);
 * параметры [isMultiple]/[isGlobal]/[dictionaryNames] остаются в сигнатуре — данные живут.
 */
@Composable
fun UserDefinedRowWidget(
    typeId: ComponentTypeId,
    name: String,
    template: ComponentTemplate,
    isMultiple: Boolean,
    isGlobal: Boolean,
    usageCount: Int,
    dictionaryNames: List<String>,
    onEdit: (ComponentTypeId) -> Unit,
    onDelete: (ComponentTypeId) -> Unit,
) {
    ComponentRowCard(
        name = name,
        template = template,
        valueCount = usageCount,
        onEdit = { onEdit(typeId) },
        onDelete = { onDelete(typeId) },
    )
}
