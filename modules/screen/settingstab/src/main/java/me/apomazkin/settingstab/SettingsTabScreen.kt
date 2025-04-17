package me.apomazkin.settingstab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.settingstab.widgets.settings.SettingsAppBar
import me.apomazkin.settingstab.widgets.settings.SettingsSectionWidget
import me.apomazkin.settingstab.widgets.settings.items.AboutAppWidget
import me.apomazkin.settingstab.widgets.settings.items.AppShareWidget
import me.apomazkin.settingstab.widgets.settings.items.ExportDataWidget
import me.apomazkin.settingstab.widgets.settings.items.FeedBackWidget
import me.apomazkin.settingstab.widgets.settings.items.ImportDataWidget
import me.apomazkin.settingstab.widgets.settings.items.LangManageWidget
import me.apomazkin.settingstab.widgets.settings.items.RateWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewScreen

@Composable
fun SettingsTabScreen(
    onLangManagementClick: () -> Unit,
    onAboutAppClick: () -> Unit,
) {
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
                SettingsSectionWidget {
                    LangManageWidget(onClick = onLangManagementClick)
                    AboutAppWidget(onClick = onAboutAppClick)
                }
            }
            item {
                SettingsSectionWidget {
                    ExportDataWidget(onClick = {})
                    ImportDataWidget(onClick = {})
                }
            }
            item {
                SettingsSectionWidget {
                    FeedBackWidget()
                    RateWidget(onClick = null)
                    AppShareWidget()
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