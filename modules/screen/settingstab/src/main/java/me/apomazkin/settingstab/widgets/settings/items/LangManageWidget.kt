package me.apomazkin.settingstab.widgets.settings.items

import androidx.compose.runtime.Composable
import me.apomazkin.settingstab.R
import me.apomazkin.settingstab.widgets.settings.items.base.SettingsItemWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun LangManageWidget(
    onClick: () -> Unit,
) {
    SettingsItemWidget(
        iconRes = R.drawable.ic_lang,
        titleRes = R.string.settings_section_lang_management,
        showNextIcon = true,
        onClick = onClick
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    LangManageWidget(
        onClick = {}
    )
}