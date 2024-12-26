package me.apomazkin.dictionarytab.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.dictionarytab.R
import me.apomazkin.dictionarytab.deps.DictionaryTabUseCase
import me.apomazkin.dictionarytab.logic.DictionaryTabState
import me.apomazkin.dictionarytab.logic.Msg
import me.apomazkin.dictionarytab.logic.UiMsg
import me.apomazkin.dictionarytab.logic.isEmpty
import me.apomazkin.dictionarytab.logic.processor.toMateEvent
import me.apomazkin.dictionarytab.tools.DataHelper
import me.apomazkin.dictionarytab.ui.widget.ConfirmDeleteWordWidget
import me.apomazkin.dictionarytab.ui.widget.EmptyWidget
import me.apomazkin.dictionarytab.ui.widget.WordListWidget
import me.apomazkin.dictionarytab.ui.widget.addWordBottom.AddWordBottomSheetWidget
import me.apomazkin.dictionarytab.ui.widget.topBar.ActionTopBarWidget
import me.apomazkin.dictionarytab.ui.widget.topBar.TopBarWidget
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.SystemBarsWidget
import me.apomazkin.ui.btn.PrimaryFabWidget
import me.apomazkin.ui.lifecycle.LifecycleEventHandler
import me.apomazkin.ui.lifecycle.LifecycleResume
import me.apomazkin.ui.logger.LexemeLogger
import me.apomazkin.ui.preview.PreviewWidget

@Composable
fun DictionaryTabScreen(
    dictionaryTabUseCase: DictionaryTabUseCase,
    logger: LexemeLogger,
    viewModel: DictionaryTabViewModel = viewModel(
        factory = DictionaryTabViewModel.Factory(dictionaryTabUseCase, logger)
    ),
    openAddDict: () -> Unit,
    openWordCard: (wordId: Long) -> Unit,
) {
    LifecycleEventHandler(action = { viewModel.accept(UiMsg.LifeCycleEvent(it.toMateEvent())) })
    val state: DictionaryTabState by viewModel.state.collectAsStateWithLifecycle()
    DictionaryTabScreen(
        state = state,
        openAddDict = openAddDict,
        openWordCard = openWordCard,
    ) { viewModel.accept(it) }
}


@Composable
internal fun DictionaryTabScreen(
    state: DictionaryTabState,
    openAddDict: () -> Unit,
    openWordCard: (wordId: Long) -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.snackbarState.show) {
        if (state.snackbarState.show) {
            snackbarHostState.showSnackbar(state.snackbarState.title).also {
                sendMessage(
                    UiMsg.Snackbar(
                        message = EMPTY_STRING, show = false
                    )
                )
            }
        }
    }
    
    LifecycleResume {
        sendMessage.invoke(Msg.TermDataLoad)
    }
    
    SystemBarsWidget(
        statusBarColor = if (state.topBarState.isActionMode) {
            MaterialTheme.colorScheme.secondary
        } else {
            Color.Transparent
        },
        statusBarDarkIcon = !state.topBarState.isActionMode
    )
    
    BackHandler(enabled = state.topBarState.isActionMode) {
        sendMessage(Msg.ChangeActionMode(false, null))
    }
    
    Scaffold(modifier = Modifier.fillMaxSize(),
        topBar = {
            if (state.topBarState.isActionMode) ActionTopBarWidget(
                state = state.topBarState.actionState,
                sendMessage = sendMessage,
            )
            else TopBarWidget(
                state = state.topBarState.langPickerState,
                sendMessage = sendMessage,
                openAddDict = openAddDict,
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = Color.Transparent,
             contentWindowInsets = WindowInsets(
                 left = 0.dp, top = 0.dp, right = 0.dp, bottom = 0.dp
             ),
        floatingActionButton = {
            AnimatedVisibility(
                visible = !state.addWordDialogState.isOpen,
                modifier = Modifier,
                enter = scaleIn(),
                exit = scaleOut(),
            ) {
                PrimaryFabWidget(
                    iconRes = R.drawable.ic_add,
                    enabled = !state.addWordDialogState.isOpen
                ) { sendMessage(Msg.StartAddWord(show = true)) }
            }
        }) { paddingValue: PaddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                
                state.isEmpty() -> {
                    EmptyWidget()
                }
                
                !state.isEmpty() -> {
                    WordListWidget(
                        modifier = Modifier.padding(top = 20.dp),
                        termList = state.termList,
                        openWordCard = { word ->
                            if (state.topBarState.isActionMode) {
                                sendMessage(
                                    Msg.ChangeActionMode(
                                        isActionMode = true, targetWord = word
                                    )
                                )
                            } else {
                                openWordCard.invoke(word.id)
                            }
                        },
                        sendMessage = sendMessage,
                    )
                }
            }
            if (state.addWordDialogState.isOpen) {
                AddWordBottomSheetWidget(
                    state = state.addWordDialogState,
                    sendMessage = sendMessage,
                )
            }
            if (state.confirmWordDeleteDialogState.isOpen) {
                ConfirmDeleteWordWidget(
                    state = state.confirmWordDeleteDialogState,
                    sendMessage = sendMessage,
                )
            }
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewLoading() {
    AppTheme {
        DictionaryTabScreen(
            state = DataHelper.State.loading,
            openAddDict = {},
            openWordCard = {},
        ) {}
    }
}

@PreviewWidget
@Composable
private fun PreviewEmpty() {
    AppTheme {
        DictionaryTabScreen(
            state = DataHelper.State.empty,
            openAddDict = {},
            openWordCard = {},
        ) {}
    }
}

@PreviewWidget
@Composable
private fun PreviewLoaded() {
    AppTheme {
        DictionaryTabScreen(
            state = DataHelper.State.loaded,
            openAddDict = {},
            openWordCard = {},
        ) {}
    }
}