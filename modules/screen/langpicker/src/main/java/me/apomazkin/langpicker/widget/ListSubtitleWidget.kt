package me.apomazkin.langpicker.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.langpicker.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu

fun LazyListScope.listSubtitleWidget(
    onClick: () -> Unit
) {
    item {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TextButton(
                onClick = onClick,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.secondary
                ),
            ) {
                Text(
                    text = stringResource(id = R.string.lang_selection_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
@PreviewWidgetRu
@PreviewWidgetEn
private fun Preview() {
    AppTheme {
        LazyColumn {
            listSubtitleWidget { }
        }
    }
}