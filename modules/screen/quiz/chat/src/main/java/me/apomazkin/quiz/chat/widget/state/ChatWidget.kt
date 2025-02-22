package me.apomazkin.quiz.chat.widget.state

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.apomazkin.quiz.chat.R
import me.apomazkin.quiz.chat.logic.ChatScreenState
import me.apomazkin.quiz.chat.logic.ChatState
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.logic.prepareToStart
import me.apomazkin.quiz.chat.widget.ChatMessageWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.btn.PrimaryFullButtonWidget
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
        ChatMessageWidget(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F),
            state = state.messagesState,
        )
        if (state.readyToStart) {
            PrimaryTextFieldWidget(
                modifier = Modifier
                    .background(color = whiteColor),
                placeHolder = R.string.chat_quiz_placeholder_text,
                value = state.inputState,
                isSendEnabled = state.inputState.isNotEmpty() && state.inputState.isNotBlank(),
                onValueChange = { sendMessage(Msg.UserTextChange(it)) },
                onSendAction = { sendMessage(Msg.UserTextEnter) }
            )
        } else {
            PrimaryFullButtonWidget(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                titleRes = R.string.chat_quiz_start_button_title,
                enabled = true,
                onClick = { sendMessage(Msg.Start) }
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
                .prepareToStart(
                    message = "Hi! Are you ready to start? \uD83D\uDCAA"
                ).chat
        ) {}
    }
}