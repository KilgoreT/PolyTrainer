package me.apomazkin.groupstab.logic

import io.github.kilgoret.mate.Subscription
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Mate Э4 | Тесты чистой функции [subscriptions]: декларативный вывод
 * набора подписок вкладки «Группы» из state. Раннер диффит набор после
 * каждого изменения state — здесь проверяются только условия попадания
 * подписок в набор (Slice / AllWindow / GroupWindow / DeleteCountdown).
 */
class SubsTest {
    // === Slice / словарь ===

    @Test
    fun `no dictionary - only current dict subscription survives`() {
        // dictionaryId=null режет всё словарное; слушать остаётся
        // только сам выбор словаря (CurrentDict — безусловна).
        val state = GroupsTabState(
            dictionaryId = null,
            allNode = AllNodeState(count = 3, isExpanded = true, window = 10),
            expandedGroupWindows = mapOf(5L to GroupWindowState(window = 10)),
            confirmDelete = ConfirmDeleteState(groupId = 5, deleteWords = true, countdownLeft = 3),
        )

        assertEquals(setOf<Subscription>(GroupsSub.CurrentDict), state.subscriptions())
    }

    @Test
    fun `dictionary only - single slice subscription`() {
        val state = GroupsTabState(dictionaryId = 1L)

        assertEquals(setOf<Subscription>(GroupsSub.CurrentDict, GroupsSub.Slice(dictionaryId = 1L)), state.subscriptions())
    }

    // === Окно «Все» ===

    @Test
    fun `expanded all node with window - all window with limit`() {
        val state = GroupsTabState(
            dictionaryId = 1L,
            allNode = AllNodeState(count = 30, isExpanded = true, window = 20),
        )

        assertEquals(
            setOf(
                GroupsSub.CurrentDict,
                GroupsSub.Slice(dictionaryId = 1L),
                GroupsSub.AllWindow(dictionaryId = 1L, limit = 20),
            ),
            state.subscriptions(),
        )
    }

    @Test
    fun `collapsed node or zero window - no all window subscription`() {
        // Свёрнут (окно осталось в state — подписки всё равно нет).
        val collapsed = GroupsTabState(
            dictionaryId = 1L,
            allNode = AllNodeState(count = 30, isExpanded = false, window = 10),
        )
        assertEquals(setOf<Subscription>(GroupsSub.CurrentDict, GroupsSub.Slice(1L)), collapsed.subscriptions())

        // Раскрыт пустым (T-5а): window=0 — заглушка без подписки.
        val zeroWindow = GroupsTabState(
            dictionaryId = 1L,
            allNode = AllNodeState(count = 0, isExpanded = true, window = 0),
        )
        assertEquals(setOf<Subscription>(GroupsSub.CurrentDict, GroupsSub.Slice(1L)), zeroWindow.subscriptions())
    }

    // === Окна групп ===

    @Test
    fun `expanded groups - group window per each with positive window`() {
        val state = GroupsTabState(
            dictionaryId = 1L,
            expandedGroupWindows = mapOf(
                5L to GroupWindowState(window = 10, isLoading = false),
                6L to GroupWindowState(window = 20, isLoading = false),
                // Раскрыта пустой — заглушка, подписки нет.
                7L to GroupWindowState(window = 0, isLoading = false),
            ),
        )

        assertEquals(
            setOf(
                GroupsSub.CurrentDict,
                GroupsSub.Slice(dictionaryId = 1L),
                GroupsSub.GroupWindow(groupId = 5L, limit = 10),
                GroupsSub.GroupWindow(groupId = 6L, limit = 20),
            ),
            state.subscriptions(),
        )
    }

    // === Тикер паузы осмысления (Э6) ===

    @Test
    fun `destructive confirm with countdown - delete countdown ticker`() {
        val state = GroupsTabState(
            dictionaryId = 1L,
            confirmDelete = ConfirmDeleteState(
                groupId = 5,
                deleteWords = true,
                countdownLeft = DELETE_COUNTDOWN_SEC,
            ),
        )

        assertEquals(
            setOf(
                GroupsSub.CurrentDict,
                GroupsSub.Slice(dictionaryId = 1L),
                GroupsSub.DeleteCountdown(groupId = 5L),
            ),
            state.subscriptions(),
        )
    }

    @Test
    fun `checkbox off or countdown zero - no ticker`() {
        // Галка снята — конфирм открыт, но тикер не нужен.
        val unchecked = GroupsTabState(
            dictionaryId = 1L,
            confirmDelete = ConfirmDeleteState(groupId = 5, deleteWords = false, countdownLeft = 3),
        )
        assertEquals(setOf<Subscription>(GroupsSub.CurrentDict, GroupsSub.Slice(1L)), unchecked.subscriptions())

        // Счётчик оттикал до нуля — тикер гаснет, кнопка активна.
        val ticked = GroupsTabState(
            dictionaryId = 1L,
            confirmDelete = ConfirmDeleteState(groupId = 5, deleteWords = true, countdownLeft = 0),
        )
        assertEquals(setOf<Subscription>(GroupsSub.CurrentDict, GroupsSub.Slice(1L)), ticked.subscriptions())
    }
}
