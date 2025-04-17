package me.apomazkin.settingstab.widgets.settings.items

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import me.apomazkin.settingstab.R
import me.apomazkin.settingstab.widgets.settings.items.base.SettingsItemWidget
import me.apomazkin.ui.preview.PreviewWidget

private const val DEFAULT_MIME_TYPE = "message/rfc822"
private const val EMAIL = "lexeme.app@gmail.com"

@Composable
fun FeedBackWidget() {
    val emailLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}
    
    SettingsItemWidget(
        iconRes = R.drawable.ic_feedback,
        titleRes = R.string.settings_section_feedback,
        showNextIcon = false,
        onClick = {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = DEFAULT_MIME_TYPE
                putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, "Тема письма")
                putExtra(Intent.EXTRA_TEXT, "Текст письма")
            }
            val chooser = Intent.createChooser(
                intent,
                "Выберите почтовое приложение"
            )
            emailLauncher.launch(chooser)
        }
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    FeedBackWidget()
}