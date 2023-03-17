package me.apomazkin.vocabulary.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import me.apomazkin.icondropdowned.DropData
import me.apomazkin.icondropdowned.DropDataItem
import me.apomazkin.theme.FFD9E3
import me.apomazkin.vocabulary.entity.TermUiItem
import me.apomazkin.vocabulary.logic.Msg

val dropData = DropData(
    icon = Icons.Default.MoreVert,
    items = listOf(
        DropDataItem.Edit(),
        DropDataItem.Delete(),
    )
)

@Composable
internal fun TermItem(
    termItem: TermUiItem,
    onOpenWordCard: (wordId: Long) -> Unit,
    sendMsg: (Msg) -> Unit,
) {
    // TODO: Эта логика перенесется на лексемы, чтобы отображалась полная инфморация через аккардеон
    val icon by remember(termItem.isExpand) {
        derivedStateOf {
            if (termItem.isExpand) Icons.Default.KeyboardArrowUp
            else Icons.Default.KeyboardArrowDown
        }
    }
    Column {
        Row(
            modifier = Modifier
                .clickable { sendMsg(Msg.ExpandTerm(termItem.id, !termItem.isExpand)) },
            verticalAlignment = CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(FFD9E3)
                    .padding(horizontal = 8.dp),
                text = termItem.wordValue,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.weight(1F))
            Icon(
                modifier = Modifier
                    .align(CenterVertically)
                    .clickable {
                        onOpenWordCard.invoke(termItem.id)
//                        sendMsg(
//                            WordDetailMsg.Show(
//                                wordId = termItem.id,
//                                word = termItem.wordValue
//                            )
//                        )
                    },
                imageVector = Icons.Default.Edit,
                contentDescription = ""
            )
        }
        if (termItem.lexemeList.isNotEmpty()) {
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
        termItem.lexemeList.forEach {
            key(it.id) {
                LexemeItem(lexeme = it)
            }
        }
    }
}

//@PreviewWidget
//@Composable
//private fun Preview() {
//    AppTheme {
//        TermItem(
//            termItem = DataHelper.Data.termList.first(),
//        ) {}
//    }
//}