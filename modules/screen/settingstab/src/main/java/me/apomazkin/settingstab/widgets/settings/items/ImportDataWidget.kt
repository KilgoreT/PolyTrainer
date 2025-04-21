package me.apomazkin.settingstab.widgets.settings.items

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import me.apomazkin.settingstab.R
import me.apomazkin.settingstab.contract.PickFileContract
import me.apomazkin.settingstab.widgets.settings.items.base.SettingsItemWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun ImportDataWidget(
    onClick: (Uri) -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = PickFileContract(),
        onResult = { uri: Uri? -> uri?.let(onClick) }
    )
    
    SettingsItemWidget(
        iconRes = R.drawable.ic_move,
        titleRes = R.string.settings_section_import,
        showNextIcon = false,
        onClick = {
            launcher.launch(arrayOf("application/octet-stream"))
        }
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    ImportDataWidget(
        onClick = {}
    )
}