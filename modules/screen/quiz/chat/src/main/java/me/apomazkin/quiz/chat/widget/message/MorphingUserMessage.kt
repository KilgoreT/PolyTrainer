package me.apomazkin.quiz.chat.widget.message

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.logic.ChatMessage
import me.apomazkin.quiz.chat.logic.ChatTiming
import me.apomazkin.quiz.chat.logic.UserMessageOrigin
import me.apomazkin.quiz.chat.widget.ChatMotion
import me.apomazkin.quiz.chat.widget.button.ACTION_CHIP_SPACING
import me.apomazkin.quiz.chat.widget.button.base.ChatButtonWidget

/**
 * Идёт превращение кнопки в пузырь: высота элемента-пузыря меняется
 * покадрово, и placement соседей на это время выключен — иначе они
 * догоняли бы раскладку с отставанием. Вставок в это окно нет (пауза
 * бота длиннее анимации).
 */
internal class MorphState {
    var active by mutableStateOf(false)
}

/**
 * Пузырь юзера, рождённый системной кнопкой: превращается из неё на месте
 * — перекраска, для «Начать» и геометрия; рядом тает призрак соседнего
 * чипа. Из «Показать ответ» пузырь уезжает вправо на место «Пропустить»,
 * а призрак «Пропустить» едет с ним и тает за краем; из «Пропустить»
 * пузырь синеет на месте, призрак «Показать ответ» гаснет слева в первой
 * фазе. Призрак одной высоты с пузырём (чип = пузырь по геометрии),
 * размер элемента не меняет.
 *
 * Прогресс — линейное время под ключом элемента, фазы — окна
 * [ChatMotion]. Читается в композиции осознанно: геометрия «Начать»
 * меняет раскладку по кадрам.
 */
@Composable
internal fun MorphingUserMessage(
    modifier: Modifier,
    itemKey: Any,
    message: ChatMessage.MessageValue,
    origin: UserMessageOrigin,
    morphState: MorphState,
) {
    val t = actionMorph(key = itemKey, morphState = morphState)
    val geometryMorph = ChatMotion.window(t, 0f, 1f)
    val colorMorph = ChatMotion.window(t, ChatMotion.RECOLOR_START, 1f)
    val ghostFade = ChatMotion.window(t, 0f, ChatMotion.GHOST_FADE_END)
    val fromShowAnswer = origin == UserMessageOrigin.SHOW_ANSWER_CHIP
    val fromSkip = origin == UserMessageOrigin.SKIP_CHIP
    // Ширина «Пропустить» + зазор — дистанция сдвига между чипами. Меряется
    // здесь же, с призрака «Пропустить» или с самого пузыря «Пропустить»
    // (та же раскладка кадра, к рисованию измерено), и читается в слое.
    val skipWidthPx = remember(itemKey) { IntArray(1) }
    val chipSpacingPx = with(LocalDensity.current) { ACTION_CHIP_SPACING.toPx() }

    Box(modifier = modifier) {
        UserMessageWidget(
            message = message,
            colorMorph = colorMorph,
            geometryMorph = geometryMorph,
            fromStart = origin == UserMessageOrigin.START_BUTTON,
            shiftFromLeftPx = { if (fromShowAnswer) skipWidthPx[0] + chipSpacingPx else 0f },
            surfaceModifier = if (fromSkip) {
                Modifier.onSizeChanged { skipWidthPx[0] = it.width }
            } else {
                Modifier
            },
        )
        if ((fromShowAnswer || fromSkip) && t < 1f) {
            ChatButtonWidget(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .onSizeChanged { if (fromShowAnswer) skipWidthPx[0] = it.width }
                    .graphicsLayer {
                        val shift = skipWidthPx[0] + chipSpacingPx
                        translationX = if (fromShowAnswer) shift * geometryMorph else -shift
                        alpha = if (fromShowAnswer) 1f - geometryMorph else 1f - ghostFade
                    },
                title = if (fromShowAnswer) {
                    R.string.chat_quiz_msg_user_skip
                } else {
                    R.string.chat_quiz_msg_user_show_answer
                },
                enabled = false,
            ) {}
        }
    }
}

/**
 * Доля превращения «системная кнопка → пузырь юзера»: линейное время
 * 0 → 1, кривые — у каждой фазы своя. Состояние под ключом элемента:
 * LazyList переиспользует композицию ушедшего элемента для нового.
 */
@Composable
private fun actionMorph(key: Any, morphState: MorphState): Float {
    val progress = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        morphState.active = true
        try {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(ChatTiming.MOTION_DURATION_MS, easing = LinearEasing),
            )
        } finally {
            morphState.active = false
        }
    }
    return progress.value
}
