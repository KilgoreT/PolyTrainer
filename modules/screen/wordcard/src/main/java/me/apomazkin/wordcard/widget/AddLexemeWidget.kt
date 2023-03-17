package me.apomazkin.wordcard.widget

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.apomazkin.wordcard.R

@Composable
fun AddLexemeWidget(
    enabled: Boolean,
    onAddLexeme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TextButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onAddLexeme
    ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = "")
        Text(text = stringResource(id = R.string.word_card_add_lexeme))
    }
}