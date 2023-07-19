@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package me.apomazkin.vocabulary.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.OutlineTextFieldWidget
import me.apomazkin.ui.btn.PrimaryFabWidget
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu
import me.apomazkin.vocabulary.R

@Composable
fun WordEditWidget(
    label: String,
    wordValue: String,
    onWordValueChange: (String) -> Unit,
    onAddWord: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(
            modifier = Modifier
                .weight(1F, fill = true),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall
            )
            OutlineTextFieldWidget(
                value = wordValue,
                onValueChange = onWordValueChange
            )
        }
        PrimaryFabWidget(
            iconRes = R.drawable.ic_send,
        ) { onAddWord.invoke(wordValue) }
    }
}

@PreviewWidgetRu
@PreviewWidgetEn
@Composable
private fun Preview() {
    AppTheme {
        WordEditWidget(
            label = "label",
            wordValue = "value",
            onWordValueChange = {},
            onAddWord = {}
        )
    }
}