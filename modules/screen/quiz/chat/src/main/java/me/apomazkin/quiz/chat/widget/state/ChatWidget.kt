package me.apomazkin.quiz.chat.widget.state

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.logic.ChatScreenState
import me.apomazkin.quiz.chat.logic.ChatState
import me.apomazkin.quiz.chat.logic.MessageContent
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.logic.systemMessage
import me.apomazkin.quiz.chat.widget.ChatMessageWidget
import me.apomazkin.quiz.chat.widget.ChatMotion
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
    
    Column(
        modifier = modifier
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
                    .background(color = whiteColor),
                placeHolder = R.string.chat_quiz_placeholder_text,
                autoCorrect = false,
                isInputEnabled = state.isUserInputEnable,
                value = state.inputState,
                isSendEnabled = state.inputState.isNotEmpty() && state.inputState.isNotBlank(),
                onValueChange = { sendMessage(Msg.UserTextChange(it)) },
                onSendAction = { sendMessage(Msg.UserAttempt(state.inputState)) }
            )
        }
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