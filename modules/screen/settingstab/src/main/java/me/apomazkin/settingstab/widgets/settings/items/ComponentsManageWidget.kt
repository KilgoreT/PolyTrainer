package me.apomazkin.settingstab.widgets.settings.items

import androidx.compose.runtime.Composable
import me.apomazkin.core_resources.R
import me.apomazkin.settingstab.widgets.settings.items.base.SettingsItemWidget
import me.apomazkin.ui.preview.PreviewWidget

/**
 * IS481: Settings drill-in entry для `ComponentsManagerScreen`.
 *
 * Parity с [LangManageWidget] — thin wrapper поверх [SettingsItemWidget].
 */
@Composable
fun ComponentsManageWidget(
    onClick: () -> Unit,
) {
    SettingsItemWidget(
        iconRes = R.drawable.ic_components,
        titleRes = R.string.settings_section_components_management,
        showNextIcon = true,
        onClick = onClick,
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    ComponentsManageWidget(
        onClick = {}
    )
}
