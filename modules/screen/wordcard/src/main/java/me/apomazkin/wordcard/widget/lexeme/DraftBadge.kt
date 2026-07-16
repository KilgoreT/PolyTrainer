package me.apomazkin.wordcard.widget.lexeme

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.core_resources.R
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.draftBadgeBg
import me.apomazkin.theme.formTextHint
import me.apomazkin.ui.preview.PreviewWidget

/** Бейдж «Черновик» на карточке несохранённой лексемы (Figma 5027:1436). */
@Composable
internal fun DraftBadge(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = draftBadgeBg,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            text = stringResource(id = R.string.word_card_draft_badge),
            style = LexemeStyle.BodySBold,
            color = formTextHint,
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        DraftBadge()
    }
}
