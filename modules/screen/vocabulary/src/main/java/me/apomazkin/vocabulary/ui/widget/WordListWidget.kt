package me.apomazkin.vocabulary.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import me.apomazkin.vocabulary.entity.TermUiItem
import me.apomazkin.vocabulary.logic.Msg

@Composable
internal fun WordListWidget(
    termList: List<TermUiItem>,
    onOpenWordCard: (wordId: Long) -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(termList) { item: TermUiItem ->
            TermItem(
                termItem = item,
                onOpenWordCard = onOpenWordCard,
            ) { sendMessage(it) }
        }
    }
}

//@PreviewWidgetEn
//@PreviewWidgetRu
//@Composable
//private fun Preview() {
//    AppTheme {
//        WordListWidget(termList = DataHelper.Data.termList, onOpenWordCard) {}
//    }
//}