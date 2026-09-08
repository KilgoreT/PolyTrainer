package me.apomazkin.groupstab.logic

import me.apomazkin.group.DisplayNode
import me.apomazkin.group.DisplayTree
import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import me.apomazkin.wordrow.entity.TermUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * IS493 | Жизненный цикл вкладки «Группы»: смена/пропажа словаря
 * (полные сбросы, переподписки), структура из slice (группы, фильтр,
 * чистка мёртвых раскрытий/конфирма), ошибки подписки slice.
 */
class GroupsTabLifecycleReducerTest {

    private val reducer = GroupsTabReducer(logger = NoopLogger)

    private fun term(id: Long) = TermUiItem(
        id = id,
        wordValue = "w$id",
        dictionaryId = 1L,
        addDate = Date(0),
    )

    private fun stateWithNode(node: AllNodeState) = GroupsTabState(
        isLoading = false,
        dictionaryId = 1L,
        allNode = node,
    )

    private fun groupsTree(vararg names: Pair<Long, String>) = DisplayTree(
        allWords = DisplayNode.AllWords(words = emptyList(), count = 0),
        groups = names.map { (id, name) ->
            DisplayNode.Group(
                id = id,
                name = name,
                children = emptyList(),
                directWords = emptyList(),
                subtreeWordCount = 0,
            )
        },
    )

    private fun countedTree(groups: List<Triple<Long, String, Int>>) = DisplayTree(
        allWords = DisplayNode.AllWords(words = emptyList(), count = 0),
        groups = groups.map { (id, name, count) ->
            DisplayNode.Group(
                id = id,
                name = name,
                children = emptyList(),
                directWords = (1..count).map { it.toLong() },
                subtreeWordCount = count,
            )
        },
    )

    private fun baseState(
        groups: List<GroupUiItem> = listOf(
            GroupUiItem(id = 5, name = "Быт", count = 0),
            GroupUiItem(id = 6, name = "Дом", count = 0),
        ),
    ) = GroupsTabState(
        isLoading = false,
        dictionaryId = 1L,
        groups = groups,
        visibleGroups = groups,
    )

    // === Словарь ===

    @Test
    fun `initial state - loading until dictionary arrives`() {
        val initial = GroupsTabState()
        assertTrue(initial.isLoading)
        assertFalse(initial.hasNoDictionary)
        assertEquals(null, initial.allNode)
    }

    @Test
    fun `dictionary changed - loading on, no effects (slice via subscriptions diff)`() {
        val result = reducer.reduce(GroupsTabState(), Msg.DictionaryChanged(1L))

        assertTrue(result.state().isLoading)
        assertEquals(1L, result.state().dictionaryId)
        // Э4: подписку Slice нового словаря включит дифф subscriptions().
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `dictionary changed to null - honest empty state, no effects`() {
        val result = reducer.reduce(GroupsTabState(), Msg.DictionaryChanged(null))

        assertFalse(result.state().isLoading)
        assertTrue(result.state().hasNoDictionary)
        // Э4: dictionaryId=null убирает всё из subscriptions() — подписки
        // (slice/окна/тикер) погасит дифф, эффектов не нужно.
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `dictionary changed to same id - no-op`() {
        val current = stateWithNode(AllNodeState(count = 3))

        val result = reducer.reduce(current, Msg.DictionaryChanged(1L))

        assertEquals(current, result.state())
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `dictionary switched under expanded node - full reset, no effects`() {
        val current = stateWithNode(
            AllNodeState(
                count = 3,
                isExpanded = true,
                window = 50,
                loadedWords = listOf(term(3), term(2)),
            )
        )

        val result = reducer.reduce(current, Msg.DictionaryChanged(2L))

        assertTrue(result.state().isLoading)
        assertEquals(2L, result.state().dictionaryId)
        assertEquals(null, result.state().allNode)
        // Э4: переподписку (старый slice гаснет, новый включается) и
        // гашение окна делает дифф subscriptions() по итоговому state.
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `dictionary switch - sheet confirm filter kebab expand reset`() {
        val busy = baseState().copy(
            sheet = GroupSheetState(mode = GroupSheetMode.Create, input = "Д"),
            confirmDelete = ConfirmDeleteState(groupId = 5L),
            openMenuGroupId = 6L,
            expandedGroupWindows = mapOf(5L to GroupWindowState(window = 0, isLoading = false)),
        )

        val result = reducer.reduce(busy, Msg.DictionaryChanged(2L))

        val state = result.state()
        assertEquals(null, state.sheet)
        assertEquals(null, state.confirmDelete)
        assertEquals(null, state.openMenuGroupId)
        assertTrue(state.expandedGroupWindows.isEmpty())
        assertEquals(emptyList<GroupUiItem>(), state.groups)
        assertEquals(emptyList<GroupUiItem>(), state.visibleGroups)
    }

    @Test
    fun `dictionary switch - all group windows closed, subscriptions collapse to slice`() {
        val expanded = baseState().copy(
            expandedGroupWindows = mapOf(
                5L to GroupWindowState(window = 10, isLoading = false),
                6L to GroupWindowState(window = 0, isLoading = false),
            ),
        )

        val result = reducer.reduce(expanded, Msg.DictionaryChanged(2L))

        assertTrue(result.state().expandedGroupWindows.isEmpty())
        // Э4: подписки окон гаснут диффом — в наборе остаётся только
        // slice нового словаря.
        assertEquals(
            setOf(GroupsSub.Slice(dictionaryId = 2L)),
            result.state().subscriptions(),
        )
    }

    // === Структура из slice ===

    @Test
    fun `slice loaded - groups from tree, sorted order preserved`() {
        val result = reducer.reduce(
            GroupsTabState(isLoading = true, dictionaryId = 1L),
            Msg.SliceLoaded(groupsTree(5L to "Быт", 6L to "Дом")),
        )

        assertEquals(listOf("Быт", "Дом"), result.state().groups.map { it.name })
        assertEquals(listOf("Быт", "Дом"), result.state().visibleGroups.map { it.name })
    }

    @Test
    fun `slice loaded with active filter - filter applied to new list`() {
        val opened = reducer.reduce(baseState(), Msg.OpenCreateSheet).state()
        val filtered = reducer.reduce(opened, Msg.SheetInputChanged("Д")).state()

        val result = reducer.reduce(
            filtered,
            Msg.SliceLoaded(groupsTree(5L to "Быт", 6L to "Дом", 7L to "Дача")),
        )

        assertEquals(listOf("Дом", "Дача"), result.state().visibleGroups.map { it.name })
    }

    @Test
    fun `slice loaded - dead ids purged from expanded windows`() {
        val expanded = baseState().copy(
            expandedGroupWindows = mapOf(
                5L to GroupWindowState(window = 0, isLoading = false),
                6L to GroupWindowState(window = 0, isLoading = false),
            ),
        )

        val result = reducer.reduce(
            expanded,
            Msg.SliceLoaded(groupsTree(5L to "Быт")),
        )

        assertEquals(setOf(5L), result.state().expandedGroupWindows.keys)
    }

    @Test
    fun `dead expanded group - window purged, subscription gone from diff`() {
        val expanded = baseState().copy(
            expandedGroupWindows = mapOf(
                5L to GroupWindowState(window = 10, isLoading = false),
                6L to GroupWindowState(window = 0, isLoading = false),
            ),
        )

        // Группа 5 умерла (нет в slice), 6 жива.
        val result = reducer.reduce(
            expanded,
            Msg.SliceLoaded(countedTree(listOf(Triple(6L, "Дом", 0)))),
        )

        assertEquals(setOf(6L), result.state().expandedGroupWindows.keys)
        // Э4: окно мёртвой группы ушло из state → его подписку погасит дифф.
        assertTrue(
            result.state().subscriptions().none { it is GroupsSub.GroupWindow },
        )
    }

    @Test
    fun `group died under confirm - confirm closed by slice`() {
        val confirm = baseState(
            groups = listOf(GroupUiItem(id = 5, name = "dom", count = 3)),
        ).copy(
            confirmDelete = ConfirmDeleteState(groupId = 5, deleteWords = true, countdownLeft = 3),
        )

        // Slice без группы 5 — умерла (удалена с другого пути).
        val result = reducer.reduce(
            confirm,
            Msg.SliceLoaded(countedTree(listOf(Triple(6L, "byt", 0)))),
        )

        assertEquals(null, result.state().confirmDelete)
    }

    @Test
    fun `group alive under confirm - confirm survives slice`() {
        val confirm = baseState(
            groups = listOf(GroupUiItem(id = 5, name = "dom", count = 3)),
        ).copy(confirmDelete = ConfirmDeleteState(groupId = 5))

        val result = reducer.reduce(
            confirm,
            Msg.SliceLoaded(countedTree(listOf(Triple(5L, "dom", 3)))),
        )

        assertEquals(ConfirmDeleteState(groupId = 5), result.state().confirmDelete)
    }

    // === Ошибка подписки (T-6) ===

    @Test
    fun `slice load failed - loading off`() {
        val result = reducer.reduce(
            GroupsTabState(isLoading = true, dictionaryId = 1L),
            Msg.SliceLoadFailed,
        )

        assertFalse(result.state().isLoading)
    }
}
