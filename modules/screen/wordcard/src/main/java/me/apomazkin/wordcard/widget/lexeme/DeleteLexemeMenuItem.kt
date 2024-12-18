package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeStyle

@Composable
internal fun DeleteLexemeMenuItem(
    onDeleteClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(id = R.string.word_card_lexeme_delete),
                style = LexemeStyle.BodyL,
                color = MaterialTheme.colorScheme.onError
            )
        },
        onClick = onDeleteClick,
    )
}