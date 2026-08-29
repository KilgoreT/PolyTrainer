package me.apomazkin.vocabulary.ui

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.di.viewModelFactory
import me.apomazkin.theme.AppTheme
import me.apomazkin.ui.SystemBarsWidget
import me.apomazkin.ui.btn.PrimaryFabWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.vocabulary.DictionarySlot
import me.apomazkin.vocabulary.R
import me.apomazkin.vocabulary.TabSpec
import me.apomazkin.vocabulary.VocabularyTab
import me.apomazkin.vocabulary.deps.VocabularyHostUiDeps
import me.apomazkin.vocabulary.logic.Msg
import me.apomazkin.vocabulary.logic.VocabularyHostState

/**
 * IS493 Э1: host вкладок таба словаря. Владеет Scaffold (containerColor,
 * insets, padding — перенос с прежнего words-Scaffold), TabRow (в content,
 * НЕ в topBar-слоте — тот свопится ActionMode-баром words), контекстным FAB
 * (с прежней анимацией) и статусбаром (D1.7: цвет от topBarOverride активной
 * вкладки — гонки words-SystemBars больше нет).
 *
 * @param tabs контракт вкладок; собирает app-мост в remember (D4.1).
 * @param onTabSwitched вызывается ТОЛЬКО при фактической смене вкладки
 *   (same-tab guard внутри, D4.2) — мост сбрасывает ActionMode words.
 */
@Composable
fun VocabularyHostScreen(
    tabs: Map<VocabularyTab, TabSpec>,
    uiDeps: VocabularyHostUiDeps,
    factory: VocabularyHostViewModel.Factory,
    onTabSwitched: (VocabularyTab) -> Unit = {},
    viewModel: VocabularyHostViewModel = viewModel(
        factory = viewModelFactory { factory.create() },
    ),
) {
    val state: VocabularyHostState by viewModel.state.collectAsStateWithLifecycle()
    VocabularyHostScreen(
        state = state,
        tabs = tabs,
        uiDeps = uiDeps,
        onTabClick = { tab ->
            if (tab != state.selectedTab) {
                viewModel.accept(Msg.SelectTab(tab = tab))
                onTabSwitched(tab)
            }
        },
    )
}

@Composable
internal fun VocabularyHostScreen(
    state: VocabularyHostState,
    tabs: Map<VocabularyTab, TabSpec>,
    uiDeps: VocabularyHostUiDeps,
    onTabClick: (VocabularyTab) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val saveableStateHolder = rememberSaveableStateHolder()
    val activeSpec: TabSpec = tabs.getValue(state.selectedTab)
    // Читалка вызывается здесь (host-scope): флип ActionMode рекомпозирует
    // только host-обвязку, спеки стабильны (контракт стабильности TabContract).
    val isTopBarOverridden = activeSpec.isTopBarOverridden()

    SystemBarsWidget(
        statusBarColor = if (isTopBarOverridden) {
            MaterialTheme.colorScheme.secondary
        } else {
            Color.Transparent
        },
        statusBarDarkIcon = !isTopBarOverridden,
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (isTopBarOverridden) {
                activeSpec.topBarOverride()
            } else {
                uiDeps.AppBar(titleResId = R.string.item_title_vocabulary)
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(
            left = 0.dp, top = 0.dp, right = 0.dp, bottom = 0.dp
        ),
        floatingActionButton = {
            activeSpec.fab?.let { fab ->
                val fabVisible = fab.visible()
                AnimatedVisibility(
                    visible = fabVisible,
                    enter = scaleIn(),
                    exit = scaleOut(),
                ) {
                    PrimaryFabWidget(
                        iconRes = fab.iconRes,
                        enabled = fabVisible,
                    ) { fab.onClick() }
                }
            }
        },
    ) { paddingValue: PaddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = Color.Transparent,
                // Сжатая высота (дефолт 48dp): убирает воздух между AppBar и
                // текстом табов — в коде зазора нет, его создавали внутренние
                // высоты панелей (решение юзера, Э1).
                modifier = Modifier.height(40.dp),
            ) {
                VocabularyTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == state.selectedTab,
                        onClick = { onTabClick(tab) },
                        text = {
                            Text(text = stringResource(id = tabs.getValue(tab).titleRes))
                        },
                    )
                }
            }
            saveableStateHolder.SaveableStateProvider(key = state.selectedTab.name) {
                // DictionarySlot — параметр вызова (D9.4): спеки стабильны,
                // смена словаря меняет только аргумент.
                activeSpec.content(
                    snackbarHostState,
                    DictionarySlot(
                        id = state.dictionaryId,
                        isResolved = state.isDictResolved,
                    ),
                )
            }
        }
    }
}

private val previewUiDeps = object : VocabularyHostUiDeps {
    @Composable
    override fun AppBar(@StringRes titleResId: Int) {
    }
}

private fun previewTabs(): Map<VocabularyTab, TabSpec> = mapOf(
    VocabularyTab.WORDS to TabSpec(
        titleRes = R.string.vocabulary_tab_words,
        content = { _, _ -> Text(text = "words") },
    ),
    VocabularyTab.GROUPS to TabSpec(
        titleRes = R.string.vocabulary_tab_groups,
        content = { _, _ -> Text(text = "groups") },
    ),
)

@PreviewWidget
@Composable
private fun PreviewWordsTab() {
    AppTheme {
        VocabularyHostScreen(
            state = VocabularyHostState(selectedTab = VocabularyTab.WORDS),
            tabs = previewTabs(),
            uiDeps = previewUiDeps,
            onTabClick = {},
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewGroupsTab() {
    AppTheme {
        VocabularyHostScreen(
            state = VocabularyHostState(selectedTab = VocabularyTab.GROUPS),
            tabs = previewTabs(),
            uiDeps = previewUiDeps,
            onTabClick = {},
        )
    }
}
