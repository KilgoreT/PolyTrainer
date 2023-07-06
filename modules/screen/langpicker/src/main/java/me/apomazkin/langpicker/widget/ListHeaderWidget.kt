package me.apomazkin.langpicker.widget

import androidx.annotation.StringRes
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.apomazkin.langpicker.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.Typography
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

@Composable
fun ListHeaderWidget(
    @StringRes titleRes: Int,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = stringResource(id = titleRes),
        style = Typography.titleMedium
    )
}

@PreviewWidgetRu
@PreviewWidgetEn
@Composable
private fun Preview() {
    AppTheme {
        ListHeaderWidget(titleRes = R.string.unknown_error)
    }
}