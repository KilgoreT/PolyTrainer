package me.apomazkin.dictionarytab.ui.widget.lexeme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionarytab.entity.TranslationUiEntity
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.translationBgColor

@Composable
fun TranslationWidget(
    modifier: Modifier = Modifier,
    translation: TranslationUiEntity,
) {
    Text(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(MaterialTheme.colorScheme.tertiary)
            .drawBehind {
                val strokeWidth = 2.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = translationBgColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = 8.dp)
            .padding(vertical = 4.dp),
        text = translation.value,
        style = LexemeStyle.BodyM
            .copy(color = MaterialTheme.colorScheme.primary),

        )
}