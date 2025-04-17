package me.apomazkin.settingstab.widgets.about

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.apomazkin.settingstab.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun AppDescriptionWidget(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = stringResource(id = R.string.settings_section_about_app_title),
        color = MaterialTheme.colorScheme.secondary,
        style = LexemeStyle.BodyM,
    )
}

@Composable
@PreviewWidget
private fun Preview() {
    AppDescriptionWidget()
}
