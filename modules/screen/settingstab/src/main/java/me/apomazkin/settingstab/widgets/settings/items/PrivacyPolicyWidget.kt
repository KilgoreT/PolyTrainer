package me.apomazkin.settingstab.widgets.settings.items

import androidx.compose.runtime.Composable
import me.apomazkin.settingstab.R
import me.apomazkin.settingstab.widgets.settings.items.base.SettingsItemWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun PrivacyPolicyWidget() {
    SettingsItemWidget(
        iconRes = R.drawable.ic_privacy_policy,
        titleRes = R.string.settings_section_privacy_policy,
        onClick = null,
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    PrivacyPolicyWidget()
}
