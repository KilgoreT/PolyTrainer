package me.apomazkin.quiz.chat.widget.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.widget.ChatMotion
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Системная кнопка «Начать» — элемент ленты под приветствием: высокая,
 * широкая, по центру, в цвете primary. Нажатие превращает её в пузырь
 * юзера «Начать»: одной кривой сжимается до пузыря, уезжает вправо, углы
 * становятся пузырными; геометрия старта — [ChatMotion].
 */
@Composable
fun StartActionWidget(
    modifier: Modifier = Modifier,
    sendMessage: (Msg) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(ChatMotion.START_BUTTON_WIDTH_FRACTION)
                .height(ChatMotion.START_BUTTON_HEIGHT),
            shape = RoundedCornerShape(ChatMotion.START_BUTTON_CORNER),
            color = MaterialTheme.colorScheme.primary,
            onClick = { sendMessage(Msg.Start) },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(id = R.string.chat_quiz_start_button_title),
                    style = LexemeStyle.BodyM.copy(
                        color = MaterialTheme.colorScheme.onPrimary
                    ),
                )
            }
        }
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(modifier = Modifier.fillMaxWidth().background(color = Color.Gray)) {
            StartActionWidget {}
        }
    }
}
