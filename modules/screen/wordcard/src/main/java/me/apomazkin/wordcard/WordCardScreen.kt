@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLifecycleComposeApi::class,
)

package me.apomazkin.wordcard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.M3Black
import me.apomazkin.theme.M3Neutral
import me.apomazkin.ui.StatusBarColorWidget
import me.apomazkin.ui.preview.PreviewScreenEn
import me.apomazkin.ui.preview.PreviewScreenRu
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.UiMsg
import me.apomazkin.wordcard.mate.WordCardState
import me.apomazkin.wordcard.mate.WordState
import me.apomazkin.wordcard.widget.*

@Composable
fun WordCardScreen(
    wordId: Long,
    wordCardUseCase: WordCardUseCase,
    onBackPress: () -> Unit,
    viewModel: WordCardViewModel = viewModel(
        factory = WordCardViewModel.Factory(
            wordId = wordId,
            wordCardUseCase = wordCardUseCase,
        )
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WordCardScreen(
        state = state,
        onBackPress = onBackPress
    ) { viewModel.accept(it) }
}

@Composable
internal fun WordCardScreen(
    state: WordCardState,
    onBackPress: () -> Unit,
    sendMessage: (Msg) -> Unit,
) {

    val snackbarHostState = remember { SnackbarHostState() }
    SnackbarLaunchEffect(
        snackState = state.snackbarState,
        host = snackbarHostState,
        onResetState = { sendMessage(UiMsg.Snackbar(text = EMPTY_STRING, show = false)) }
    )

    LaunchedEffect(state.closeScreen) {
        if (state.closeScreen) onBackPress.invoke()
    }

    StatusBarColorWidget(
        statusBarColor = M3Black,
        navigationBarColor = M3Neutral,
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        topBar = {
            TopBarWidget(
                onBackPress = onBackPress,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        contentWindowInsets = WindowInsets(
            left = 0.dp,
            top = 0.dp,
            right = 0.dp,
            bottom = 0.dp
        ),
    ) { paddingValue ->
        Column(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            WordFieldWidget(
                wordState = state.wordState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .padding(horizontal = 16.dp),
                onEditClick = { sendMessage(Msg.EditWord) },
                onWordChange = { sendMessage(Msg.ChangeWordValue(it)) },
                onSaveWord = { sendMessage(Msg.SaveWordValue) }
            )
            AddLexemeWidget(
                enabled = state.canAddLexeme,
                onAddLexeme = { sendMessage(Msg.AddLexeme) },
                modifier = Modifier
                    .align(End)
                    .padding(horizontal = 16.dp),
            )
            state.lexemeList.forEachIndexed { index, lexemeState ->
                key(lexemeState.id) {
                    LexemeWidget(
                        modifier = Modifier
                            .padding(horizontal = 16.dp),
                        state = lexemeState,
                        sendMessage = sendMessage
                    )
                    if (index < state.lexemeList.size - 1) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
            DeleteWordWidget { sendMessage(Msg.DeleteWord(state.wordState.id)) }
        }
    }
}

@PreviewScreenEn
@PreviewScreenRu
@Composable
private fun Preview() {
    AppTheme {
        WordCardScreen(
            state = WordCardState(
                wordState = WordState(value = "Word")
            ),
            onBackPress = {},
            sendMessage = {}
        )
    }
}