@file:OptIn(ExperimentalFoundationApi::class)

package me.apomazkin.dictionarytab.ui.widget

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
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.dictionarytab.logic.Msg
import me.apomazkin.dictionarytab.tools.DataHelper
import me.apomazkin.dictionarytab.ui.widget.lexeme.LexemeWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.blackColor
import me.apomazkin.theme.dividerColor
import me.apomazkin.ui.preview.PreviewWidget

@Composable
internal fun TermWidget(
        termItem: TermUiItem,
        openWordCard: (word: WordInfo) -> Unit,
        sendMsg: (Msg) -> Unit,
) {
    Surface(
            modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .combinedClickable(
                            onLongClick = {
                                sendMsg(
                                        Msg.ShowActionMode(
                                                targetWord = WordInfo(
                                                        id = termItem.id,
                                                        wordValue = termItem.wordValue,
                                                ),
                                        )
                                )
                            },
                            onClick = { openWordCard(WordInfo(termItem.id, termItem.wordValue)) }
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

@PreviewWidget
@Composable
private fun Preview1() {
    AppTheme {
        Box(
                modifier = Modifier
                        .padding(16.dp)
        ) {
            TermWidget(
                    termItem = DataHelper.Data.termList.first().copy(
                            lexemeList = DataHelper.Data.termList
                                    .first()
                                    .lexemeList
                                    .take(2)
                    ),
                    openWordCard = {},
            ) {}
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
            TermWidget(
                    termItem = DataHelper.Data.termList[1].copy(
                            lexemeList = DataHelper.Data.termList[1]
                                    .lexemeList
                                    .take(2)
                    ),
                    openWordCard = {},
            ) {}
        }
    }
}