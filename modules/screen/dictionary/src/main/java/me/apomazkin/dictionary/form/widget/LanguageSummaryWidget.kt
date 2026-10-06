package me.apomazkin.dictionary.form.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeColor
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.formTextSecondary
import me.apomazkin.ui.preview.PreviewWidget

/**
 * IS525, минимальный вид: «<изучаемый> → <перевод>». Нажатие на язык
 * открывает его выбор. Оформление — вторым заходом.
 */
@Composable
internal fun LanguageSummaryWidget(
    learningName: String,
    translationName: String,
    onLearningClick: () -> Unit,
    onTranslationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = learningName,
            style = LexemeStyle.BodyM,
            color = LexemeColor.primary,
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onLearningClick)
                .padding(vertical = 12.dp),
        )
        Text(
            text = "→",
            style = LexemeStyle.BodyM,
            color = formTextSecondary,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Text(
            text = translationName,
            style = LexemeStyle.BodyM,
            color = LexemeColor.primary,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onTranslationClick)
                .padding(vertical = 12.dp),
        )
    }
}

@Composable
@PreviewWidget
private fun Preview() {
    AppTheme {
        LanguageSummaryWidget(
            learningName = "Испанский (Мексика)",
            translationName = "Русский",
            onLearningClick = {},
            onTranslationClick = {},
        )
    }
}
