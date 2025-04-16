package me.apomazkin.settingstab

import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.settingstab.widgets.SettingsAppBar
import me.apomazkin.settingstab.widgets.SettingsItemWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.dividerColor
import me.apomazkin.ui.preview.PreviewScreen

@Composable
fun SettingsTabScreen(
    onLangManagementClick: () -> Unit,
    onAboutAppClick: () -> Unit,
) {
    //    val emailLauncher = rememberLauncherForActivityResult(SendEmailContract()) {
    //        Log.d("Email", "Email intent completed")
    //    }
    
    val emailLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        Log.d("Email", "Email intent completed")
    }
    
    val shareLauncher = rememberLauncherForActivityResult(AppShareContract()) {
        Log.d(
            "###",
            "<SettingsTabScreen.kt>::SettingsTabScreen => AppShareContract"
        )
    }
    
    Scaffold(
        topBar = { SettingsAppBar() },
    ) { paddings: PaddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddings),
            contentPadding = PaddingValues(
                horizontal = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Surface(
                    modifier = Modifier,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, color = dividerColor),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SettingsItemWidget(
                            iconRes = R.drawable.ic_lang,
                            titleRes = R.string.settings_section_lang_management,
                            showNextIcon = true,
                            onClick = onLangManagementClick
                        )
                        SettingsItemWidget(
                            iconRes = R.drawable.ic_about,
                            titleRes = R.string.settings_section_about_app,
                            showNextIcon = true,
                            onClick = onAboutAppClick
                        )
                    }
                }
            }
            item {
                Surface(
                    modifier = Modifier,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, color = dividerColor),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SettingsItemWidget(
                            iconRes = R.drawable.ic_feedback,
                            titleRes = R.string.settings_section_feedback,
                            showNextIcon = false,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "message/rfc822"
                                    putExtra(
                                        Intent.EXTRA_EMAIL,
                                        arrayOf("someone@example.com")
                                    )
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        "Тема письма"
                                    )
                                    putExtra(Intent.EXTRA_TEXT, "Текст письма")
                                }
                                val chooser = Intent.createChooser(
                                    intent,
                                    "Выберите почтовое приложение"
                                )
                                emailLauncher.launch(chooser)
                                //                                emailLauncher.launch("")
                            }
                        )
                        SettingsItemWidget(
                            iconRes = R.drawable.ic_rate,
                            titleRes = R.string.settings_section_rate,
                            showNextIcon = false,
                            onClick = { }
                        )
                        SettingsItemWidget(
                            iconRes = R.drawable.ic_share,
                            titleRes = R.string.settings_section_share,
                            showNextIcon = false,
                            onClick = {
                                shareLauncher.launch("Lexeme - year personal dictionary")
                            }
                        )
                    }
                }
            }
            
        }
    }
}

@Composable
@PreviewScreen
private fun Preview() {
    AppTheme {
        SettingsTabScreen(
            onLangManagementClick = {},
            onAboutAppClick = {}
        )
    }
}