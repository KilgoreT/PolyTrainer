package me.apomazkin.quiz.chat.widget.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

/**
 * Действия по текущему вопросу — элемент ленты чата, прокручивается
 * вместе с сообщениями. Показ/скрытие — по `ChatState.showUserActions`.
 */
/** Зазор между чипами; вместе с шириной «Пропустить» — дистанция сдвига пузыря «Показать ответ». */
val ACTION_CHIP_SPACING = 8.dp

@Composable
fun UserActionsWidget(
    modifier: Modifier = Modifier,
    onSkipChipMeasured: (widthPx: Int) -> Unit = {},
    sendMessage: (Msg) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(ACTION_CHIP_SPACING, Alignment.End),
        verticalAlignment = Alignment.Bottom,
    ) {
        ShowAnswerButtonWidget { sendMessage(it) }
        SkipButtonWidget(
            modifier = Modifier.onSizeChanged { onSkipChipMeasured(it.width) },
        ) { sendMessage(it) }
    }
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
