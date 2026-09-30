package me.apomazkin.quiz.chat.widget.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Действия по текущему вопросу — элемент ленты чата, прокручивается
 * вместе с сообщениями. Показ/скрытие — по `ChatState.showUserActions`.
 */
@Composable
fun UserActionsWidget(
    modifier: Modifier = Modifier,
    sendMessage: (Msg) -> Unit,
) {
    ActionChipsRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        onShowAnswer = { sendMessage(Msg.GetAnswer) },
        onSkip = { sendMessage(Msg.Skip) },
    )
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(modifier = Modifier.fillMaxWidth().background(color = Color.Gray)) {
            UserActionsWidget {}
        }
    }
}
