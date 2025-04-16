@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.settingstab.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.apomazkin.settingstab.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun SettingsAppBar() {
    TopAppBar(
        title = {
            Text(
                text = stringResource(id = R.string.item_title_settings),
                style = LexemeStyle.H5,
            )
        }
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            SettingsAppBar()
        }
    }
}