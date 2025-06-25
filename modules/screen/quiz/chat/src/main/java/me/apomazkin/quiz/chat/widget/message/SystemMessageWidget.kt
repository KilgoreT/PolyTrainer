package me.apomazkin.quiz.chat.widget.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.logic.ChatMessage
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun SystemMessageWidget(
    modifier: Modifier = Modifier,
    message: ChatMessage,
    showAvatar: Boolean,
    isInChain: Boolean,
    showButtons: Boolean,
    sendMessage: (Msg) -> Unit,
) {
    Box(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (showAvatar) {
                AvatarWidget(avatarRes = R.drawable.ic_logo)
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
            Surface(
                modifier = Modifier,
                shape = RoundedCornerShape(
                    topStart = if (isInChain) 8.dp else 20.dp,
                    topEnd = 20.dp,
                    bottomEnd = 20.dp,
                    bottomStart = 8.dp,
                ),
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = message.message.asText(),
                        style = LexemeStyle.BodyM.copy(
                            color = MaterialTheme.colorScheme.secondary
                        ),
                    )
                    if (showButtons) {
                        message.buttons.forEachIndexed { index, it ->
                            val offset = if (index == 0) 12 else 4
                            Spacer(modifier = Modifier.height(offset.dp))
                            MessageButtonWidget(
                                titleRes = it.title,
                            ) { sendMessage(Msg.UserAction(it.action)) }
                        }
                    }
                }
            }
        }
    }
}

@PreviewWidget
@Composable
private fun Preview(
    @PreviewParameter(BoolParam::class) isLast: Boolean
) {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Gray)
        ) {
            SystemMessageWidget(
                message = ChatMessage(
                    isSystemMessage = true,
                    message = ChatMessage.MessageValue.Plain("This is system message"),
                    buttons = listOf(
                        ChatMessage.ChatButton(
                            R.string.button_cancel,
                            ChatMessage.Companion.UserAction.EXIT
                        ),
                        ChatMessage.ChatButton(
                            R.string.button_cancel,
                            ChatMessage.Companion.UserAction.EXIT
                        ),
                        ChatMessage.ChatButton(
                            R.string.button_cancel,
                            ChatMessage.Companion.UserAction.EXIT
                        ),
                    )
                ),
                showAvatar = isLast,
                isInChain = false,
                showButtons = true,
            ) {}
        }
    }
}

