package me.apomazkin.wordcard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.TextValueState
import me.apomazkin.wordcard.mate.UiMsg
import me.apomazkin.wordcard.mate.WordCardState
import me.apomazkin.wordcard.mate.WordState
import me.apomazkin.wordcard.widget.AddLexemeWidget
import me.apomazkin.wordcard.widget.ConfirmDeleteWordWidget
import me.apomazkin.wordcard.widget.SnackbarLaunchEffect
import me.apomazkin.wordcard.widget.TopBarWidget
import me.apomazkin.wordcard.widget.WordFieldWidget
import me.apomazkin.wordcard.widget.addlexeme.AddLexemeBottomWidget
import me.apomazkin.wordcard.widget.lexeme.LexemeItemWidget
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

@OptIn(ExperimentalMaterial3Api::class)
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
        topBar = {
            TopBarWidget(
                topBarState = state.topBarState,
                onBackPress = onBackPress,
                sendMessage = sendMessage,
            )
        },
        floatingActionButton = {
            AddLexemeWidget(
                enabled = true,
                onAddLexeme = { sendMessage(Msg.ShowAddLexemeBottom) },
                modifier = Modifier
                    .navigationBarsPadding(),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(left = 0.dp, top = 0.dp, right = 0.dp, bottom = 0.dp),
    ) { paddingValue ->

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValue)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.tertiary)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                WordFieldWidget(
                    wordState = state.wordState,
                    sendMessage = sendMessage,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.lexemeList.forEachIndexed { index, lexemeState ->
                        key(lexemeState.id) {
                            LexemeItemWidget(
                                order = index + 1,
                                state = lexemeState,
                                sendMessage = sendMessage,
                            )
                        }
                    }
                }
            }
        }
        if (state.wordState.showWarningDialog) {
            ConfirmDeleteWordWidget(
                state = state.wordState,
                sendMessage = sendMessage
            )
        }
        if (state.addLexemeBottomState.show) {
            AddLexemeBottomWidget(
                state = state.addLexemeBottomState,
                onDismiss = { sendMessage(Msg.HideAddLexemeBottom) },
                sendMessage = sendMessage,
            )
        }
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

@PreviewScreen
@Composable
private fun PreviewWithLexeme() {
    AppTheme {
        WordCardScreen(
            state = WordCardState(
                wordState = WordState(
                    value = "Word",
                    added = Date(),
                ),
                lexemeList = listOf(
                    LexemeState(
                        id = 1,
                        translation = TextValueState(
                            origin = "Translation",
                        ),
                        definition = TextValueState(
                            origin = "Definition",
                        )
                    )
                )
            ),
            onBackPress = {},
            sendMessage = {}
        )
    }
}