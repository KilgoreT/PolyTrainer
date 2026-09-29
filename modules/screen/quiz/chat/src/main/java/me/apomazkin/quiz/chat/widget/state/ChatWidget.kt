package me.apomazkin.quiz.chat.widget.state

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.logic.ChatScreenState
import me.apomazkin.quiz.chat.logic.ChatState
import me.apomazkin.quiz.chat.logic.MessageContent
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.logic.nextOrder
import me.apomazkin.quiz.chat.logic.systemMessage
import me.apomazkin.quiz.chat.widget.ChatMessageWidget
import me.apomazkin.quiz.chat.widget.ChatMotion
import me.apomazkin.quiz.chat.widget.FlightLayer
import me.apomazkin.quiz.chat.widget.FlightState
import me.apomazkin.quiz.chat.widget.launch
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.input.PrimaryTextFieldWidget
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun ChatWidget(
    modifier: Modifier = Modifier,
    state: ChatState,
    sendMessage: (Msg) -> Unit,
) {
    
    // Полёт набранного ответа из поля в пузырь: лента режет содержимое по
    // вертикали, а поле — отдельный узел под ней, поэтому копия текста
    // летит в слое поверх обоих (FlightLayer), в координатах корня.
    val flight = remember { FlightState() }
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            // «Начать» — системная кнопка внутри ленты (под приветствием), а не
            // полоса под ней: нажатие превращает её в пузырь юзера на месте.
            ChatMessageWidget(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F),
                state = state.messagesState,
                showUserActions = state.showUserActions,
                showStartAction = !state.readyToStart,
                flight = flight,
                sendMessage = sendMessage,
            )

            // Поле ввода разворачивается снизу той же кривой, что «Начать»
            // превращается в пузырь: лента уезжает вверх одним движением,
            // а не скачком на высоту поля.
            AnimatedVisibility(
                visible = state.readyToStart,
                enter = expandVertically(
                    animationSpec = tween(ChatMotion.DURATION_MS, easing = ChatMotion.EASING),
                ),
                exit = ExitTransition.None,
            ) {
                PrimaryTextFieldWidget(
                    modifier = Modifier
                        .background(color = whiteColor)
                        .onGloballyPositioned { flight.fieldCoordinates = it },
                    placeHolder = R.string.chat_quiz_placeholder_text,
                    // Плейсхолдер гаснет в момент отправки и проявляется в окне
                    // смены «чипы → плейсхолдер» — синхронно с гашением чипов
                    // в ленте, а не скачком под отрывающимися буквами.
                    placeholderModifier = Modifier.graphicsLayer {
                        alpha = if (flight.order == null) {
                            1f
                        } else {
                            ChatMotion.window(flight.progress, ChatMotion.SWAP_START, ChatMotion.SWAP_END)
                        }
                    },
                    autoCorrect = false,
                    isInputEnabled = state.isUserInputEnable,
                    value = state.inputState,
                    isSendEnabled = state.inputState.isNotEmpty() && state.inputState.isNotBlank(),
                    onValueChange = { sendMessage(Msg.UserTextChange(it)) },
                    onSendAction = {
                        // Источник полёта снимается до апдейта: после него
                        // поле уже пустое. Порядок нового сообщения известен
                        // заранее — nextOrder().
                        flight.launch(
                            order = state.nextOrder(),
                            text = state.inputState,
                            measurer = measurer,
                            density = density,
                        )
                        sendMessage(Msg.UserAttempt(state.inputState))
                    }
                )
            }
        }
        FlightLayer(
            modifier = Modifier.matchParentSize(),
            flight = flight,
        )
    }
}

@PreviewWidget
@Composable
private fun Preview() {
    AppTheme {
        ChatWidget(
            state = ChatScreenState()
                .systemMessage(
                    message = MessageContent.create("Hi! Are you ready to start? \uD83D\uDCAA")
                ).chat
        ) {}
    }
}