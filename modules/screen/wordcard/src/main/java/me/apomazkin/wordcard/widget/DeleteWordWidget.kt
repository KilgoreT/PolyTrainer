package me.apomazkin.wordcard.widget

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.wordcard.R

@Composable
fun ColumnScope.DeleteWordWidget(
    onDelete: () -> Unit,
) {
    Spacer(modifier = Modifier.weight(1F))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        contentAlignment = Alignment.BottomCenter,
    ) {
        OutlinedButton(onClick = onDelete) {
            Text(text = stringResource(id = R.string.word_card_delete_word))
        }
    }
}