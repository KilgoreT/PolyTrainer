package me.apomazkin.wordcard.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.btn.PrimaryLongFabWidget
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordcard.R

@Composable
internal fun AddLexemeWidget(
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    onAddLexeme: () -> Unit,
) {
    PrimaryLongFabWidget(
        modifier = modifier,
        iconRes = R.drawable.ic_add_value,
        titleRes = R.string.word_card_add_lexeme,
        enabled = enabled,
    ) { onAddLexeme.invoke() }
}

@PreviewWidget
@Composable
private fun Preview(
    @PreviewParameter(BoolParam::class) enabled: Boolean
) {
    AppTheme {
        Box {
            AddLexemeWidget(
                enabled = enabled
            ) {}
        }
    }
}
