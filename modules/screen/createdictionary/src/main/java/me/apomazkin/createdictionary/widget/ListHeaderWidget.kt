package me.apomazkin.createdictionary.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.apomazkin.createdictionary.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.Typography
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun ColumnScope.ListHeaderWidget(
    @StringRes titleRes: Int,
    @StringRes subTitleRes: Int,
    modifier: Modifier = Modifier
) {
    Text(
        modifier = modifier,
        text = stringResource(id = titleRes),
        style = Typography.headlineMedium
            .copy(color = MaterialTheme.colorScheme.secondary)
    )
    Text(
        modifier = modifier,
        text = stringResource(id = subTitleRes),
        style = Typography.headlineSmall
            .copy(color = MaterialTheme.colorScheme.secondary)
    )
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(whiteColor)
        ) {
            ListHeaderWidget(
                titleRes = R.string.unknown_error,
                subTitleRes = R.string.unknown_error,
            )
        }
    }
}