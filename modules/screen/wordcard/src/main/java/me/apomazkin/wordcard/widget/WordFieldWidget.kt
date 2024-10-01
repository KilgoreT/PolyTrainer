package me.apomazkin.wordcard.widget

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.blackColor
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu
import me.apomazkin.wordcard.mate.WordState

@Composable
fun WordFieldWidget(
    wordState: WordState,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = wordState.value,
            style = MaterialTheme.typography.titleLarge,
            color = blackColor,
        )
        IconBoxed(
            iconRes = R.drawable.ic_add,
            colorEnabled = blackColor,
            enabled = !wordState.isEdit
        ) { onEditClick.invoke() }
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
        )
    }
}