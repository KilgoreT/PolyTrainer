package me.apomazkin.quiztab.widget

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.quiztab.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun AppBarTitleWidget() {
    Text(
        text = stringResource(R.string.quiz_tab_title),
        style = LexemeStyle.H5.copy(
            color = MaterialTheme.colorScheme.secondary
        )
    )
}

@PreviewWidget
@Composable
fun AppBarTitleWidgetPreview() {
    AppBarTitleWidget()
}