package me.apomazkin.settingstab.widgets.settings.items

import androidx.compose.runtime.Composable
import me.apomazkin.settingstab.R
import me.apomazkin.settingstab.widgets.settings.items.base.SettingsItemWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun ExportDataWidget(
    onClick: () -> Unit,
) {
    SettingsItemWidget(
        iconRes = R.drawable.ic_move,
        titleRes = R.string.settings_section_export,
        showNextIcon = false,
        onClick = onClick
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    ExportDataWidget(
        onClick = {}
    )
}