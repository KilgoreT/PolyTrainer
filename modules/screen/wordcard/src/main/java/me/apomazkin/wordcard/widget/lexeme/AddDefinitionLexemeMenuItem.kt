package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.apomazkin.core_resources.R
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor

@Composable
internal fun AddDefinitionLexemeMenuItem(
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(id = R.string.word_card_lexeme_add_definition),
                style = LexemeStyle.BodyL,
                color = blackColor
            )
        },
        onClick = onClick,
    )
}