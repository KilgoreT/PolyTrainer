package me.apomazkin.quiz.chat.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import me.apomazkin.quiz.chat.logic.ChatMessage
import me.apomazkin.quiz.chat.logic.ChatMessageState
import me.apomazkin.quiz.chat.logic.MessageContent
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.logic.isPreviousHasSameType
import me.apomazkin.quiz.chat.widget.message.SystemMessageWidget
import me.apomazkin.quiz.chat.widget.message.UserMessageWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun ChatMessageWidget(
    modifier: Modifier = Modifier,
    state: ChatMessageState,
    sendMessage: (Msg) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    
    LaunchedEffect(state) {
        launch {
            lazyListState.animateScrollToItem(state.list.lastIndex)
        }
    }
    
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(all = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        reverseLayout = false
    ) {
        itemsIndexed(
            items = state.list,
            key = { _, item -> item.order.toString() }
        ) { index: Int, item: ChatMessage ->
            
            val needSpaceBeforePrevious = !state.isPreviousHasSameType(index)
            if (needSpaceBeforePrevious) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (item.isSystemMessage) {
                val notShowAvatar = index < state.list.lastIndex
                        && state.list[index + 1].isSystemMessage
                val isInChain = index > 0
                        && state.list[index - 1].isSystemMessage
                val isLastMessage = index == state.list.lastIndex
                SystemMessageWidget(
                    showAvatar = notShowAvatar.not(),
                    isInChain = isInChain,
                    showButtons = isLastMessage,
                    message = item,
                    sendMessage = sendMessage
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
                        message = MessageContent.create(text = "Kill, World!")
                    ),
                )
            )
        ) {}
    }
}