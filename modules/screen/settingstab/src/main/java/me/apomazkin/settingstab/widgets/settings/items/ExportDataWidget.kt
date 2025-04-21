package me.apomazkin.settingstab.widgets.settings.items

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import me.apomazkin.settingstab.R
import me.apomazkin.settingstab.widgets.settings.items.base.SettingsItemWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun ExportDataWidget(
    onClick: (Uri) -> Unit,
) {
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri: Uri? -> uri?.let(onClick) }
    )
    SettingsItemWidget(
        iconRes = R.drawable.ic_move,
        titleRes = R.string.settings_section_export,
        showNextIcon = false,
        onClick = {
            launcher.launch(null)
        }
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    ExportDataWidget(
        onClick = {}
    )
}