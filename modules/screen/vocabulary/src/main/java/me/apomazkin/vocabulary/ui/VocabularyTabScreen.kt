@file:OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLifecycleComposeApi::class,
)

package me.apomazkin.vocabulary.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.gradientPrimary
import me.apomazkin.ui.StatusBarColorWidget
import me.apomazkin.ui.btn.SecondaryFabWidget
import me.apomazkin.ui.lifecycle.LifecycleResume
import me.apomazkin.ui.preview.PreviewWidgetEn
import me.apomazkin.vocabulary.R
import me.apomazkin.vocabulary.deps.VocabularyUseCase
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.logic.UiMsg
import me.apomazkin.vocabulary.logic.VocabularyTabState
import me.apomazkin.vocabulary.logic.isEmpty
import me.apomazkin.vocabulary.tools.DataHelper
import me.apomazkin.vocabulary.ui.widget.AddWordWidget
import me.apomazkin.vocabulary.ui.widget.EmptyWidget
import me.apomazkin.vocabulary.ui.widget.WordListWidget
import me.apomazkin.vocabulary.ui.widget.detailDialog.WordDetailDialogWidget
import me.apomazkin.vocabulary.ui.widget.topBar.TopBarWidget

@Composable
fun VocabularyTabScreen(
    vocabularyUseCase: VocabularyUseCase,
    viewModel: VocabularyTabViewModel = viewModel(
        factory = VocabularyTabViewModel.Factory(vocabularyUseCase)
    ),
    onAddLang: () -> Unit,
    onOpenWordCard: (wordId: Long) -> Unit,
) {
    val state: VocabularyTabState by viewModel.state.collectAsStateWithLifecycle()
    VocabularyTabScreen(
        state = state,
        onAddLang = onAddLang,
        onOpenWordCard = onOpenWordCard,
    ) { viewModel.accept(it) }
}


@Composable
internal fun VocabularyTabScreen(
    state: VocabularyTabState,
    onAddLang: () -> Unit,
    onOpenWordCard: (wordId: Long) -> Unit,
    sendMessage: (Msg) -> Unit,
) {

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarState.show) {
        if (state.snackbarState.show) {
            snackbarHostState.showSnackbar(state.snackbarState.title).also {
                sendMessage(UiMsg.Snackbar(message = EMPTY_STRING, show = false))
            }
        }
    }

    LifecycleResume {
        sendMessage.invoke(Msg.TermDataLoad)
    }

    StatusBarColorWidget(
        statusBarDarkIcon = false,
        navigationBarDarkIcon = true,
        navigationBarContrastEnforced = true,
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = gradientPrimary)
            .statusBarsPadding(),
        topBar = {
            TopBarWidget(
                state = state.topBarActionState,
                sendMessage = sendMessage,
                onAddLang = onAddLang,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(left = 0.dp, top = 0.dp, right = 0.dp, bottom = 0.dp),
        floatingActionButton = {
            AnimatedVisibility(
                visible = !state.addWordDialogState.isAddWordWidgetOpen,
                modifier = Modifier,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                SecondaryFabWidget(
                    iconRes = R.drawable.ic_add,
                    enabled = !state.addWordDialogState.isAddWordWidgetOpen
                ) { sendMessage(Msg.AddWordWidget(show = true)) }
            }
        }
    ) { paddingValue ->
        Box(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 32.dp))
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center),
                        color = Color.Red
                    )
                }
                state.isEmpty() -> {
                    EmptyWidget()
                }
                !state.isEmpty() -> {
                    WordListWidget(
                        modifier = Modifier
                            .padding(top = 20.dp),
                        termList = state.termList,
                        onOpenWordCard = onOpenWordCard,
                        sendMessage = sendMessage,
                    )
                }
            }
            if (state.wordDetailDialogState.isOpen) {
                WordDetailDialogWidget(
                    state = state.wordDetailDialogState,
                    sendMsg = sendMessage,
                )
            }
        }
    }
    AddWordWidget(
        state = state.addWordDialogState,
        wordValue = state.addWordDialogState.addWordValue,
        checkValue = { state.addWordDialogState.isAddDetailEnable },
        sendMessage = sendMessage,
    )
}

@PreviewWidgetEn
@Composable
private fun PreviewLoading() {
    AppTheme {
        VocabularyTabScreen(
            state = DataHelper.State.loading,
            onAddLang = {},
            onOpenWordCard = {},
        ) {}
    }
}

@PreviewWidgetEn
@Composable
private fun PreviewEmpty() {
    AppTheme {
        VocabularyTabScreen(
            state = DataHelper.State.empty,
            onAddLang = {},
            onOpenWordCard = {},
        ) {}
    }
}

@PreviewWidgetEn
@Composable
private fun PreviewLoaded() {
    AppTheme {
        VocabularyTabScreen(
            state = DataHelper.State.loaded,
            onAddLang = {},
            onOpenWordCard = {},
        ) {}
    }
}