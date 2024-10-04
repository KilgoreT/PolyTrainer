package me.apomazkin.vocabulary.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.ui.preview.PreviewWidgetRu
import me.apomazkin.vocabulary.entity.TermUiItem
import me.apomazkin.vocabulary.entity.WordInfo
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.tools.DataHelper

@Composable
internal fun WordListWidget(
    termList: List<TermUiItem>,
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
        items(termList) { item: TermUiItem ->
            TermItem(
                termItem = item,
                openWordCard = openWordCard,
            ) { sendMessage(it) }
        }
    }
}

@PreviewWidgetEn
@PreviewWidgetRu
@Composable
private fun Preview() {
    AppTheme {
        WordListWidget(
            termList = DataHelper.Data.termList,
            openWordCard = {}
        ) {}
    }
}