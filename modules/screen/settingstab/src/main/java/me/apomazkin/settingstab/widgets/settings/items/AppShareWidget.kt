package me.apomazkin.settingstab.widgets.settings.items

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import me.apomazkin.settingstab.R
import me.apomazkin.settingstab.contract.AppShareContract
import me.apomazkin.settingstab.widgets.settings.items.base.SettingsItemWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun AppShareWidget() {
    val shareLauncher = rememberLauncherForActivityResult(AppShareContract()) {}
    SettingsItemWidget(
        iconRes = R.drawable.ic_share,
        titleRes = R.string.settings_section_share,
        showNextIcon = false,
        onClick = {
            shareLauncher.launch("Lexeme - year personal dictionary")
        }
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    AppShareWidget()
}