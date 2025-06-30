package me.apomazkin.dictionaryappbar.widget

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.dictionaryappbar.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun AppBarTitleWidget(
        @StringRes titleResId: Int,
) {
    Text(
            text = stringResource(id = titleResId),
            style = LexemeStyle.H5.copy(
            color = MaterialTheme.colorScheme.secondary
        )
    )
}

@PreviewWidget
@Composable
fun AppBarTitleWidgetPreview() {
    AppBarTitleWidget(
            titleResId = R.string.quiz_tab_title
    )
}