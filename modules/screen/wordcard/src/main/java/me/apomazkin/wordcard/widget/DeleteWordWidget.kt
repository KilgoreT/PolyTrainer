package me.apomazkin.wordcard.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.btn.ErrorButtonWidget
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu
import me.apomazkin.wordcard.R

@Composable
fun DeleteWordWidget(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    onDelete: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        ErrorButtonWidget(
            iconRes = R.drawable.ic_delete,
            titleRes = R.string.word_card_delete_word,
            enabled = enabled,
        ) { onDelete.invoke() }
    }
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun Preview() {
    AppTheme {
        Column {
            DeleteWordWidget {}
        }
    }
}