package me.apomazkin.dictionarytab.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.dictionarytab.logic.Msg

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
                TermWidget(
                        termItem = term,
                        openWordCard = openWordCard,
                ) { sendMessage(it) }
            }
        }
    }
}

//@PreviewWidget
//@Composable
//private fun Preview() {
//    AppTheme {
//        WordListWidget(
//            termList = DataHelper.Data.termList,
//            openWordCard = {}
//        ) {}
//    }
//}