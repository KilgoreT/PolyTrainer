@file:OptIn(ExperimentalFoundationApi::class)

package me.apomazkin.vocabulary.ui.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import me.apomazkin.ui.preview.PreviewWidgetRu
import me.apomazkin.vocabulary.entity.TermUiItem
import me.apomazkin.vocabulary.entity.WordInfo
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.tools.DataHelper

@Composable
internal fun TermItem(
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
                        Msg.ChangeActionMode(
                            isActionMode = true,
                            targetWord = WordInfo(termItem.id, termItem.wordValue),
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
        Column {
            Text(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                text = termItem.wordValue,
                style = LexemeStyle.BodyXLBold,
                color = MaterialTheme.colorScheme.onSecondary,
            )
        }
    }
}

@PreviewWidgetRu
@Composable
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .padding(16.dp)
        ) {
            TermItem(
                termItem = DataHelper.Data.termList.first(),
                openWordCard = {},
            ) {}
        }
    }
}