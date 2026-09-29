package me.apomazkin.quiz.chat.widget.message

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import me.apomazkin.quiz.chat.logic.ChatMessage
import me.apomazkin.quiz.chat.widget.ChatMotion
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.chatMessageBtnBorder
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.preview.PreviewWidget
import kotlin.math.roundToInt

private val BUBBLE_CORNER = 20.dp
private val BUBBLE_TAIL_CORNER = 8.dp

/**
 * Превращение системной кнопки в пузырь двумя долями (0 — вид кнопки,
 * 1 — пузырь): [colorMorph] — цвет и рамка, [geometryMorph] — высота,
 * ширина, положение, углы. Для чипа под вопросом ([fromStart] = false)
 * форма и отступы совпадают с пузырём, превращение — перекраска на месте
 * (белая с рамкой → primary). Для кнопки «Начать» ([fromStart] = true)
 * цвет уже primary, меняется геометрия: широкая по центру → по тексту
 * справа. [shiftFromLeftPx] — старт левее своего места (чип «Показать
 * ответ» стоит левее «Пропустить»): пузырь уезжает вправо на своё место.
 */
@Composable
fun UserMessageWidget(
    modifier: Modifier = Modifier,
    message: ChatMessage.MessageValue,
    colorMorph: Float = 1f,
    geometryMorph: Float = 1f,
    fromStart: Boolean = false,
    shiftFromLeftPx: Float = 0f,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val startColor = if (fromStart) primary else whiteColor
    val startTextColor = if (fromStart) onPrimary else primary
    val borderAlpha = if (fromStart) 0f else 1f - colorMorph
    val corner = if (fromStart) lerp(ChatMotion.START_BUTTON_CORNER, BUBBLE_CORNER, geometryMorph) else BUBBLE_CORNER
    val tailCorner = if (fromStart) lerp(ChatMotion.START_BUTTON_CORNER, BUBBLE_TAIL_CORNER, geometryMorph) else BUBBLE_TAIL_CORNER
    val wideFraction = if (fromStart) ChatMotion.START_BUTTON_WIDTH_FRACTION * (1f - geometryMorph) else 0f
    val minHeight: Dp = if (fromStart) lerp(ChatMotion.START_BUTTON_HEIGHT, 0.dp, geometryMorph) else 0.dp
    val alignment = if (fromStart) {
        BiasAlignment(horizontalBias = lerp(0f, 1f, geometryMorph), verticalBias = 0f)
    } else {
        Alignment.CenterEnd
    }
    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = alignment,
    ) {
        Surface(
            // Минимальные ширина и высота — от кнопки «Начать», тают с morph:
            // в нуле пузырь ровно повторяет кнопку, в единице — по тексту.
            modifier = Modifier
                .graphicsLayer { translationX = -shiftFromLeftPx * (1f - geometryMorph) }
                .layout { measurable, constraints ->
                val minWidth = (constraints.maxWidth * wideFraction)
                    .roundToInt()
                    .coerceAtMost(constraints.maxWidth)
                val minHeightPx = minHeight.roundToPx().coerceAtMost(constraints.maxHeight)
                val placeable = measurable.measure(
                    constraints.copy(minWidth = minWidth, minHeight = minHeightPx)
                )
                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
            },
            shape = RoundedCornerShape(
                topStart = corner,
                topEnd = corner,
                bottomStart = corner,
                bottomEnd = tailCorner,
            ),
            color = lerp(startColor, primary, colorMorph),
            border = BorderStroke(
                width = 1.dp,
                color = chatMessageBtnBorder.copy(alpha = borderAlpha),
            ),
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = message.asText(),
                    style = LexemeStyle.BodyM.copy(
                        color = lerp(startTextColor, onPrimary, colorMorph)
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Gray)
        ) {
            UserMessageWidget(
                message = ChatMessage.MessageValue.Plain("Юзер мессадж"),
            )
        }
    }
}
