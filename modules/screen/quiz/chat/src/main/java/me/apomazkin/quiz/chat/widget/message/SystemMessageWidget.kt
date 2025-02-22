package me.apomazkin.quiz.chat.widget.message

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun SystemMessageWidget(
    modifier: Modifier = Modifier,
    message: ChatMessage.MessageValue,
    showAvatar: Boolean,
) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (showAvatar) {
                AvatarWidget(avatarRes = R.drawable.ic_logo)
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            Surface(
                modifier = Modifier,
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomEnd = 20.dp,
                    bottomStart = 8.dp,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = message.asText(),
                        style = LexemeStyle.BodyM.copy(
                            color = MaterialTheme.colorScheme.secondary
                        ),
                    )
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
                message = ChatMessage.plain("Привет! Готов начать тренировку? \uD83D\uDCAA"),
                showAvatar = isLast
            )
        }
    }
}

