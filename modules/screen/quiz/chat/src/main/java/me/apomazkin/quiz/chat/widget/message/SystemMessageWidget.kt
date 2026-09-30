package me.apomazkin.quiz.chat.widget.message

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.logic.ChatMessage
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.widget.ChatMotion
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.ui.preview.BoolParam
import me.apomazkin.ui.preview.PreviewWidget

/**
 * [avatarDescends] — сообщение продолжает цепочку системных: аватар
 * стартует на месте аватара предыдущего пузыря (на высоту своего ряда +
 * зазор выше) и той же кривой, что движение ленты, опускается на своё
 * место; у предыдущего аватар в этот же кадр заменён пустым местом —
 * одна иконка непрерывно съезжает вниз. [avatarDescentExtraPx] — добавка
 * к дистанции спуска, равная добавке въезда ряда (старт под полем
 * ввода): въезд и спуск компенсируют друг друга, иконка на экране стоит
 * на месте. Читается в фазе рисования.
 */
@Composable
fun SystemMessageWidget(
    modifier: Modifier = Modifier,
    message: ChatMessage,
    showAvatar: Boolean,
    isInChain: Boolean,
    avatarDescends: Boolean = false,
    avatarDescentExtraPx: () -> Float = { 0f },
    motionDurationMs: Int = ChatMotion.DURATION_MS,
    showButtons: Boolean,
    sendMessage: (Msg) -> Unit,
) {
    // Состояние под ключом сообщения: LazyList переиспользует композицию
    // ушедшего элемента для нового.
    // Линейное время, кривая применяется в слое — как у въезда ряда:
    // спуск компенсирует движение ряда покадрово.
    val descent = remember(message.order) { Animatable(if (avatarDescends) 0f else 1f) }
    LaunchedEffect(message.order) {
        if (avatarDescends) {
            descent.animateTo(
                targetValue = 1f,
                animationSpec = tween(motionDurationMs, easing = LinearEasing),
            )
        }
    }
    // Высота ряда известна из раскладки этого же кадра — до рисования.
    val rowHeightPx = remember { IntArray(1) }
    val spacingPx = with(LocalDensity.current) { ChatMotion.ITEM_SPACING.toPx() }
    Box(
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.onSizeChanged { rowHeightPx[0] = it.height },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            if (showAvatar) {
                AvatarWidget(
                    modifier = Modifier.graphicsLayer {
                        // Та же кривая и та же дистанция, что у въезда ряда
                        // (добавка + высота ряда + зазор), со знаком минус:
                        // иконка на экране стоит на месте.
                        val remaining = 1f - ChatMotion.EASING.transform(descent.value)
                        translationY = -(avatarDescentExtraPx() + rowHeightPx[0] + spacingPx) * remaining
                    },
                    avatarRes = R.drawable.ic_logo_avatar,
                )
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

