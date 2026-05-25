package me.apomazkin.wordcard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.di.viewModelFactory
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.SystemBarsWidget
import me.apomazkin.ui.preview.PreviewScreen
import me.apomazkin.wordcard.mate.LexemeState
import me.apomazkin.wordcard.mate.Msg
import me.apomazkin.wordcard.mate.TextValueState
import me.apomazkin.wordcard.mate.WordCardState
import me.apomazkin.wordcard.mate.WordState
import me.apomazkin.wordcard.widget.AddLexemeWidget
import me.apomazkin.wordcard.widget.ConfirmDeleteLexemeWidget
import me.apomazkin.wordcard.widget.ConfirmDeleteWordWidget
import me.apomazkin.wordcard.widget.TopBarWidget
import me.apomazkin.wordcard.widget.WordFieldWidget
import me.apomazkin.wordcard.widget.internal.UiHostImpl
import me.apomazkin.core_resources.R
import me.apomazkin.wordcard.widget.lexeme.AddLexemeMeaningRow
import me.apomazkin.wordcard.widget.lexeme.DeleteLexemeButton
import me.apomazkin.wordcard.widget.lexeme.LexemeCard
import me.apomazkin.wordcard.widget.lexeme.LexemeMeaningField
import java.util.Date

@Composable
fun WordCardScreen(
    wordId: Long,
    factory: WordCardViewModel.Factory,
    navigator: WordCardNavigator,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val uiHost = remember(snackbarHostState, context) {
        UiHostImpl(snackbarHostState = snackbarHostState, context = context)
    }
    val viewModel: WordCardViewModel = viewModel(
        key = "wordCard_$wordId",
        factory = viewModelFactory { factory.create(wordId, navigator, uiHost) },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    WordCardScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        sendMessage = { viewModel.accept(it) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WordCardScreen(
    state: WordCardState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    sendMessage: (Msg) -> Unit,
) {
    SystemBarsWidget(
        color = whiteColor,
    )

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopBarWidget(
                topBarState = state.topBarState,
                onBackPress = { sendMessage(Msg.NavigateBack) },
                onOpenMenu = { sendMessage(Msg.OpenTopBarMenu) },
                onCloseMenu = { sendMessage(Msg.CloseTopBarMenu) },
                onDeleteWord = {
                    sendMessage(Msg.CloseTopBarMenu)
                    sendMessage(Msg.OpenDeleteWordDialog)
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (state.isLoaded) {
                AddLexemeWidget(
                    enabled = state.canAddLexeme,
                    onAddLexeme = { sendMessage(Msg.CreateLexeme) },
                )
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(left = 0.dp, top = 0.dp, right = 0.dp, bottom = 0.dp),
    ) { paddingValue ->
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(paddingValue)
                .consumeWindowInsets(paddingValue)
                .imePadding()
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { focusManager.clearFocus() },
                )
        ) {
            if (state.wordState is WordState.Loaded) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.tertiary)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    WordFieldWidget(
                        loaded = state.wordState,
                        enabled = !state.isPendingDbOp,
                        onValueChange = { sendMessage(Msg.UpdateWordInput(it)) },
                        onOpenEditMode = { sendMessage(Msg.EnterWordEditMode) },
                        onCommit = { sendMessage(Msg.CommitWordChanges) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.lexemeList.forEach { lexemeState ->
                            key(lexemeState.id) {
                                LexemeCard {
                                    lexemeState.translation?.let { translation ->
                                        LexemeMeaningField(
                                            labelRes = R.string.word_card_bottom_translation,
                                            state = translation,
                                            enabled = !state.isPendingDbOp,
                                            onValueChange = { sendMessage(Msg.UpdateTranslationInput(lexemeState.id, it)) },
                                            onOpenEditMode = { sendMessage(Msg.EnterTranslationEditMode(lexemeState.id)) },
                                            onCommitEdit = { sendMessage(Msg.CommitTranslationEdit(lexemeState.id)) },
                                            onRemove = { sendMessage(Msg.RemoveTranslation(lexemeState.id)) },
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    lexemeState.definition?.let { definition ->
                                        LexemeMeaningField(
                                            labelRes = R.string.word_card_bottom_definition,
                                            state = definition,
                                            enabled = !state.isPendingDbOp,
                                            onValueChange = { sendMessage(Msg.UpdateDefinitionInput(lexemeState.id, it)) },
                                            onOpenEditMode = { sendMessage(Msg.EnterDefinitionEditMode(lexemeState.id)) },
                                            onCommitEdit = { sendMessage(Msg.CommitDefinitionEdit(lexemeState.id)) },
                                            onRemove = { sendMessage(Msg.RemoveDefinition(lexemeState.id)) },
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }
                                    if (lexemeState.canAddTranslation || lexemeState.canAddDefinition) {
                                        AddLexemeMeaningRow(
                                            canAddTranslation = lexemeState.canAddTranslation,
                                            canAddDefinition = lexemeState.canAddDefinition,
                                            enabled = !state.isPendingDbOp,
                                            onCreateTranslation = { sendMessage(Msg.CreateTranslation(lexemeState.id)) },
                                            onCreateDefinition = { sendMessage(Msg.CreateDefinition(lexemeState.id)) },
                                        )
                                    }
                                    DeleteLexemeButton(
                                        enabled = !state.isPendingDbOp,
                                        onClick = { sendMessage(Msg.OpenDeleteLexemeDialog(lexemeState.id)) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.wordState is WordState.Loaded && state.wordState.showWarningDialog) {
            ConfirmDeleteWordWidget(
                onConfirm = { sendMessage(Msg.RemoveWord(state.wordState.id)) },
                onDismiss = { sendMessage(Msg.CloseDeleteWordDialog) },
            )
        }

        state.lexemeIdPendingDelete?.let { pendingLexemeId ->
            ConfirmDeleteLexemeWidget(
                onConfirm = { sendMessage(Msg.RemoveLexeme(pendingLexemeId)) },
                onDismiss = { sendMessage(Msg.CloseDeleteLexemeDialog) },
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
                wordState = WordState.Loaded(
                    id = 1L,
                    added = Date(),
                    value = "Word",
                ),
            ),
            sendMessage = {},
        )
    }
}

@PreviewScreen
@Composable
private fun PreviewWithLexeme() {
    AppTheme {
        WordCardScreen(
            state = WordCardState(
                wordState = WordState.Loaded(
                    id = 1L,
                    added = Date(),
                    value = "Word",
                ),
                lexemeList = listOf(
                    LexemeState(
                        id = 1L,
                        translation = TextValueState(
                            origin = "Translation",
                        ),
                        definition = TextValueState(
                            origin = "Definition",
                        ),
                    ),
                ),
            ),
            sendMessage = {},
        )
    }
}
