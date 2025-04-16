package me.apomazkin.settingstab

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.settingstab.widgets.AboutAppBar
import me.apomazkin.settingstab.widgets.AppDescriptionWidget
import me.apomazkin.settingstab.widgets.LogoVersionWidget
import me.apomazkin.settingstab.widgets.SettingsItemWidget
import me.apomazkin.theme.dividerColor
import me.apomazkin.ui.preview.PreviewScreen

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
                horizontal = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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
                Surface(
                    modifier = Modifier
                        .padding(top = 20.dp),
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
}

@Composable
@PreviewScreen
private fun Preview() {
    AboutAppScreen(
        onBackPress = {}
    )
}