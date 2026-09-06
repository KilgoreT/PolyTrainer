package me.apomazkin.groupstab.logic

import me.apomazkin.groupstab.LogTags
import me.apomazkin.logger.LexemeLogger
import io.github.kilgoret.mate.Effect
import me.apomazkin.mate.ReducerLogging
import io.github.kilgoret.mate.ReducerResult
import io.github.kilgoret.mate.begin
import me.apomazkin.wordrow.entity.TermUiItem
import java.util.Locale

internal typealias GroupsResult = ReducerResult<GroupsTabState, Effect>

/**
 * ###### Атомарные state-экстеншны вкладки «Группы» (конвенция юзера,
 * ручной прогон Э3): один экстеншн = ОДИН технический шаг изменения;
 * КАЖДЫЙ возвращает [ReducerResult] — эффект рождается там же, где
 * изменение (Elm-паттерн), и логируется через [logStep] (логгер — из
 * dispatch receiver'а [ReducerLogging], не параметр и не state).
 * Атом без своих данных пишет [noOp] с причиной — «шаг не сделан»
 * читается в логе прямо. Оркестраторов нет: guard'ы, ветвление и
 * цепочки `begin()/then` — в ветках reducer'а. Reducer и ext-тест
 * НАСЛЕДУЮТ этот класс. ######
 */
abstract class StateAtoms(logger: LexemeLogger) : ReducerLogging(logger) {

    /** Фичевый тег вкладки: шаги, сообщения reducer'а и логи handler'а —
     * под одним `###GROUPS###` (весь флоу одним grep'ом). */
    override val logTag: String = LogTags.GROUPS

    /** ###### ЗАГРУЗКА / СЛОВАРЬ ###### */

    /**
     * Показать общий индикатор загрузки вкладки.
     * Зовётся при смене словаря — до первой эмиссии combine(slice, groupTree).
     * Меняет только [GroupsTabState.isLoading] → true. Эффектов нет.
     */
    fun GroupsTabState.showLoading(): GroupsResult {
        logStep("showLoading")
        return copy(isLoading = true).begin()
    }

    /**
     * Погасить общий индикатор загрузки.
     * Зовётся по приходу slice, по [Msg.SliceLoadFailed] (иначе вечный
     * спиннер) и в цепочке «словарей нет» (грузить нечего).
     * Меняет только [GroupsTabState.isLoading] → false. Эффектов нет.
     */
    fun GroupsTabState.hideLoading(): GroupsResult {
        logStep("hideLoading")
        return copy(isLoading = false).begin()
    }

    /**
     * Поднять флаг «словарей нет» (host прислал resolved null, D9.1;
     * прецедент — words IS476, краш при удалении всех словарей).
     * Меняет только [GroupsTabState.hasNoDictionary] → true; сопутствующие
     * шаги (гашение загрузки, сброс словаря) — отдельные атомы цепочки.
     * Эффектов нет.
     */
    fun GroupsTabState.markNoDictionary(): GroupsResult {
        logStep("markNoDictionary")
        return copy(hasNoDictionary = true).begin()
    }

    /**
     * Снять флаг «словарей нет» — словарь (снова) существует.
     * Зовётся при приходе ненулевого словаря от host.
     * Меняет только [GroupsTabState.hasNoDictionary] → false. Эффектов нет.
     */
    fun GroupsTabState.markDictionaryPresent(): GroupsResult {
        logStep("markDictionaryPresent")
        return copy(hasNoDictionary = false).begin()
    }

    /**
     * Запомнить текущий словарь вкладки.
     * @param id id словаря из DictionarySlot host'а (не null — null-путь идёт
     *   через [clearDictionary]).
     * Меняет только [GroupsTabState.dictionaryId]. Эффектов нет (подписку
     * переключает ветка reducer'а — ей известен контекст смены).
     */
    fun GroupsTabState.selectDictionary(id: Long): GroupsResult {
        logStep("selectDictionary", "id" to id)
        return copy(dictionaryId = id).begin()
    }

    /**
     * Забыть текущий словарь («словарей нет»: держать id мёртвого словаря —
     * противоречие). Меняет только [GroupsTabState.dictionaryId] → null.
     * Эффектов нет.
     */
    fun GroupsTabState.clearDictionary(): GroupsResult {
        logStep("clearDictionary")
        return copy(dictionaryId = null).begin()
    }

    /** ###### УЗЕЛ «ВСЕ» ###### */

    /**
     * Сбросить узел «Все» целиком (смена словаря: старые данные не мигают).
     * Меняет только [GroupsTabState.allNode] → null. Эффектов нет.
     */
    fun GroupsTabState.clearAllNode(): GroupsResult {
        logStep("clearAllNode")
        return copy(allNode = null).begin()
    }

    /**
     * Принять счётчик слов из slice: узла нет (первый slice) → создать
     * свёрнутый узел; узел есть → обновить [AllNodeState.count] и пересчитать
     * [AllNodeState.hasMore] против загруженного. Компенсация окна при росте —
     * отдельный атом [widenWindowForGrowth] (в цепочке ДО этого — ему нужен
     * прежний count). Эффектов нет.
     * @param count число слов словаря (distinct, из DisplayTree.allWords).
     */
    fun GroupsTabState.applyAllCount(count: Int): GroupsResult {
        logStep("applyAllCount", "count" to count)
        val node = allNode
            ?: return copy(allNode = AllNodeState(count = count, hasMore = count > 0)).begin()
        return copy(
            allNode = node.copy(count = count, hasMore = node.loadedWords.size < count),
        ).begin()
    }

    /**
     * Компенсация вытеснения: словарь ВЫРОС под раскрытым узлом с активным
     * окном → окно шире на дельту роста (вставка в голову не выталкивает низ)
     * и эффект [GroupsEffect.SetWindow] с новым лимитом.
     * Зовётся в цепочке SliceLoaded ДО [applyAllCount] — дельта считается от
     * ещё не обновлённого [AllNodeState.count].
     * @param newCount свежий счётчик слов из slice.
     * No-op: узла нет / свёрнут / окно не открыто / роста нет.
     */
    fun GroupsTabState.widenWindowForGrowth(newCount: Int): GroupsResult {
        val node = allNode ?: return noOp("widenWindowForGrowth: no node")
        val growth = newCount - node.count
        if (!node.isExpanded || node.window <= 0) {
            return noOp("widenWindowForGrowth: window closed")
        }
        if (growth <= 0) return noOp("widenWindowForGrowth: no growth")
        val widened = node.window + growth
        logStep("widenWindowForGrowth", "newCount" to newCount, "window" to widened)
        return copy(allNode = node.copy(window = widened)) to
            setOf(GroupsEffect.SetWindow(limit = widened))
    }

    /**
     * Раскрыть узел «Все» (только флаг; окно — отдельный атом [openWindow]).
     * Меняет только [AllNodeState.isExpanded] → true. No-op без узла.
     * Эффектов нет.
     */
    fun GroupsTabState.expandAllNode(): GroupsResult {
        val node = allNode ?: return noOp("expandAllNode: no node")
        logStep("expandAllNode")
        return copy(allNode = node.copy(isExpanded = true)).begin()
    }

    /**
     * Свернуть узел «Все» (только флаг; окно гасит атом [closeWindow]).
     * Меняет только [AllNodeState.isExpanded] → false. No-op без узла.
     * Эффектов нет.
     */
    fun GroupsTabState.collapseAllNode(): GroupsResult {
        val node = allNode ?: return noOp("collapseAllNode: no node")
        logStep("collapseAllNode")
        return copy(allNode = node.copy(isExpanded = false)).begin()
    }

    /**
     * Открыть живое окно контента: [AllNodeState.window] = [limit], спиннер
     * окна включён; эффект [GroupsEffect.SetWindow] запускает подписку.
     * @param limit размер окна (CHUNK_SIZE при раскрытии).
     * No-op без узла.
     */
    fun GroupsTabState.openWindow(limit: Int): GroupsResult {
        val node = allNode ?: return noOp("openWindow: no node")
        logStep("openWindow", "limit" to limit)
        return copy(allNode = node.copy(window = limit, isWindowLoading = true)) to
            setOf(GroupsEffect.SetWindow(limit = limit))
    }

    /**
     * Закрыть живое окно: окно = 0, контент сброшен, спиннер погашен; эффект
     * [GroupsEffect.SetWindow] (null) гасит подписку.
     * [AllNodeState.hasMore] пересчитывает следующий атом цепочки
     * ([recalcHasMore]). No-op без узла.
     */
    fun GroupsTabState.closeWindow(): GroupsResult {
        val node = allNode ?: return noOp("closeWindow: no node")
        logStep("closeWindow")
        return copy(
            allNode = node.copy(window = 0, loadedWords = emptyList(), isWindowLoading = false),
        ) to setOf(GroupsEffect.SetWindow(limit = null))
    }

    /**
     * Расширить окно на шаг «Ещё»: окно += [step], спиннер, эффект
     * [GroupsEffect.SetWindow] с новым лимитом.
     * @param step шаг расширения (CHUNK_SIZE).
     * No-op без узла.
     */
    fun GroupsTabState.widenWindowBy(step: Int): GroupsResult {
        val node = allNode ?: return noOp("widenWindowBy: no node")
        val newWindow = node.window + step
        logStep("widenWindowBy", "step" to step, "window" to newWindow)
        return copy(
            allNode = node.copy(window = newWindow, isWindowLoading = true),
        ) to setOf(GroupsEffect.SetWindow(limit = newWindow))
    }

    /**
     * Принять слова эмиссии живого окна: контент заменяется ЦЕЛИКОМ (не
     * append — окно эмитит всё, что покрывает LIMIT).
     * @param words слова окна в порядке id DESC (голова словаря).
     * Меняет только [AllNodeState.loadedWords]; спиннер и hasMore — следующие
     * атомы цепочки. No-op без узла. Эффектов нет.
     */
    fun GroupsTabState.applyWindowWords(words: List<TermUiItem>): GroupsResult {
        val node = allNode ?: return noOp("applyWindowWords: no node")
        logStep("applyWindowWords", "words" to words.size)
        return copy(allNode = node.copy(loadedWords = words)).begin()
    }

    /**
     * Погасить спиннер окна. Зовётся в цепочке приёма эмиссии и на пути
     * ошибки ([Msg.WindowLoadFailed]): кнопка «Ещё» снова живая.
     * Меняет только [AllNodeState.isWindowLoading] → false. No-op без узла.
     * Эффектов нет.
     */
    fun GroupsTabState.hideWindowLoading(): GroupsResult {
        val node = allNode ?: return noOp("hideWindowLoading: no node")
        logStep("hideWindowLoading")
        return copy(allNode = node.copy(isWindowLoading = false)).begin()
    }

    /**
     * Пересчитать футер «Ещё» против счётчика:
     * [AllNodeState.hasMore] = loadedWords.size < count (после сворачивания
     * контент пуст → hasMore = count > 0). No-op без узла. Эффектов нет.
     */
    fun GroupsTabState.recalcHasMore(): GroupsResult {
        val node = allNode ?: return noOp("recalcHasMore: no node")
        val hasMore = node.loadedWords.size < node.count
        logStep("recalcHasMore", "hasMore" to hasMore)
        return copy(allNode = node.copy(hasMore = hasMore)).begin()
    }

    /**
     * Спрятать футер «Ещё» принудительно: пустой словарь (T-5а) либо «Ещё»
     * нажат, когда показано уже всё (защитная ветка LoadMore).
     * Меняет только [AllNodeState.hasMore] → false. No-op без узла.
     * Эффектов нет.
     */
    fun GroupsTabState.markNoMore(): GroupsResult {
        val node = allNode ?: return noOp("markNoMore: no node")
        logStep("markNoMore")
        return copy(allNode = node.copy(hasMore = false)).begin()
    }

    /** ###### ГРУППЫ (СПИСОК) ###### */

    /**
     * Принять новый список групп из DisplayTree.
     * @param items группы в порядке сортировки Collator'а (порядок задан
     *   handler'ом через buildDisplayTree — здесь не пересортировывается).
     * Меняет только [GroupsTabState.groups]; видимые строки и чистку
     * раскрытых делают следующие атомы цепочки ([refreshVisibleGroups],
     * [purgeDeadExpanded]). Эффектов нет.
     */
    fun GroupsTabState.applyGroups(items: List<GroupUiItem>): GroupsResult {
        logStep("applyGroups", "groups" to items.size)
        return copy(groups = items).begin()
    }

    /**
     * Сбросить список групп (смена словаря).
     * Меняет только [GroupsTabState.groups] → []. Эффектов нет.
     */
    fun GroupsTabState.clearGroups(): GroupsResult {
        logStep("clearGroups")
        return copy(groups = emptyList()).begin()
    }

    /**
     * Пересчитать видимые строки: [GroupsTabState.groups] под префиксным
     * case-insensitive фильтром по вводу шторки — В ОБОИХ режимах (правка
     * юзера); шторки нет / ввод пуст → полный список. Зовётся после любого
     * шага, менявшего groups или шторку. Меняет только
     * [GroupsTabState.visibleGroups]. Эффектов нет.
     */
    fun GroupsTabState.refreshVisibleGroups(): GroupsResult {
        val visible = visibleGroupsFor(groups, sheet)
        logStep("refreshVisibleGroups", "visible" to visible.size)
        return copy(visibleGroups = visible).begin()
    }

    /**
     * Вычистить МЁРТВЫЕ группы из раскрытых: группа удалена под
     * раскрытием (ревью T-2). Зовётся после [applyGroups]. Меняет
     * [GroupsTabState.expandedGroupWindows] (∩ живые); на каждое
     * закрытое окно рождает [GroupsEffect.SetGroupWindow] (null) —
     * подписка мёртвой группы гаснет.
     */
    fun GroupsTabState.purgeDeadExpanded(): GroupsResult {
        val livingIds = groups.mapTo(HashSet()) { it.id }
        val dead = expandedGroupWindows.keys.filter { it !in livingIds }
        logStep("purgeDeadExpanded", "dead" to dead.size)
        if (dead.isEmpty()) return begin()
        val effects: Set<Effect> = dead
            .mapTo(HashSet()) { GroupsEffect.SetGroupWindow(groupId = it, limit = null) }
        return copy(expandedGroupWindows = expandedGroupWindows - dead.toSet()) to effects
    }

    /**
     * Свернуть ВСЕ группы (смена словаря): карта окон очищается.
     * Подписки гасит эффект [GroupsEffect.ClearGroupWindows] ветки
     * reducer'а (контекст смены словаря знает только она). Эффектов нет.
     */
    fun GroupsTabState.collapseAllGroups(): GroupsResult {
        logStep("collapseAllGroups")
        return copy(expandedGroupWindows = emptyMap()).begin()
    }

    /**
     * Раскрыть ПУСТУЮ группу (count=0, T-5а-зеркало): заглушка «пусто»,
     * окно НЕ открывается, подписки нет — `window=0`.
     * @param id id живой группы.
     * Меняет только [GroupsTabState.expandedGroupWindows] (+id).
     * Эффектов нет.
     */
    fun GroupsTabState.expandGroupEmpty(id: Long): GroupsResult {
        logStep("expandGroupEmpty", "id" to id)
        return copy(
            expandedGroupWindows = expandedGroupWindows +
                (id to GroupWindowState(window = 0, isLoading = false)),
        ).begin()
    }

    /**
     * Раскрыть группу С ОКНОМ: окно = [limit], спиннер; эффект
     * [GroupsEffect.SetGroupWindow] запускает живую подписку контента.
     * @param id id живой группы (count > 0).
     * @param limit стартовый размер окна (CHUNK_SIZE).
     */
    fun GroupsTabState.openGroupWindow(id: Long, limit: Int): GroupsResult {
        logStep("openGroupWindow", "id" to id, "limit" to limit)
        return copy(
            expandedGroupWindows = expandedGroupWindows +
                (id to GroupWindowState(window = limit, isLoading = true)),
        ) to setOf(GroupsEffect.SetGroupWindow(groupId = id, limit = limit))
    }

    /**
     * Свернуть группу: ключ уходит из карты, эффект
     * [GroupsEffect.SetGroupWindow] (null) гасит подписку (для пустой
     * раскрытой подписки не было — эффект no-op в handler'е).
     * @param id id группы.
     */
    fun GroupsTabState.closeGroupWindow(id: Long): GroupsResult {
        logStep("closeGroupWindow", "id" to id)
        return copy(expandedGroupWindows = expandedGroupWindows - id) to
            setOf(GroupsEffect.SetGroupWindow(groupId = id, limit = null))
    }

    /**
     * Расширить окно группы на шаг «Ещё»: окно += [step], спиннер,
     * эффект [GroupsEffect.SetGroupWindow] с новым лимитом.
     * @param id id раскрытой группы.
     * @param step шаг расширения (CHUNK_SIZE).
     * No-op, если группа не раскрыта.
     */
    fun GroupsTabState.widenGroupWindowBy(id: Long, step: Int): GroupsResult {
        val win = expandedGroupWindows[id]
            ?: return noOp("widenGroupWindowBy: not expanded")
        val newWindow = win.window + step
        logStep("widenGroupWindowBy", "id" to id, "step" to step, "window" to newWindow)
        return copy(
            expandedGroupWindows = expandedGroupWindows +
                (id to win.copy(window = newWindow, isLoading = true)),
        ) to setOf(GroupsEffect.SetGroupWindow(groupId = id, limit = newWindow))
    }

    /**
     * Принять слова эмиссии окна группы: контент заменяется ЦЕЛИКОМ.
     * @param id id раскрытой группы.
     * @param words слова окна в порядке id DESC.
     * Меняет только [GroupWindowState.loadedWords]; спиннер и hasMore —
     * следующие атомы цепочки. No-op, если группа не раскрыта.
     * Эффектов нет.
     */
    fun GroupsTabState.applyGroupWindowWords(id: Long, words: List<TermUiItem>): GroupsResult {
        val win = expandedGroupWindows[id]
            ?: return noOp("applyGroupWindowWords: not expanded")
        logStep("applyGroupWindowWords", "id" to id, "words" to words.size)
        return copy(
            expandedGroupWindows = expandedGroupWindows + (id to win.copy(loadedWords = words)),
        ).begin()
    }

    /**
     * Погасить спиннер окна группы (эмиссия принята либо
     * [Msg.GroupWindowFailed] — кнопка «Ещё» снова живая).
     * @param id id раскрытой группы.
     * No-op, если группа не раскрыта. Эффектов нет.
     */
    fun GroupsTabState.hideGroupWindowLoading(id: Long): GroupsResult {
        val win = expandedGroupWindows[id]
            ?: return noOp("hideGroupWindowLoading: not expanded")
        logStep("hideGroupWindowLoading", "id" to id)
        return copy(
            expandedGroupWindows = expandedGroupWindows + (id to win.copy(isLoading = false)),
        ).begin()
    }

    /**
     * Пересчитать футер «Ещё» окна группы против живого счётчика:
     * hasMore = loadedWords.size < count ([GroupUiItem.count] из
     * [GroupsTabState.groups]).
     * @param id id раскрытой группы.
     * No-op, если группа не раскрыта. Эффектов нет.
     */
    fun GroupsTabState.recalcGroupHasMore(id: Long): GroupsResult {
        val win = expandedGroupWindows[id]
            ?: return noOp("recalcGroupHasMore: not expanded")
        val count = groups.firstOrNull { it.id == id }?.count ?: 0
        val hasMore = win.loadedWords.size < count
        logStep("recalcGroupHasMore", "id" to id, "hasMore" to hasMore)
        return copy(
            expandedGroupWindows = expandedGroupWindows + (id to win.copy(hasMore = hasMore)),
        ).begin()
    }

    /**
     * Спрятать футер «Ещё» окна группы принудительно (защитная ветка
     * LoadMoreGroup: показано уже всё).
     * @param id id раскрытой группы.
     * No-op, если группа не раскрыта. Эффектов нет.
     */
    fun GroupsTabState.markGroupNoMore(id: Long): GroupsResult {
        val win = expandedGroupWindows[id]
            ?: return noOp("markGroupNoMore: not expanded")
        logStep("markGroupNoMore", "id" to id)
        return copy(
            expandedGroupWindows = expandedGroupWindows + (id to win.copy(hasMore = false)),
        ).begin()
    }

    /**
     * Окна раскрытых групп реагируют на СВЕЖИЕ счётчики slice (D21.1,
     * ревью 4 агентов) — один шаг «окна синхронизированы со счётчиками»:
     *  - раскрыта пустой (window=0), count 0→N — окно АВТООТКРЫВАЕТСЯ
     *    (юзер добавил слово из карточки и смотрит на вкладку);
     *  - открыто окно, count → 0 — окно закрывается в заглушку «пусто»;
     *  - открыто окно, count вырос — компенсация вытеснения (окно шире
     *    на дельту);
     *  - иначе — только пересчёт hasMore.
     * Эффекты [GroupsEffect.SetGroupWindow] рождаются здесь же.
     *
     * **ПОРЯДОК ЗНАЧИМ (ревью Mate-3): в цепочке SliceLoaded — строго
     * ДО [applyGroups]**: прежний count живёт в [GroupsTabState.groups],
     * после applyGroups дельта всегда 0.
     * @param newCounts свежие счётчики групп из DisplayTree
     *   (groupId → count).
     */
    fun GroupsTabState.applyGroupCounts(newCounts: Map<Long, Int>): GroupsResult {
        if (expandedGroupWindows.isEmpty()) return noOp("applyGroupCounts: none expanded")
        val oldCounts = groups.associate { it.id to it.count }
        val updated = expandedGroupWindows.toMutableMap()
        val effects = HashSet<Effect>()
        expandedGroupWindows.forEach { (id, win) ->
            // Мёртвых (нет в newCounts) не трогаем — их закроет
            // purgeDeadExpanded после applyGroups.
            val newCount = newCounts[id] ?: return@forEach
            val oldCount = oldCounts[id] ?: 0
            val growth = newCount - oldCount
            when {
                win.window == 0 && newCount > 0 -> {
                    updated[id] = GroupWindowState(window = CHUNK_SIZE, isLoading = true)
                    effects += GroupsEffect.SetGroupWindow(groupId = id, limit = CHUNK_SIZE)
                }

                win.window > 0 && newCount == 0 -> {
                    updated[id] = GroupWindowState(window = 0, isLoading = false)
                    effects += GroupsEffect.SetGroupWindow(groupId = id, limit = null)
                }

                win.window > 0 && growth > 0 -> {
                    val widened = win.window + growth
                    updated[id] = win.copy(
                        window = widened,
                        hasMore = win.loadedWords.size < newCount,
                    )
                    effects += GroupsEffect.SetGroupWindow(groupId = id, limit = widened)
                }

                else -> updated[id] = win.copy(hasMore = win.loadedWords.size < newCount)
            }
        }
        logStep("applyGroupCounts", "expanded" to updated.size, "effects" to effects.size)
        return copy(expandedGroupWindows = updated) to effects
    }

    /** ###### ШТОРКА ###### */

    /**
     * Открыть шторку СОЗДАНИЯ группы (FAB): пустой ввод, без ошибки.
     * Меняет только [GroupsTabState.sheet]; фильтр списка пересчитывает
     * следующий атом цепочки ([refreshVisibleGroups]). Эффектов нет.
     */
    fun GroupsTabState.openCreateSheet(): GroupsResult {
        logStep("openCreateSheet")
        return copy(sheet = GroupSheetState(mode = GroupSheetMode.Create)).begin()
    }

    /**
     * Открыть шторку ПЕРЕИМЕНОВАНИЯ (kebab): ввод предзаполнен текущим
     * именем — оно сразу отфильтрует список ([refreshVisibleGroups] следом;
     * правка юзера: видеть занятые имена важнее).
     * @param groupId id переименовываемой группы.
     * @param name её текущее имя (предзаполнение ввода).
     * Меняет только [GroupsTabState.sheet]. Эффектов нет.
     */
    fun GroupsTabState.openRenameSheet(groupId: Long, name: String): GroupsResult {
        logStep("openRenameSheet", "groupId" to groupId, "name" to name)
        return copy(
            sheet = GroupSheetState(mode = GroupSheetMode.Rename(groupId = groupId), input = name),
        ).begin()
    }

    /**
     * Убрать шторку. Зовётся на dismiss, Applied и NotFound-гонку rename;
     * сброс фильтра — следующий атом цепочки ([refreshVisibleGroups]).
     * Меняет только [GroupsTabState.sheet] → null. Эффектов нет.
     */
    fun GroupsTabState.closeSheet(): GroupsResult {
        logStep("closeSheet")
        return copy(sheet = null).begin()
    }

    /**
     * Принять ввод шторки.
     * @param value полный текущий текст поля (не дельта).
     * Меняет только [GroupSheetState.input]; гашение ошибки и фильтр —
     * следующие атомы цепочки ([clearSheetError], [refreshVisibleGroups]).
     * No-op, если шторка закрыта. Эффектов нет.
     */
    fun GroupsTabState.updateSheetInput(value: String): GroupsResult {
        val sheet = sheet ?: return noOp("updateSheetInput: sheet closed")
        logStep("updateSheetInput", "value" to value)
        return copy(sheet = sheet.copy(input = value)).begin()
    }

    /**
     * Погасить ошибку валидации шторки (юзер правит ввод — сообщение
     * устарело). Меняет только [GroupSheetState.error] → null.
     * No-op, если шторка закрыта. Эффектов нет.
     */
    fun GroupsTabState.clearSheetError(): GroupsResult {
        val sheet = sheet ?: return noOp("clearSheetError: sheet closed")
        logStep("clearSheetError")
        return copy(sheet = sheet.copy(error = null)).begin()
    }

    /**
     * Показать ошибку валидации строкой ПОД полем шторки (снекбар под
     * шторкой/клавиатурой не виден — ручной прогон Э3).
     * @param error тип ошибки (EMPTY/DUPLICATE/RESERVED) — текст резолвит UI.
     * Меняет только [GroupSheetState.error]; разблокировку submit делает
     * следующий атом цепочки ([unmarkSubmitting]). No-op, если шторка
     * закрыта. Эффектов нет.
     */
    fun GroupsTabState.showSheetError(error: GroupSheetError): GroupsResult {
        val sheet = sheet ?: return noOp("showSheetError: sheet closed")
        logStep("showSheetError", "error" to error)
        return copy(sheet = sheet.copy(error = error)).begin()
    }

    /**
     * Пометить «запись летит»: submit-кнопка шторки дизейблится
     * (ignore-while-committing, А11). Сам эффект мутации добавляет ветка
     * reducer'а (`withEffect`) — только ей известны dictionaryId и mode.
     * No-op, если шторка закрыта.
     */
    fun GroupsTabState.markSubmitting(): GroupsResult {
        val sheet = sheet ?: return noOp("markSubmitting: sheet closed")
        logStep("markSubmitting")
        return copy(sheet = sheet.copy(isSubmitting = true)).begin()
    }

    /**
     * Снять «запись летит»: submit снова доступен — пути ошибок
     * ([Msg.GroupMutationFailed], Rejected-исходы) и Ignored-заделы Э4/Э6.
     * No-op, если шторка закрыта. Эффектов нет.
     */
    fun GroupsTabState.unmarkSubmitting(): GroupsResult {
        val sheet = sheet ?: return noOp("unmarkSubmitting: sheet closed")
        logStep("unmarkSubmitting")
        return copy(sheet = sheet.copy(isSubmitting = false)).begin()
    }

    /**
     * Показать ошибку мутации снекбаром host'а — ТОЛЬКО гонка dismiss
     * (шторка уже закрыта, inline негде показать).
     * @param error тип ошибки.
     * Меняет только [GroupsTabState.errorSnackbar]. Эффектов нет.
     */
    fun GroupsTabState.showErrorSnackbar(error: GroupSheetError): GroupsResult {
        logStep("showErrorSnackbar", "error" to error)
        return copy(errorSnackbar = error).begin()
    }

    /**
     * Сброс флага снекбара после показа ([Msg.ErrorSnackbarShown]) — иначе
     * LaunchedEffect показал бы его повторно при рекомпозиции. Эффектов нет.
     */
    fun GroupsTabState.consumeErrorSnackbar(): GroupsResult {
        logStep("consumeErrorSnackbar")
        return copy(errorSnackbar = null).begin()
    }

    /** ###### КОНФИРМ / КЕБАБ ###### */

    /**
     * Показать конфирм удаления группы (В2: конфирм — всегда). Свежий
     * объект: галка при каждом открытии снята (Э6).
     * @param groupId id удаляемой группы (хранится до Confirm/Dismiss).
     * Меняет только [GroupsTabState.confirmDelete]. Эффектов нет.
     */
    fun GroupsTabState.askDeleteConfirm(groupId: Long): GroupsResult {
        logStep("askDeleteConfirm", "groupId" to groupId)
        return copy(confirmDelete = ConfirmDeleteState(groupId = groupId)).begin()
    }

    /**
     * Э6: переключить галку «удалить вместе со словами». Лог — СО
     * ЗНАЧЕНИЕМ (ревью UX-5). No-op без конфирма.
     * Отметка галки запускает ПАУЗУ ОСМЫСЛЕНИЯ (решение юзера):
     * [ConfirmDeleteState.countdownLeft] = [DELETE_COUNTDOWN_SEC] +
     * эффект [GroupsEffect.StartDeleteCountdown] (тики шлёт handler);
     * повторная отметка перезапускает. Снятие — счётчик в 0 + эффект
     * [GroupsEffect.CancelDeleteCountdown].
     */
    fun GroupsTabState.toggleDeleteWords(): GroupsResult {
        val confirm = confirmDelete ?: return noOp("toggleDeleteWords: no confirm")
        val toggled = !confirm.deleteWords
        logStep("toggleDeleteWords", "deleteWords" to toggled)
        return if (toggled) {
            copy(
                confirmDelete = confirm.copy(
                    deleteWords = true,
                    countdownLeft = DELETE_COUNTDOWN_SEC,
                ),
            ) to setOf(GroupsEffect.StartDeleteCountdown(seconds = DELETE_COUNTDOWN_SEC))
        } else {
            copy(
                confirmDelete = confirm.copy(deleteWords = false, countdownLeft = 0),
            ) to setOf(GroupsEffect.CancelDeleteCountdown)
        }
    }

    /**
     * Э6: секундный тик паузы осмысления ([Msg.DeleteCountdownTick] от
     * handler'а): [ConfirmDeleteState.countdownLeft] − 1 (не ниже 0).
     * No-op: конфирма нет / галка снята / уже 0 (хвостовой тик после
     * отмены). Эффектов нет.
     */
    fun GroupsTabState.tickDeleteCountdown(): GroupsResult {
        val confirm = confirmDelete ?: return noOp("tickDeleteCountdown: no confirm")
        if (!confirm.deleteWords) return noOp("tickDeleteCountdown: checkbox off")
        if (confirm.countdownLeft <= 0) return noOp("tickDeleteCountdown: already zero")
        val left = confirm.countdownLeft - 1
        logStep("tickDeleteCountdown", "left" to left)
        return copy(confirmDelete = confirm.copy(countdownLeft = left)).begin()
    }

    /**
     * Закрыть конфирм удаления (по Confirm — эффект добавляет ветка
     * reducer'а, по Dismiss). Эффектов нет.
     */
    fun GroupsTabState.closeDeleteConfirm(): GroupsResult {
        logStep("closeDeleteConfirm")
        return copy(confirmDelete = null).begin()
    }

    /**
     * Э6 (ревью Mate-1): конфирм на МЁРТВОЙ группе (нет в живых groups)
     * закрывается — иначе висит на трупе, а lookup N проваливается.
     * Шаг цепочки SliceLoaded ПОСЛЕ [applyGroups]. No-op: конфирма нет
     * либо группа жива. Эффектов нет.
     */
    fun GroupsTabState.closeConfirmForDeadGroup(): GroupsResult {
        val confirm = confirmDelete ?: return noOp("closeConfirmForDeadGroup: no confirm")
        if (groups.any { it.id == confirm.groupId }) {
            return noOp("closeConfirmForDeadGroup: group alive")
        }
        logStep("closeConfirmForDeadGroup", "groupId" to confirm.groupId)
        // Возможные тики паузы осмысления гасятся вместе с конфирмом.
        return copy(confirmDelete = null) to setOf(GroupsEffect.CancelDeleteCountdown)
    }

    /**
     * Открыть kebab-меню строки группы (controlled-дропдаун, U-2/F-3).
     * @param groupId id группы, чьё меню открыто (одно меню за раз).
     * Меняет только [GroupsTabState.openMenuGroupId]. Эффектов нет.
     */
    fun GroupsTabState.openKebab(groupId: Long): GroupsResult {
        logStep("openKebab", "groupId" to groupId)
        return copy(openMenuGroupId = groupId).begin()
    }

    /**
     * Закрыть kebab-меню (dismiss дропдауна либо выбор пункта). Эффектов нет.
     */
    fun GroupsTabState.closeKebab(): GroupsResult {
        logStep("closeKebab")
        return copy(openMenuGroupId = null).begin()
    }

    /** ###### ВНУТРЕННИЙ ХЕЛПЕР (чистая функция, не шаг — не логируется) ###### */

    /**
     * Видимый список: префиксный case-insensitive фильтр по вводу шторки —
     * в обоих режимах (правка юзера). Locale — default (D12.3).
     */
    private fun visibleGroupsFor(
        groups: List<GroupUiItem>,
        sheet: GroupSheetState?,
    ): List<GroupUiItem> {
        if (sheet == null) return groups
        val pattern = sheet.input.trim()
        if (pattern.isEmpty()) return groups
        val locale = Locale.getDefault()
        val key = pattern.lowercase(locale)
        return groups.filter { it.name.lowercase(locale).startsWith(key) }
    }
}
