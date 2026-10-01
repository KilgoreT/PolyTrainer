package me.apomazkin.ui.text

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Мелкая метка-чип: короткий признак рядом с текстом (часть речи у
 * значения в вопросе тренировки и подобное).
 *
 * Одна строка, длинный текст режется многоточием; кегль — слот
 * `labelSmall` темы, цвета — пара `secondaryContainer` /
 * `onSecondaryContainer`. Размеры снаружи задаёт место, где чип стоит:
 * сам он только не растёт выше строки своего текста.
 */
@Composable
fun LexemeBadgeChip(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Row {
            LexemeBadgeChip(text = "сущ.")
            LexemeBadgeChip(
                text = "очень длинная пользовательская опция",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .widthIn(max = 120.dp),
            )
        }
    }
}
