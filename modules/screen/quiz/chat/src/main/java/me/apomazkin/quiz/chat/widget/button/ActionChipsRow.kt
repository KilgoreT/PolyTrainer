package me.apomazkin.quiz.chat.widget.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.widget.button.base.ChatButtonWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.preview.PreviewWidget

/** Зазор между чипами; вместе с шириной «Пропустить» — дистанция сдвига пузыря «Показать ответ». */
val ACTION_CHIP_SPACING = 8.dp

/**
 * Ряд чипов «Показать ответ» / «Пропустить» — один и для живого ряда в
 * ленте ([UserActionsWidget]), и для призрака внутри пузыря ответа на
 * время полёта (тогда [enabled] = false, обработчики не нужны).
 */
@Composable
internal fun ActionChipsRow(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onShowAnswer: () -> Unit = {},
    onSkip: () -> Unit = {},
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ACTION_CHIP_SPACING, Alignment.End),
        verticalAlignment = Alignment.Bottom,
    ) {
        ChatButtonWidget(
            title = R.string.chat_quiz_msg_user_show_answer,
            enabled = enabled,
            onClick = onShowAnswer,
        )
        ChatButtonWidget(
            title = R.string.chat_quiz_msg_user_skip,
            enabled = enabled,
            onClick = onSkip,
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        Box(modifier = Modifier.fillMaxWidth().background(color = Color.Gray)) {
            ActionChipsRow(modifier = Modifier.fillMaxWidth())
        }
    }
}
