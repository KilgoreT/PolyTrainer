package me.apomazkin.wordstab.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import me.apomazkin.wordrow.entity.TermUiItem
import me.apomazkin.wordrow.ui.WordRowWidget
import me.apomazkin.wordstab.entity.WordInfo
import me.apomazkin.wordstab.logic.Msg

@Composable
internal fun WordListWidget(
        termList: LazyPagingItems<TermUiItem>,
        modifier: Modifier = Modifier,
        openWordCard: (word: WordInfo) -> Unit,
        sendMessage: (Msg) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            top = 4.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(termList.itemCount) { index ->
            termList[index]?.let { term ->
                // Вёрстка строки — wordrow (IS493/Э2); selection-логика (Msg)
                // остаётся здесь: колбеки маппятся в WordInfo/Msg (D8.2).
                WordRowWidget(
                        termItem = term,
                        onClick = { openWordCard(WordInfo(it.id, it.wordValue)) },
                        onLongClick = {
                            sendMessage(
                                    Msg.EnterSelectionMode(
                                            targetWord = WordInfo(
                                                    id = it.id,
                                                    wordValue = it.wordValue,
                                            ),
                                    )
                            )
                        },
                )
            }
        }
    }
}
