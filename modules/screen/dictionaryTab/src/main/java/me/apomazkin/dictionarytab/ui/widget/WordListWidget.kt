package me.apomazkin.dictionarytab.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.dictionarytab.entity.TermUiItem
import me.apomazkin.dictionarytab.entity.WordInfo
import me.apomazkin.dictionarytab.logic.Msg
import me.apomazkin.dictionarytab.tools.DataHelper
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

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
            TermWidget(
                termItem = item,
                openWordCard = openWordCard,
            ) { sendMessage(it) }
        }
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        WordListWidget(
            termList = DataHelper.Data.termList,
            openWordCard = {}
        ) {}
    }
}