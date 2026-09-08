package me.apomazkin.groupstab.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import me.apomazkin.di.viewModelFactory
import me.apomazkin.groupstab.R
import me.apomazkin.groupstab.logic.AllNodeState
import me.apomazkin.groupstab.logic.GroupSheetError
import me.apomazkin.groupstab.logic.GroupUiItem
import me.apomazkin.groupstab.logic.GroupWindowState
import me.apomazkin.groupstab.logic.GroupsTabState
import me.apomazkin.groupstab.logic.Msg
import me.apomazkin.groupstab.ui.widget.GroupInputPanelWidget
import me.apomazkin.grouptree.GroupNodeWidget
import me.apomazkin.icondropdowned.DeleteIcon
import me.apomazkin.icondropdowned.EditIcon
import me.apomazkin.icondropdowned.IconDropdownWidget
import me.apomazkin.icondropdowned.MenuItem
import me.apomazkin.icondropdowned.StringSource
import me.apomazkin.theme.AppTheme
import me.apomazkin.theme.LexemeStyle
import me.apomazkin.theme.destructiveColor
import me.apomazkin.theme.grayTextColor
import me.apomazkin.theme.whiteColor
import me.apomazkin.ui.dialog.AlarmDialogWidget
import me.apomazkin.ui.preview.PreviewWidget
import me.apomazkin.wordrow.ui.WordRowWidget

/**
 * IS493 Э3 (D16): handle-паттерн по образцу words — публичный API вкладки
 * «Группы» для app-моста. VM создаётся и коллектится ВНУТРИ модуля;
 * наружу — FAB-читалки и Content.
 */
@Composable
fun rememberGroupsTabHandle(
    factory: GroupsTabViewModel.Factory,
): GroupsTabHandle {
    val viewModel: GroupsTabViewModel = viewModel(
        factory = viewModelFactory { factory.create() },
    )
    val state = viewModel.state.collectAsStateWithLifecycle()
    return remember(viewModel) {
        GroupsTabHandle(viewModel = viewModel, stateProvider = state)
    }
}

@Stable
class GroupsTabHandle internal constructor(
    private val viewModel: GroupsTabViewModel,
    private val stateProvider: State<GroupsTabState>,
) {
    /** FAB скрыт под шторкой/конфирмом (как words: под открытым диалогом). */
    val isFabVisible: State<Boolean> = derivedStateOf {
        val state = stateProvider.value
        state.sheet == null && state.confirmDelete == null
    }

    fun onFabClick() {
        viewModel.accept(Msg.OpenCreateSheet)
    }

    /**
     * Контент вкладки. Проводка словаря (D9.4) — внутри: мост распаковывает
     * DictionarySlot host'а в примитивы, Msg шлётся ТОЛЬКО при isResolved.
     * Ошибки мутаций — снекбаром через host'овый [SnackbarHostState]
     * (решение юзера; шторка к этому моменту закрыта reducer'ом).
     */
    @Composable
    fun Content(
        snackbarHostState: SnackbarHostState,
        dictionaryId: Long?,
        isDictResolved: Boolean,
    ) {
        LaunchedEffect(dictionaryId, isDictResolved) {
            if (isDictResolved) {
                viewModel.accept(Msg.DictionaryChanged(dictionaryId = dictionaryId))
            }
        }
        val error = stateProvider.value.errorSnackbar
        val errorText = error?.let {
            stringResource(
                id = when (it) {
                    GroupSheetError.EMPTY -> R.string.group_error_empty_name
                    GroupSheetError.DUPLICATE -> R.string.group_error_duplicate_name
                    GroupSheetError.RESERVED -> R.string.group_error_reserved_name
                }
            )
        }
        LaunchedEffect(error) {
            if (error != null && errorText != null) {
                snackbarHostState.showSnackbar(errorText)
                viewModel.accept(Msg.ErrorSnackbarShown)
            }
        }
        GroupsTabContent(
            state = stateProvider.value,
            onWordClick = { wordId -> viewModel.accept(Msg.OpenWordCard(wordId)) },
            sendMessage = viewModel::accept,
        )
    }
}

@Composable
internal fun GroupsTabContent(
    state: GroupsTabState,
    onWordClick: (wordId: Long) -> Unit,
    sendMessage: (Msg) -> Unit,
) {
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

            state.hasNoDictionary -> NoDictionaryWidget()

            else -> GroupsList(
                state = state,
                // IS496 Р3: тап по слову при открытой панели — панель
                // закрывается (намерение сменилось), затем карточка.
                onWordClick = { wordId ->
                    if (state.sheet != null) sendMessage(Msg.DismissSheet)
                    onWordClick(wordId)
                },
                sendMessage = sendMessage,
            )
        }
        if (state.sheet != null) {
            GroupInputPanelWidget(
                modifier = Modifier.align(Alignment.BottomCenter),
                state = state.sheet,
                sendMessage = sendMessage,
            )
        }
        // Э6 (финал): ЕДИНЫЙ диалог удаления — кнопка меняется галкой.
        state.confirmDelete?.let { confirm ->
            val count = state.groups.firstOrNull { it.id == confirm.groupId }?.count ?: 0
            DeleteConfirmDialog(
                count = count,
                deleteWords = confirm.deleteWords,
                countdownLeft = confirm.countdownLeft,
                sendMessage = sendMessage,
            )
        }
    }
}

/**
 * Э6 (финал 2026-08-28): единый диалог удаления группы.
 * count≥1 — динамический текст по галке; count=0 — прежний вид (Э3).
 * Галка снята → обычная «Удалить»; отмечена → ДЕСТРУКТИВНАЯ
 * «Удалить всё» (насыщенно-красная), на время паузы осмысления
 * задизейблена со счётчиком В НАЗВАНИИ («Удалить всё (5)»…) — счётчик
 * живёт в state и тикает effect handler'ом, UI только рендерит.
 */
@Composable
private fun DeleteConfirmDialog(
    count: Int,
    deleteWords: Boolean,
    countdownLeft: Int,
    sendMessage: (Msg) -> Unit,
) {
    val counting = deleteWords && countdownLeft > 0
    AlarmDialogWidget(
        alarmButtonText = if (deleteWords) {
            R.string.group_delete_all_button
        } else {
            R.string.group_delete_button
        },
        alarmEnabled = !counting,
        alarmButtonOverride = if (counting) {
            stringResource(id = R.string.group_delete_all_button) + " ($countdownLeft)"
        } else null,
        alarmContainerColor = if (deleteWords) destructiveColor else null,
        alarmContentColor = if (deleteWords) whiteColor else null,
        onAlarmClick = { sendMessage(Msg.ConfirmDelete) },
        onDismissRequest = { sendMessage(Msg.DismissDelete) },
    ) {
        Text(
            text = stringResource(id = R.string.group_delete_confirm),
            style = LexemeStyle.H6,
            color = MaterialTheme.colorScheme.secondary,
        )
        if (count > 0) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = if (deleteWords) {
                    stringResource(id = R.string.group_delete_with_words_warn, count)
                } else {
                    stringResource(id = R.string.group_delete_words_stay, count)
                },
                style = LexemeStyle.BodyM,
                color = MaterialTheme.colorScheme.secondary,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { sendMessage(Msg.ToggleDeleteWords) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = deleteWords,
                    onCheckedChange = { sendMessage(Msg.ToggleDeleteWords) },
                )
                Text(
                    text = stringResource(id = R.string.group_delete_with_words_label),
                    style = LexemeStyle.BodyM,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

/**
 * Единая LazyColumn (D8.3): «Все» с живым окном (Э2) + строки групп (Э3).
 * Ключи — составные, неймспейсы не пересекаются: "node:all"/"all:{id}"/
 * "footer:all" (Э2) и "node:group:{id}"/"empty:group:{id}" (U-4).
 */
@Composable
private fun GroupsList(
    state: GroupsTabState,
    onWordClick: (wordId: Long) -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 4.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.allNode?.let { node ->
            allNodeItems(
                node = node,
                onWordClick = onWordClick,
                sendMessage = sendMessage,
            )
        }
        groupItems(
            groups = state.visibleGroups,
            openMenuGroupId = state.openMenuGroupId,
            expandedGroupWindows = state.expandedGroupWindows,
            onWordClick = onWordClick,
            sendMessage = sendMessage,
        )
    }
}

private fun LazyListScope.allNodeItems(
    node: AllNodeState,
    onWordClick: (wordId: Long) -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    item(key = "node:all") {
        GroupNodeWidget(
            title = stringResource(id = R.string.group_all_title),
            count = node.count,
            isExpanded = node.isExpanded,
            onToggle = { sendMessage(Msg.ToggleAll) },
        )
    }
    if (node.isExpanded) {
        items(
            count = node.loadedWords.size,
            key = { index -> "all:${node.loadedWords[index].id}" },
        ) { index ->
            val term = node.loadedWords[index]
            WordRowWidget(
                termItem = term,
                onClick = { onWordClick(it.id) },
                onLongClick = null,
            )
        }
        if (node.hasMore || node.isWindowLoading) {
            item(key = "footer:all") {
                ChunkFooter(
                    shown = node.loadedWords.size,
                    total = node.count,
                    isWindowLoading = node.isWindowLoading,
                    onLoadMore = { sendMessage(Msg.LoadMore) },
                )
            }
        }
    }
}

private fun LazyListScope.groupItems(
    groups: List<GroupUiItem>,
    openMenuGroupId: Long?,
    expandedGroupWindows: Map<Long, GroupWindowState>,
    onWordClick: (wordId: Long) -> Unit,
    sendMessage: (Msg) -> Unit,
) {
    groups.forEach { group ->
        val window = expandedGroupWindows[group.id]
        item(key = "node:group:${group.id}") {
            GroupNodeWidget(
                title = group.name,
                count = group.count,
                isExpanded = window != null,
                onToggle = { sendMessage(Msg.ToggleGroup(groupId = group.id)) },
                actions = {
                    GroupKebab(
                        groupId = group.id,
                        isOpen = openMenuGroupId == group.id,
                        sendMessage = sendMessage,
                    )
                },
            )
        }
        // Контент — СРАЗУ ПОД своей строкой (фикс ручного прогона Э3),
        // отдельными item (виртуализация, D8.3). Э5: живое окно слов
        // группы (механика «Все»); window=0 — заглушка «пусто».
        if (window != null) {
            if (window.window == 0) {
                item(key = "empty:group:${group.id}") {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        text = stringResource(id = R.string.group_empty_stub),
                        style = LexemeStyle.BodyM,
                        color = grayTextColor,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                items(
                    count = window.loadedWords.size,
                    key = { index -> "group:${group.id}:word:${window.loadedWords[index].id}" },
                ) { index ->
                    val term = window.loadedWords[index]
                    WordRowWidget(
                        termItem = term,
                        onClick = { onWordClick(it.id) },
                        onLongClick = null,
                    )
                }
                if (window.hasMore || window.isLoading) {
                    item(key = "footer:group:${group.id}") {
                        ChunkFooter(
                            shown = window.loadedWords.size,
                            total = group.count,
                            isWindowLoading = window.isLoading,
                            onLoadMore = { sendMessage(Msg.LoadMoreGroup(groupId = group.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupKebab(
    groupId: Long,
    isOpen: Boolean,
    sendMessage: (Msg) -> Unit,
) {
    IconDropdownWidget(
        isDropDownOpen = isOpen,
        onClickDropDown = { sendMessage(Msg.OpenKebab(groupId = groupId)) },
        onDismissRequest = { sendMessage(Msg.DismissKebab) },
    ) {
        MenuItem.withIcon(
            icon = EditIcon,
            title = StringSource.fromRes(resId = R.string.group_menu_rename),
            onClick = { sendMessage(Msg.OpenRenameSheet(groupId = groupId)) },
        ).Widget()
        MenuItem.withIcon(
            icon = DeleteIcon,
            title = StringSource.fromRes(resId = R.string.group_menu_delete),
            onClick = { sendMessage(Msg.RequestDelete(groupId = groupId)) },
        ).Widget()
    }
}

@Composable
private fun ChunkFooter(
    shown: Int,
    total: Int,
    isWindowLoading: Boolean,
    onLoadMore: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isWindowLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            TextButton(onClick = onLoadMore) {
                Text(text = stringResource(id = R.string.group_load_more))
            }
            Text(
                text = stringResource(id = R.string.group_shown_of, shown, total),
                style = LexemeStyle.BodyM,
                color = grayTextColor,
            )
        }
    }
}

/**
 * Empty-state «нет словаря» (строка заглушки Э1 переиспользована).
 * Показывается только при resolved null (D9.1).
 */
@Composable
private fun NoDictionaryWidget() {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.groups_tab_stub_text),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@PreviewWidget
@Composable
private fun PreviewWithGroups() {
    AppTheme {
        GroupsTabContent(
            state = GroupsTabState(
                isLoading = false,
                dictionaryId = 1L,
                allNode = AllNodeState(count = 3, hasMore = true),
                groups = listOf(
                    GroupUiItem(id = 1, name = "Быт", count = 0),
                    GroupUiItem(id = 2, name = "Дом", count = 0),
                ),
                visibleGroups = listOf(
                    GroupUiItem(id = 1, name = "Быт", count = 0),
                    GroupUiItem(id = 2, name = "Дом", count = 0),
                ),
                expandedGroupWindows = mapOf(
                    2L to GroupWindowState(window = 0, isLoading = false),
                ),
            ),
            onWordClick = {},
            sendMessage = {},
        )
    }
}

@PreviewWidget
@Composable
private fun PreviewNoDictionary() {
    AppTheme {
        GroupsTabContent(
            state = GroupsTabState(
                isLoading = false,
                hasNoDictionary = true,
            ),
            onWordClick = {},
            sendMessage = {},
        )
    }
}
