package me.apomazkin.dictionaryappbar.widget

import androidx.compose.runtime.Composable
import me.apomazkin.core_resources.R
import me.apomazkin.theme.enableIconColor
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.preview.PreviewWidget

/**
 * IS481: Icon-button «hammer» в `DictionaryAppBar.actions`. Открывает
 * `PerDictionaryComponentsScreen` для текущего словаря.
 *
 * Wrapper поверх [IconBoxed] (Tier 1 primitive). Видимость контролируется
 * callsite'ом в `DictionaryAppBar.kt` (показывается при `currentDict != null && !isLoading`).
 */
@Composable
internal fun ComponentsToolsIconButton(
    onClick: () -> Unit,
) {
    IconBoxed(
        iconRes = R.drawable.ic_hammer,
        contentDescriptionRes = R.string.components_tools_description,
        enabled = true,
        colorEnabled = enableIconColor,
        size = 44,
        onClick = onClick,
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    ComponentsToolsIconButton(onClick = {})
}
