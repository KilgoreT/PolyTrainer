package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import me.apomazkin.core_resources.R
import me.apomazkin.icondropdowned.MenuItem
import me.apomazkin.icondropdowned.StringSource
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun DeleteLexemeMenuItem(
        onDeleteClick: () -> Unit,
) {
    MenuItem
            .text(
                    title = StringSource.fromRes(
                            resId = R.string.word_card_lexeme_delete,
                            style = LexemeStyle.BodyL,
                            color = MaterialTheme.colorScheme.onError
                    ),
                    onClick = onDeleteClick,
            )
            .Widget()
}

@Composable
@PreviewWidget
private fun DeleteLexemeMenuItemPreview() {
    AppTheme {
        DeleteLexemeMenuItem(
                onDeleteClick = {}
        )
    }
}