package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.runtime.Composable
import me.apomazkin.core_resources.R
import me.apomazkin.icondropdowned.MenuItem
import me.apomazkin.icondropdowned.StringSource
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun AddTranslationLexemeMenuItem(
        onChangeClick: () -> Unit,
) {
    MenuItem
            .text(
                    title = StringSource.fromRes(
                            resId = R.string.word_card_lexeme_add_translation,
                            style = LexemeStyle.BodyL,
                    ),
                    onClick = onChangeClick,
            )
            .Widget()
}

@Composable
@PreviewWidget
private fun AddTranslationLexemeMenuItemPreview() {
    AppTheme {
        AddTranslationLexemeMenuItem(
                onChangeClick = {}
        )
    }
}