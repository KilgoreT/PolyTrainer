package me.apomazkin.wordcard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.SystemBarsWidget
import me.apomazkin.ui.preview.PreviewScreen
import me.apomazkin.wordcard.deps.WordCardUseCase
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.UiMsg
import me.apomazkin.wordcard.mate.WordCardState
import me.apomazkin.wordcard.mate.WordState
import me.apomazkin.wordcard.widget.AddLexemeWidget
import me.apomazkin.wordcard.widget.LexemeWidget
import me.apomazkin.wordcard.widget.SnackbarLaunchEffect
import me.apomazkin.wordcard.widget.TopBarWidget
import me.apomazkin.wordcard.widget.WordFieldWidget
import java.util.Date

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

    SystemBarsWidget(
        color = whiteColor,
    )

    Scaffold(
        topBar = { TopBarWidget(onBackPress = onBackPress) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(left = 0.dp, top = 0.dp, right = 0.dp, bottom = 0.dp),
    ) { paddingValue ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValue)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.tertiary)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                WordFieldWidget(wordState = state.wordState)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1F),
                ) {
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
                }
            }
            AddLexemeWidget(
                enabled = state.canAddLexeme,
                onAddLexeme = { sendMessage(Msg.AddLexeme) },
                modifier = Modifier
                    .align(BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
            )
        }
//        EditWordDialogWidget(
//            state = state.wordState,
//            sendMessage = sendMessage,
//        )
//        DeleteWordWarningDialog(
//            state = state.wordState,
//            sendMessage = sendMessage,
//        )
    }
}

@PreviewScreen
@Composable
private fun Preview() {
    AppTheme {
        WordCardScreen(
            state = WordCardState(
                wordState = WordState(
                    value = "Word",
                    added = Date(),
                )
            ),
            onBackPress = {},
            sendMessage = {}
        )
    }
}