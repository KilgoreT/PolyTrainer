package me.apomazkin.quiz.chat.widget.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.widget.button.base.ChatButtonWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun ShowAnswerButtonWidget(
    sendMessage: (Msg) -> Unit
) {
    ChatButtonWidget(
        title = R.string.chat_quiz_msg_user_show_answer
    ) {
        sendMessage.invoke(Msg.GetAnswer)
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Gray)
        ) {
            ShowAnswerButtonWidget {}
        }
    }
}