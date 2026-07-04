package me.apomazkin.component_widgets.widgets

import androidx.annotation.StringRes
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTemplate

/**
 * IS481 phase 2: принимает extension functions из удалённых одноимённых файлов обоих
 * screen-модулей (миграция components_manager + per_dictionary_components).
 *
 * UI-extension: маппинг [ComponentTemplate] → string resource для отображения в чипах
 * и radio-group. Domain enum (см. `modules/domain/lexeme/.../ComponentTemplate.kt`)
 * сознательно не содержит `displayName` — UI-presentation strictly local.
 */
@StringRes
internal fun ComponentTemplate.labelRes(): Int = when (this) {
    ComponentTemplate.TEXT -> R.string.components_template_text
    ComponentTemplate.IMAGE -> R.string.components_template_image
}
