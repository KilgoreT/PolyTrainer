@file:OptIn(ExperimentalFoundationApi::class)

package me.apomazkin.wordrow.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.theme.dividerColor
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordrow.entity.DefinitionUiEntity
import me.apomazkin.wordrow.entity.LexemeUiItem
import me.apomazkin.wordrow.entity.TermUiItem
import me.apomazkin.wordrow.entity.TranslationUiEntity
import me.apomazkin.wordrow.ui.lexeme.LexemeWidget
import java.util.Date

/**
 * Строка слова — вёрстка бывшего `TermWidget` (wordstab), вынесена в IS493/Э2
 * для переиспользования (words + группы).
 *
 * Контракт (D8.2): колбеки вместо Msg — виджет не знает про логику потребителя.
 * `onLongClick == null` — long-press НЕ регистрируется (read-only потребители,
 * например список слов группы): null обязан доходить до [combinedClickable]
 * null'ом, а не пустой лямбдой — иначе жест потребляется (haptic/ripple)
 * при отсутствующем действии.
 */
@Composable
fun WordRowWidget(
    termItem: TermUiItem,
    onClick: (TermUiItem) -> Unit,
    onLongClick: ((TermUiItem) -> Unit)? = null,
) {
    Surface(
            modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                            onLongClick = onLongClick?.let { cb -> { cb(termItem) } },
                            onClick = { onClick(termItem) }
                    ),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(
                    width = 1.dp,
                    color = if (termItem.isSelected) blackColor else dividerColor
            ),
    ) {
        Column(
                modifier = Modifier
                        .padding(vertical = 12.dp)
        ) {
            Text(
                    modifier = Modifier
                            .padding(horizontal = 16.dp),
                    text = termItem.wordValue,
                    style = LexemeStyle.BodyXLBold,
                    color = MaterialTheme.colorScheme.secondary,
            )
            if (termItem.lexemeList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            termItem.lexemeList.forEachIndexed { index, lexeme ->
                LexemeWidget(
                        modifier = Modifier
                                .padding(horizontal = 16.dp),
                        lexeme = lexeme
                )
                if (index < termItem.lexemeList.size - 1) {
                    HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = dividerColor,
                    )
                }
            }
        }
    }
}

internal val previewTermItem = TermUiItem(
    id = 0,
    wordValue = "uno",
    dictionaryId = 0,
    lexemeList = listOf(
        LexemeUiItem(
            id = 0,
            translation = TranslationUiEntity("одын"),
            definition = DefinitionUiEntity("одын одын одын одын одын одын"),
            addDate = Date(0),
        ),
        LexemeUiItem(
            id = 1,
            translation = TranslationUiEntity("единица"),
            definition = DefinitionUiEntity("раз-раз раз-раз раз-раз раз-раз"),
            addDate = Date(0),
        ),
    ),
    addDate = Date(0),
)

@PreviewWidget
@Composable
private fun Preview1() {
    AppTheme {
        Box(
                modifier = Modifier
                        .padding(16.dp)
        ) {
            WordRowWidget(
                    termItem = previewTermItem,
                    onClick = {},
            )
        }
    }
}

@PreviewWidget
@Composable
private fun Preview2() {
    AppTheme {
        Box(
                modifier = Modifier
                        .padding(16.dp)
        ) {
            WordRowWidget(
                    termItem = previewTermItem.copy(
                            wordValue = "dos",
                            isSelected = true,
                            lexemeList = previewTermItem.lexemeList.take(1),
                    ),
                    onClick = {},
                    onLongClick = {},
            )
        }
    }
}
