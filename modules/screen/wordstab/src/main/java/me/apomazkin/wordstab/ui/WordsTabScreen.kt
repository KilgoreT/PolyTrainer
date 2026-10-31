package me.apomazkin.wordstab.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import me.apomazkin.di.viewModelFactory
import me.apomazkin.wordstab.R
import me.apomazkin.wordstab.logic.WordsTabState
import me.apomazkin.wordstab.logic.Msg
import me.apomazkin.wordstab.logic.UiMsg
import me.apomazkin.wordstab.logic.processor.toMateEvent
import me.apomazkin.wordstab.tools.DataHelper
import me.apomazkin.wordstab.ui.photo.rememberPhotoPicker
import me.apomazkin.wordstab.ui.widget.ConfirmDeleteWordWidget
import me.apomazkin.wordstab.ui.widget.EmptyWidget
import me.apomazkin.wordstab.ui.widget.WordListWidget
import me.apomazkin.wordstab.ui.widget.addWordBottom.AddWordPanelWidget
import me.apomazkin.wordstab.ui.widget.scan.ScanDialog
import me.apomazkin.wordstab.ui.widget.topBar.ActionTopBarWidget
import me.apomazkin.mate.EMPTY_STRING
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.lifecycle.LifecycleEventHandler
import me.apomazkin.ui.preview.PreviewWidget

/**
 * IS493 Э1: handle-паттерн (stage1_design_tree D2.6) — публичный API вкладки
 * «Слова» для app-моста. VM создаётся и коллектится ВНУТРИ модуля; наружу —
 * узкие derived-State и composable-куски, из которых мост собирает TabSpec
 * host'а. Scaffold-обвязка (topBar/FAB/snackbar/статусбар) переехала в host
 * (`modules/screen/vocabulary`).
 */
@Composable
fun rememberWordsTabHandle(
    factory: WordsTabViewModel.Factory,
): WordsTabHandle {
    val viewModel: WordsTabViewModel = viewModel(
        factory = viewModelFactory { factory.create() },
    )
    val state = viewModel.state.collectAsStateWithLifecycle()
    return remember(viewModel) {
        WordsTabHandle(viewModel = viewModel, stateProvider = state)
    }
}

@Stable
class WordsTabHandle internal constructor(
    private val viewModel: WordsTabViewModel,
    private val stateProvider: State<WordsTabState>,
) {
    /** ActionMode активен — мост подставляет [ActionTopBar] в topBarOverride. */
    val isActionMode: State<Boolean> =
        derivedStateOf { stateProvider.value.topBarState.isActionMode }

    /** Видимость FAB добавления слова (прячется, пока открыт диалог). */
    val isFabVisible: State<Boolean> =
        derivedStateOf { !stateProvider.value.addWordDialogState.isOpen }

    @DrawableRes
    val fabIconRes: Int = R.drawable.ic_add

    fun onFabClick() {
        viewModel.accept(Msg.OpenAddWordDialog())
    }

    /** Кнопка-камера над «+» — лексемы-кандидаты со снимка (TFP-04). */
    @DrawableRes
    val photoFabIconRes: Int = R.drawable.ic_photo_camera

    @StringRes
    val photoFabDescriptionRes: Int = R.string.photo_words_title

    fun onPhotoFabClick() {
        viewModel.accept(Msg.OpenScanDialog)
    }

    fun onExitSelectionMode() {
        viewModel.accept(Msg.ExitSelectionMode)
    }

    @Composable
    fun ActionTopBar() {
        ActionTopBarWidget(
            state = stateProvider.value.topBarState.actionState,
            sendMessage = viewModel::accept,
        )
    }

    @Composable
    fun Content(snackbarHostState: SnackbarHostState) {
        LifecycleEventHandler(action = {
            viewModel.accept(UiMsg.LifecycleEvent(it.toMateEvent()))
        })
        // Launcher'ы регистрируются всегда, не под `if`: после гибели процесса в
        // камере диалог закрыт, а снимок должен дойти (TFP-22).
        val photoPicker = rememberPhotoPicker(
            onPicked = { uri -> viewModel.accept(Msg.ImagePicked(uri = uri.toString())) },
            logger = viewModel.logger,
        )
        // VM живёт дольше Activity: PhotoPicker привязывается к её держателю
        // запуска заново с каждой композицией.
        DisposableEffect(photoPicker) {
            viewModel.scanLauncher.bind(photoPicker)
            onDispose { viewModel.scanLauncher.unbind() }
        }
        WordsTabContent(
            state = stateProvider.value,
            snackbarHostState = snackbarHostState,
            isCameraAvailable = photoPicker::isCameraAvailable,
        ) { viewModel.accept(it) }
    }
}

/**
 * Контент вкладки «Слова» без Scaffold-обвязки: список/диалоги/BackHandler.
 * Padding и background применяет host; snackbar показывается через host'овый
 * [SnackbarHostState].
 *
 * @param isCameraAvailable есть ли приложение камеры; спрашивается при
 * открытии диалога выбора источника (TFP-06.3).
 */
@Composable
internal fun WordsTabContent(
    state: WordsTabState,
    snackbarHostState: SnackbarHostState,
    isCameraAvailable: () -> Boolean,
    sendMessage: (Msg) -> Unit,
) {
    LaunchedEffect(state.snackbarState.show) {
        if (state.snackbarState.show) {
            snackbarHostState.showSnackbar(state.snackbarState.title).also {
                sendMessage(
                    UiMsg.ShowNotification(
                        message = EMPTY_STRING, show = false
                    )
                )
            }
        }
    }

    BackHandler(enabled = state.topBarState.isActionMode) {
        sendMessage(Msg.ExitSelectionMode)
    }

    // IS496 Р4: скролл живого фона под открытой панелью прячет клавиатуру
    // (панель остаётся — закрытие сбросило бы фильтр и позицию).
    val keyboard = LocalSoftwareKeyboardController.current
    val hideKeyboardOnScroll = remember(keyboard) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (available.y != 0f) keyboard?.hide()
                return Offset.Zero
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(hideKeyboardOnScroll),
    ) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            state.hasNoDictionary -> {
                // IS476: защитная ветка — этот state по дизайну unreachable
                // для пользователя (DICTIONARY_LIST — root route, после exit
                // приложение перезапускается в SETUP). Защита нужна на случай
                // фоновой подписки таба в back stack при удалении всех словарей.
                EmptyWidget()
            }

            else -> {
                // IS493 Э1: без верхнего зазора — список скроллится вплотную к
                // TabRow, строки уходят ровно за его границу (решение юзера).
                WordListWidget(
                    modifier = Modifier,
                    termList = state.termList.termListFlow.collectAsLazyPagingItems(),
                    openWordCard = { word ->
                        if (state.topBarState.isActionMode) {
                            sendMessage(Msg.ToggleSelection(targetWord = word))
                        } else {
                            sendMessage(Msg.OpenWordCard(wordId = word.id))
                        }
                    },
                    sendMessage = sendMessage,
                )
            }
        }
        if (state.addWordDialogState.isOpen) {
            AddWordPanelWidget(
                modifier = Modifier.align(Alignment.BottomCenter),
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
        if (state.scanDialogState.isOpen) {
            // Наличие камеры — на открытие диалога, не на каждую рекомпозицию.
            val hasCamera = remember { isCameraAvailable() }
            ScanDialog(
                state = state.scanDialogState,
                isCameraAvailable = hasCamera,
                onCamera = { sendMessage(Msg.ChooseCamera) },
                onGallery = { sendMessage(Msg.ChooseGallery) },
                onDismiss = { sendMessage(Msg.CloseScanDialog) },
            )
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewLoading() {
    AppTheme {
        WordsTabContent(
            state = DataHelper.State.loading,
            snackbarHostState = remember { SnackbarHostState() },
            isCameraAvailable = { true },
        ) {}
    }
}

@PreviewWidget
@Composable
private fun PreviewEmpty() {
    AppTheme {
        WordsTabContent(
            state = DataHelper.State.empty,
            snackbarHostState = remember { SnackbarHostState() },
            isCameraAvailable = { true },
        ) {}
    }
}

@PreviewWidget
@Composable
private fun PreviewLoaded() {
    AppTheme {
        WordsTabContent(
            state = DataHelper.State.loaded,
            snackbarHostState = remember { SnackbarHostState() },
            isCameraAvailable = { true },
        ) {}
    }
}
