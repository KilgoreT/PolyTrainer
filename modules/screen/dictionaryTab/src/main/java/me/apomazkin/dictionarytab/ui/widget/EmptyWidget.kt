package me.apomazkin.dictionarytab.ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.apomazkin.dictionarytab.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun BoxScope.EmptyWidget() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.vocabulary_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box {
            EmptyWidget()
        }
    }
}