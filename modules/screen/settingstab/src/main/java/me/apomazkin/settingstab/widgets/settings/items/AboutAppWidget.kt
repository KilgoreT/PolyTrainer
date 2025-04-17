package me.apomazkin.settingstab.widgets.settings.items

import androidx.compose.runtime.Composable
import me.apomazkin.settingstab.R
import me.apomazkin.settingstab.widgets.settings.items.base.SettingsItemWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun AboutAppWidget(
    onClick: () -> Unit,
) {
    SettingsItemWidget(
        iconRes = R.drawable.ic_about,
        titleRes = R.string.settings_section_about_app,
        showNextIcon = true,
        onClick = onClick
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    AboutAppWidget(
        onClick = {}
    )
}