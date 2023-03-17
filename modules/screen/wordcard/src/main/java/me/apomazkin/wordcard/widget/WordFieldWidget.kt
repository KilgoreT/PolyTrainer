@file:OptIn(ExperimentalMaterial3Api::class)

package me.apomazkin.wordcard.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.apomazkin.coloredtext.ColoredText
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu
import me.apomazkin.wordcard.mate.WordState

@Composable
fun WordFieldWidget(
    wordState: WordState,
    onEditClick: () -> Unit,
    onWordChange: (String) -> Unit,
    onSaveWord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!wordState.isEdit) {
            ColoredText(
                modifier = Modifier,
                text = wordState.value
            )
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onEditClick.invoke() }
                    .padding(8.dp),
                imageVector = Icons.Default.Edit,
                contentDescription = ""
            )
        } else {
            OutlinedTextField(
                value = wordState.edited,
                onValueChange = onWordChange
            )
            Spacer(modifier = Modifier.weight(1F))
            Icon(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onSaveWord.invoke() }
                    .padding(8.dp),
                imageVector = Icons.Default.Done,
                contentDescription = ""
            )
        }
    }
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun Preview() {
    AppTheme {
        WordFieldWidget(
            wordState = WordState(value = "Word"),
            onEditClick = {},
            onWordChange = {},
            onSaveWord = {}
        )
    }
}