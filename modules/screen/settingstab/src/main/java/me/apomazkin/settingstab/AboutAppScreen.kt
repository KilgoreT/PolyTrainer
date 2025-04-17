package me.apomazkin.settingstab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.settingstab.widgets.about.AboutAppBar
import me.apomazkin.settingstab.widgets.about.AppDescriptionWidget
import me.apomazkin.settingstab.widgets.about.LogoVersionWidget
import me.apomazkin.settingstab.widgets.settings.SettingsSectionWidget
import me.apomazkin.settingstab.widgets.settings.items.base.SettingsItemWidget
import me.apomazkin.ui.preview.PreviewScreen

private const val DEFAULT_HORIZONTAL_PADDING = 16
private const val DEFAULT_VERTICAL_ARRANGEMENT = 8

@Composable
fun AboutAppScreen(
    onBackPress: () -> Unit
) {
    
    Scaffold(
        topBar = { AboutAppBar(onBackPress = onBackPress) },
    ) { paddings: PaddingValues ->
        
        LazyColumn(
            modifier = Modifier
                .padding(paddings),
            contentPadding = PaddingValues(
                horizontal = DEFAULT_HORIZONTAL_PADDING.dp
            ),
            verticalArrangement = Arrangement
                .spacedBy(DEFAULT_VERTICAL_ARRANGEMENT.dp),
        ) {
            
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 44.dp),
                ) {
                    LogoVersionWidget(
                        imageRes = R.drawable.ic_logo,
                        titleRes = R.string.logo_title,
                        versionTitle = "debug"
                    )
                }
            }
            
            item {
                AppDescriptionWidget(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                )
            }
            
            item {
                SettingsSectionWidget {
                    SettingsItemWidget(
                        iconRes = R.drawable.ic_feedback,
                        titleRes = R.string.settings_section_feedback,
                        showNextIcon = true,
                        onClick = { }
                    )
                    SettingsItemWidget(
                        iconRes = R.drawable.ic_rate,
                        titleRes = R.string.settings_section_rate,
                        showNextIcon = true,
                        onClick = { }
                    )
                }
            }
        }
    }
}

@Composable
@PreviewScreen
private fun Preview() {
    AboutAppScreen(
        onBackPress = {}
    )
}