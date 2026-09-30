package me.apomazkin.quiz.chat.widget.message

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import me.apomazkin.quiz.chat.logic.ChatMessage
import me.apomazkin.quiz.chat.widget.ChatMotion
import me.apomazkin.quiz.chat.widget.FlightState
import me.apomazkin.quiz.chat.widget.button.ActionChipsRow

/**
 * Пузырь набранного ответа на время полёта копии текста из поля
 * ([FlightState]): сам пузырь скрыт и отдаёт слою полёта свою позицию
 * (цель), а на его месте стоит призрак ряда чипов — их элемент исчез тем
 * же апдейтом, что добавил ответ, — и гаснет в окне смены «чипы →
 * плейсхолдер поля». Прогресс полёта читается в слое рисования.
 *
 * Вход защёлкнут на ключ, поэтому после полёта элемент остаётся здесь и
 * ведёт себя как обычный пузырь: всё, что относится к полёту, — только
 * пока летит именно к нему (`flight.order == order`); иначе прошлый
 * ответ перебивал бы цель следующего своей позицией.
 */
@Composable
internal fun FlyingUserMessage(
    modifier: Modifier,
    message: ChatMessage.MessageValue,
    order: Int,
    flight: FlightState,
) {
    Box(modifier = modifier) {
        UserMessageWidget(
            message = message,
            surfaceModifier = Modifier
                .onGloballyPositioned {
                    if (flight.order == order) flight.target = it.positionInRoot()
                }
                .graphicsLayer { alpha = if (flight.order == order) 0f else 1f },
        )
        if (flight.order == order) {
            ActionChipsRow(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer {
                        alpha = 1f - ChatMotion.window(
                            flight.progress, ChatMotion.SWAP_START, ChatMotion.SWAP_END,
                        )
                    },
                enabled = false,
            )
        }
    }
}
