package me.apomazkin.component_widgets.widgets

import androidx.annotation.StringRes
import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.NameError

/**
 * IS481 phase 2: принимает extension functions из удалённых одноимённых файлов обоих
 * screen-модулей.
 *
 * UI-extension: маппинг domain [NameError] → string resource. Domain содержит только
 * enum-ветки, тексты живут в UI слое.
 */
@StringRes
internal fun NameError.labelRes(): Int = when (this) {
    NameError.Empty -> R.string.components_name_error_empty
    NameError.SameScopeCollision -> R.string.components_name_error_same_scope_collision
    NameError.CrossScopeCollision -> R.string.components_name_error_cross_scope_collision
}
