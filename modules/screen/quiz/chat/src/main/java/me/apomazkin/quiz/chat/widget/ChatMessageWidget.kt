package me.apomazkin.quiz.chat.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.logic.ChatMessage
import me.apomazkin.quiz.chat.logic.ChatMessageState
import me.apomazkin.quiz.chat.widget.message.SystemMessageWidget
import me.apomazkin.quiz.chat.widget.message.UserMessageWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun ChatMessageWidget(
    modifier: Modifier = Modifier,
    state: ChatMessageState,
) {
    val lazyListState = rememberLazyListState()
    
    LaunchedEffect(state) {
        lazyListState.animateScrollToItem(state.list.lastIndex)
    }
    
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = false
    ) {
        itemsIndexed(
            items = state.list,
            key = { _, item -> item.order.toString() }
        ) { index: Int, item: ChatMessage ->
            val isNotLast = index < state.list.lastIndex
                    && state.list[index + 1].isSystemMessage
            if (item.isSystemMessage) {
                SystemMessageWidget(
                    showAvatar = isNotLast.not(),
                    message = item.message
                )
            } else {
                UserMessageWidget(message = item.message)
            }
        }
    }
}


@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        ChatMessageWidget(
            state = ChatMessageState(
                list = listOf(
                    ChatMessage.addSystemMessage(
                        order = 1,
                        message = "Hello, World!"
                    ),
                    ChatMessage.addSystemMessage(
                        order = 2,
                        message = "Fuck, World!"
                    ),
                    ChatMessage.addUserMessage(
                        order = 3,
                        message = "Kill, World!"
                    ),
                )
            )
        )
    }
}