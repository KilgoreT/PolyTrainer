package me.apomazkin.groupstab.logic

import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.MateReducer
import io.github.kilgoret.mate.ReducerResult
import io.github.kilgoret.mate.begin
import io.github.kilgoret.mate.then
import io.github.kilgoret.mate.withEffect
import me.apomazkin.logger.LexemeLogger

/** Стартовый размер окна и шаг «Ещё». Решение юзера (2026-08-11): 10. */
internal const val CHUNK_SIZE = 10

/** Э6: секунды паузы осмысления деструктива (решение юзера 2026-08-28). */
internal const val DELETE_COUNTDOWN_SEC = 5

/**
 * IS493 Э2/Э3 (D10 v3, D15): reducer вкладки «Группы».
 *
 * Конвенция (юзер, ручной прогон Э3): Msg → видимая ЦЕПОЧКА атомарных
 * state-экстеншнов ([StateAtoms]) — технические шаги изменения читаются
 * прямо в ветке; guard'ы и ветвление тоже здесь. Каждый атом возвращает
 * [ReducerResult] — эффекты рождаются в атоме и аккумулируются
 * комбинаторами `begin/then/withEffect` (mate/ReducerChain), а сам шаг
 * логируется `Reduce ---step---` (логгер приходит атомам через dispatch
 * receiver — наследование [StateAtoms] → mate/ReducerLogging).
 */
class GroupsTabReducer(
    logger: LexemeLogger,
) : StateAtoms(logger),
    MateReducer<GroupsTabState, Msg, Effect> {
    override fun reduce(
        state: GroupsTabState,
        message: Msg,
    ): ReducerResult<GroupsTabState, Effect> {
        // Data-Msg логируются именем (полный toString тащит списки слов);
        // их содержимое handler уже пишет компактно (slice/window: counts).
        logMessage(
            when (message) {
                is Msg.WindowLoaded, is Msg.SliceLoaded -> message::class.simpleName.orEmpty()
                else -> message.toString()
            },
        )
        return reduceMessage(state, message)
    }

    private fun reduceMessage(
        state: GroupsTabState,
        message: Msg,
    ): ReducerResult<GroupsTabState, Effect> =
        when (message) {
            is Msg.DictionaryChanged ->
                when {
                    message.dictionaryId == null ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.markNoDictionary() }
                            .then { it.hideLoading() }
                            .then { it.clearDictionary() }
                            .then { it.clearAllNode() }
                            .then { it.clearGroups() }
                            .then { it.collapseAllGroups() }
                            .then { it.closeSheet() }
                            .then { it.refreshVisibleGroups() }
                            .then { it.closeDeleteConfirm() }
                            .then { it.closeKebab() }
                    // Подписки (slice/окна/тикер) погасит дифф
                    // subscriptions() — dictionaryId=null убирает всё из набора.

                    // Повторный LaunchedEffect при рекомпозиции.
                    message.dictionaryId == state.dictionaryId -> state.noOp("same dictionary")

                    else ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.selectDictionary(message.dictionaryId) }
                            .then { it.markDictionaryPresent() }
                            .then { it.showLoading() }
                            .then { it.clearAllNode() }
                            .then { it.clearGroups() }
                            .then { it.collapseAllGroups() }
                            .then { it.closeSheet() }
                            .then { it.refreshVisibleGroups() }
                            .then { it.closeDeleteConfirm() }
                            .then { it.closeKebab() }
                    // Slice нового словаря включит дифф subscriptions()
                    // (Slice(dictionaryId) появился в наборе), старые окна/тикер
                    // погаснут (их условия в state обнулены цепочкой выше).
                }

            // Синхронизация окон групп и компенсация окна «Все» — ДО
            // applyGroups/applyAllCount (дельты считаются от прежних count).
            is Msg.SliceLoaded ->
                state
                    .begin<GroupsTabState, Effect>()
                    .then { it.hideLoading() }
                    .then { it.applyGroupCounts(message.tree.toGroupCounts()) }
                    .then { it.applyGroups(message.tree.toGroupItems()) }
                    .then { it.purgeDeadExpanded() }
                    .then { it.closeConfirmForDeadGroup() }
                    .then { it.refreshVisibleGroups() }
                    .then { it.widenWindowForGrowth(newCount = message.tree.allWords.count) }
                    .then { it.applyAllCount(count = message.tree.allWords.count) }

            is Msg.ToggleAll ->
                when {
                    // Slice ещё не пришёл — узла нет.
                    state.allNode == null -> state.noOp("toggleAll: no node yet")

                    state.allNode.isExpanded ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.collapseAllNode() }
                            .then { it.closeWindow() }
                            .then { it.recalcHasMore() }

                    // Пустой словарь (T-5а): раскрыть без подписки, футер спрятать.
                    state.allNode.count == 0 ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.expandAllNode() }
                            .then { it.markNoMore() }

                    else ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.expandAllNode() }
                            .then { it.openWindow(limit = CHUNK_SIZE) }
                }

            is Msg.LoadMore ->
                when {
                    // Узла нет / свёрнут / окно уже грузится (спам «Ещё», А11).
                    state.allNode == null -> state.noOp("loadMore: no node")
                    !state.allNode.isExpanded -> state.noOp("loadMore: node collapsed")
                    state.allNode.isWindowLoading -> state.noOp("loadMore: window already loading")

                    // Показано уже всё — спрятать футер принудительно.
                    state.allNode.loadedWords.size >= state.allNode.count -> state.markNoMore()

                    else -> state.widenWindowBy(step = CHUNK_SIZE)
                }

            is Msg.WindowLoaded ->
                when {
                    // Эмиссия догнала сворачивание/сброс — контент не нужен.
                    state.allNode?.isExpanded != true -> state.noOp("windowLoaded: node collapsed")

                    else ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.applyWindowWords(message.words) }
                            .then { it.hideWindowLoading() }
                            .then { it.recalcHasMore() }
                }

            is Msg.WindowLoadFailed -> state.hideWindowLoading()

            is Msg.SliceLoadFailed -> state.hideLoading()

            // === Э3 (D15) ===

            is Msg.OpenCreateSheet ->
                state
                    .begin<GroupsTabState, Effect>()
                    .then { it.openCreateSheet() }
                    .then { it.refreshVisibleGroups() }

            // Lookup живой группы; нет (гонка с удалением) — no-op.
            is Msg.OpenRenameSheet ->
                state
                    .groups
                    .firstOrNull { it.id == message.groupId }
                    ?.let { group ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.openRenameSheet(groupId = group.id, name = group.name) }
                            .then { it.refreshVisibleGroups() }
                            .then { it.closeKebab() }
                    } ?: state.noOp("openRenameSheet: group already gone")

            is Msg.SheetInputChanged ->
                when {
                    state.sheet == null -> state.noOp("sheetInputChanged: sheet closed")

                    else ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.updateSheetInput(message.value) }
                            .then { it.clearSheetError() }
                            .then { it.refreshVisibleGroups() }
                }

            is Msg.SubmitSheet ->
                when {
                    // Шторка закрыта / запись уже летит (спам, А11) / словаря нет.
                    state.sheet == null -> state.noOp("submit: sheet closed")
                    state.sheet.isSubmitting -> state.noOp("submit: already submitting")
                    state.dictionaryId == null -> state.noOp("submit: no dictionary")

                    else ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.markSubmitting() }
                            .withEffect(state.sheet.toMutationEffect(dictionaryId = state.dictionaryId))
                }

            is Msg.DismissSheet ->
                state
                    .begin<GroupsTabState, Effect>()
                    .then { it.closeSheet() }
                    .then { it.refreshVisibleGroups() }

            // Исходы мутаций — плоские Msg (handler маппит доменные outcomes
            // через toMutationMsg до отправки).
            is Msg.MutationApplied ->
                when {
                    // Гонка dismiss (U-3): шторка уже закрыта.
                    state.sheet == null -> state.noOp("applied: sheet already dismissed")

                    else ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.closeSheet() }
                            .then { it.refreshVisibleGroups() }
                }

            is Msg.MutationRejected ->
                when {
                    // Гонка dismiss: inline негде показать — снекбар host'а.
                    state.sheet == null -> state.showErrorSnackbar(message.error)

                    else ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.showSheetError(message.error) }
                            .then { it.unmarkSubmitting() }
                }

            is Msg.MutationIgnored -> state.unmarkSubmitting()

            is Msg.RequestDelete ->
                state
                    .begin<GroupsTabState, Effect>()
                    .then { it.askDeleteConfirm(message.groupId) }
                    .then { it.closeKebab() }

            // Конфирм не открыт — no-op.
            // Э6 (финал: единый диалог — переспрос заменён паузой
            // осмысления со счётчиком в названии кнопки).
            is Msg.ConfirmDelete -> {
                val confirm = state.confirmDelete
                when {
                    confirm == null -> state.noOp("confirmDelete: nothing pending")

                    // Пауза осмысления тикает — reducer не верит UI-дизейблу.
                    confirm.deleteWords && confirm.countdownLeft > 0 ->
                        state.noOp("confirmDelete: countdown running")

                    // «Удалить всё» — ДЕСТРУКТИВ (галка + счётчик оттикал).
                    // Guard по живому счётчику (ревью Mate-3): count упал до
                    // 0 под отмеченной галкой → галка игнорируется.
                    confirm.deleteWords && groupCount(state, confirm.groupId) > 0 ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.closeDeleteConfirm() }
                            .withEffect(GroupsEffect.DeleteGroupWithWords(groupId = confirm.groupId))

                    else ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.closeDeleteConfirm() }
                            .withEffect(GroupsEffect.DeleteGroup(groupId = confirm.groupId))
                }
            }

            is Msg.ToggleDeleteWords -> state.toggleDeleteWords()

            is Msg.DeleteCountdownTick -> state.tickDeleteCountdown()

            // Тикер паузы осмысления погасит дифф (конфирм = null).
            is Msg.DismissDelete -> state.closeDeleteConfirm()

            // Список обновит живая подписка (combine slice+groupTree).
            is Msg.DeleteOutcomeMsg -> state.noOp("delete outcome: list updated by subscription")

            is Msg.DeleteWithWordsOutcomeMsg ->
                state.noOp("delete-with-words outcome: list updated by subscription")

            is Msg.GroupMutationFailed -> state.unmarkSubmitting()

            is Msg.ErrorSnackbarShown -> state.consumeErrorSnackbar()

            is Msg.OpenKebab -> state.openKebab(message.groupId)

            is Msg.DismissKebab -> state.closeKebab()

            // Тап по слову: разовое намерение открыть карточку — довозит
            // shared nav-handler по таблице appNavTable.
            is Msg.OpenWordCard ->
                state to setOf(GroupsNavigationEffect.OpenWordCard(wordId = message.wordId))

            // Э5: раскрытие = окно контента (count=0 — заглушка без подписки).
            is Msg.ToggleGroup ->
                when {
                    message.groupId in state.expandedGroupWindows ->
                        state.closeGroupWindow(message.groupId)

                    groupCount(state, message.groupId) == 0 ->
                        state.expandGroupEmpty(message.groupId)

                    else -> state.openGroupWindow(id = message.groupId, limit = CHUNK_SIZE)
                }

            is Msg.LoadMoreGroup -> {
                val win = state.expandedGroupWindows[message.groupId]
                when {
                    win == null -> state.noOp("loadMoreGroup: not expanded")
                    win.isLoading -> state.noOp("loadMoreGroup: window already loading")

                    // Показано уже всё — спрятать футер принудительно.
                    win.loadedWords.size >= groupCount(state, message.groupId) ->
                        state.markGroupNoMore(message.groupId)

                    else -> state.widenGroupWindowBy(id = message.groupId, step = CHUNK_SIZE)
                }
            }

            is Msg.GroupWindowLoaded ->
                when {
                    // Эмиссия догнала сворачивание/смену словаря.
                    message.groupId !in state.expandedGroupWindows ->
                        state.noOp("groupWindowLoaded: window closed")

                    else ->
                        state
                            .begin<GroupsTabState, Effect>()
                            .then { it.applyGroupWindowWords(message.groupId, message.words) }
                            .then { it.hideGroupWindowLoading(message.groupId) }
                            .then { it.recalcGroupHasMore(message.groupId) }
                }

            is Msg.GroupWindowFailed ->
                when {
                    message.groupId !in state.expandedGroupWindows ->
                        state.noOp("groupWindowFailed: window closed")

                    else -> state.hideGroupWindowLoading(message.groupId)
                }
        }

    /** Живой счётчик группы (GroupUiItem.count из slice); нет группы — 0. */
    private fun groupCount(
        state: GroupsTabState,
        groupId: Long,
    ): Int = state.groups.firstOrNull { it.id == groupId }?.count ?: 0
}

/** Маппер входа: группы DisplayTree → строки списка (порядок сохраняется). */
private fun me.apomazkin.group.DisplayTree.toGroupItems(): List<GroupUiItem> =
    groups.map { node ->
        GroupUiItem(id = node.id, name = node.name, count = node.subtreeWordCount)
    }

/** Маппер входа: свежие счётчики групп для applyGroupCounts (Э5). */
private fun me.apomazkin.group.DisplayTree.toGroupCounts(): Map<Long, Int> = groups.associate { it.id to it.subtreeWordCount }
