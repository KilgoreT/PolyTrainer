package me.apomazkin.component_widgets.templates

import androidx.compose.runtime.Composable
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.TemplateValues
import me.apomazkin.lexeme.TextValues

/**
 * IS481 phase 2 — exhaustive-when resolver по [ComponentTemplate].
 *
 * Per `concept/typed_views.md` Tier-2: фундамент per-template architecture. Каждый
 * template имеет свой Tier-2 widget; resolver диспатчит по `type.template`.
 *
 * MVP: только [ComponentTemplate.TEXT] имеет рендер ([TextWidget]); IMAGE — backlog
 * (placeholder либо не рисуется).
 *
 * @param type domain `ComponentType` (используется для name-label в [ComponentBlock]).
 * @param values type-tagged значение (sealed: TextValues | ImageValues).
 * @param editable если true — editable-mode (передаётся в Tier-2 widget).
 * @param onValueChange callback для editable-mode (вызывается с обновлённым `TemplateValues`).
 */
@Composable
fun ComponentByTemplate(
    type: ComponentType,
    values: TemplateValues,
    editable: Boolean = false,
    onValueChange: (TemplateValues) -> Unit = {},
) {
    when (type.template) {
        ComponentTemplate.TEXT -> {
            val text = (values as? TextValues)?.value?.value.orEmpty()
            ComponentBlock(type = type) {
                TextWidget(
                    value = text,
                    editable = editable,
                    onValueChange = { newText ->
                        onValueChange(
                            TextValues(value = me.apomazkin.lexeme.Primitive.Text(newText)),
                        )
                    },
                )
            }
        }

        ComponentTemplate.IMAGE -> {
            // Backlog: IMAGE template UI (placeholder либо иконка).
            ComponentBlock(type = type) {
                // Empty content slot — IMAGE рендер в будущей фиче.
            }
        }
    }
}
