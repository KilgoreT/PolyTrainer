package me.apomazkin.settingstab.widgets.settings.items

import androidx.compose.runtime.Composable
import me.apomazkin.settingstab.R
import me.apomazkin.settingstab.widgets.settings.items.base.SettingsItemWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun RateWidget(
    onClick: (() -> Unit)? = null,
) {
    SettingsItemWidget(
        iconRes = R.drawable.ic_rate,
        titleRes = R.string.settings_section_rate,
        showNextIcon = false,
        onClick = onClick
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    RateWidget(
        onClick = {}
    )
}