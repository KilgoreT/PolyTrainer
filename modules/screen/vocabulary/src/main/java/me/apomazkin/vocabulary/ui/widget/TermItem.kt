package me.apomazkin.vocabulary.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.IconBoxed
import me.apomazkin.ui.preview.PreviewWidgetRu
import me.apomazkin.vocabulary.R
import me.apomazkin.vocabulary.entity.TermUiItem
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.tools.DataHelper

@Composable
internal fun TermItem(
    termItem: TermUiItem,
    onOpenWordCard: (wordId: Long) -> Unit,
    sendMsg: (Msg) -> Unit,
) {

    Surface(
        modifier = Modifier,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { sendMsg(Msg.ExpandTerm(termItem.id, !termItem.isExpand)) },
                verticalAlignment = CenterVertically,
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .weight(1F),
                    text = termItem.wordValue,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                if (termItem.lexemeList.isEmpty()) {
                    IconBoxed(
                        iconRes = R.drawable.ic_add_circled
                    ) {}
                }
                IconBoxed(
                    iconRes = R.drawable.ic_edit,
                    enabled = true, //TODO
                ) {
                    onOpenWordCard.invoke(termItem.id)
                }
            }
        }
    }

//    Column {
//        if (termItem.lexemeList.isNotEmpty()) {
//            Divider(
//                modifier = Modifier
//                    .fillMaxWidth()
//            )
//        }
//        termItem.lexemeList.forEach {
//            key(it.id) {
//                LexemeItem(lexeme = it)
//            }
//        }
//    }
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
                onOpenWordCard = {},
            ) {}
        }
    }
}