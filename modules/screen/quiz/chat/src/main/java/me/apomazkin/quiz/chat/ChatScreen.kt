package me.apomazkin.quiz.chat

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import me.apomazkin.quiz.chat.logic.ChatScreenState
import me.apomazkin.quiz.chat.logic.Msg
import me.apomazkin.quiz.chat.widget.AppBarWidget
import me.apomazkin.quiz.chat.widget.state.ChatWidget
import me.apomazkin.quiz.chat.widget.state.InitQuizWidget
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.ui.preview.PreviewScreen
import me.apomazkin.ui.resource.ResourceManager

@Composable
fun ChatScreen(
    quizChatUseCase: QuizChatUseCase,
    resourceManager: ResourceManager,
    logger: LexemeLogger,
    viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(
            quizChatUseCase,
            resourceManager,
            logger
        )
    ),
    onBackPress: () -> Unit,
) {
    val state: ChatScreenState by viewModel.state.collectAsStateWithLifecycle()
    ChatScreen(
        state = state,
        onBackPress = onBackPress,
    ) { viewModel.accept(it) }
}

@Composable
internal fun ChatScreen(
    state: ChatScreenState,
    onBackPress: () -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    
    LaunchedEffect(state.exit) {
        if (state.exit) {
            onBackPress()
        }
    }
    
    Scaffold(
        topBar = { AppBarWidget(onBackPress = onBackPress) },
    ) { paddings: PaddingValues ->
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddings)
                .consumeWindowInsets(paddings)
                .imePadding()
                .background(color = MaterialTheme.colorScheme.tertiary)
        ) {
            Image(
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillHeight,
                painter = painterResource(R.drawable.ic_chat_bg),
                contentDescription = stringResource(R.string.chat_quiz_bg_image_content_description)
            )
            when (state.loading) {
                true -> InitQuizWidget()
                false -> ChatWidget(
                    state = state.chat,
                ) { sendMessage(it) }
            }
        }
    }
}

@PreviewScreen
@Composable
private fun Preview() {
    AppTheme {
        ChatScreen(
            state = ChatScreenState(loading = false),
            onBackPress = {},
            sendMessage = {},
        )
    }
}