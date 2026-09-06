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
 * IS493 | Живые окна раскрытых групп: раскрытие/«Ещё»/эмиссии,
 * реакция на счётчики slice (автооткрытие 0→N, закрытие N→0,
 * компенсация роста, shrink), независимость окон разных групп.
 */
class GroupWindowsReducerTest {

    private val reducer = GroupsTabReducer(logger = NoopLogger)

    private fun term(id: Long) = TermUiItem(
        id = id,
        wordValue = "w$id",
        dictionaryId = 1L,
        addDate = Date(0),
    )

    private fun tree(
        groups: List<Triple<Long, String, Int>>,
        allCount: Int = 0,
    ) = DisplayTree(
        allWords = DisplayNode.AllWords(
            words = (allCount.toLong() downTo 1L).toList(),
            count = allCount,
        ),
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
            GroupUiItem(id = 5, name = "Быт", count = 3),
            GroupUiItem(id = 6, name = "Дом", count = 0),
        ),
        windows: Map<Long, GroupWindowState> = emptyMap(),
    ) = GroupsTabState(
        isLoading = false,
        dictionaryId = 1L,
        groups = groups,
        visibleGroups = groups,
        expandedGroupWindows = windows,
    )

    // === ToggleGroup ===

    @Test
    fun `toggle non-empty group - window opened with chunk, SetGroupWindow`() {
        val result = reducer.reduce(baseState(), Msg.ToggleGroup(groupId = 5))

        val win = requireNotNull(result.state().expandedGroupWindows[5L])
        assertEquals(CHUNK_SIZE, win.window)
        assertTrue(win.isLoading)
        assertEquals(
            setOf(GroupsEffect.SetGroupWindow(groupId = 5, limit = CHUNK_SIZE)),
            result.effects(),
        )
    }

    @Test
    fun `toggle empty group - stub without subscription`() {
        val result = reducer.reduce(baseState(), Msg.ToggleGroup(groupId = 6))

        val win = requireNotNull(result.state().expandedGroupWindows[6L])
        assertEquals(0, win.window)
        assertFalse(win.isLoading)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `toggle expanded group - window closed, subscription off`() {
        val expanded = baseState(
            windows = mapOf(5L to GroupWindowState(window = 10, isLoading = false)),
        )

        val result = reducer.reduce(expanded, Msg.ToggleGroup(groupId = 5))

        assertTrue(result.state().expandedGroupWindows.isEmpty())
        assertEquals(
            setOf(GroupsEffect.SetGroupWindow(groupId = 5, limit = null)),
            result.effects(),
        )
    }

    @Test
    fun `toggle group - expand and collapse (empty stub path)`() {
        // Группы baseState-подобного стейта с count=0 — раскрытие пустой.
        val zeroGroups = listOf(
            GroupUiItem(id = 5, name = "Быт", count = 0),
            GroupUiItem(id = 6, name = "Дом", count = 0),
        )
        val state = baseState(groups = zeroGroups)

        val expanded = reducer.reduce(state, Msg.ToggleGroup(groupId = 6)).state()
        assertEquals(setOf(6L), expanded.expandedGroupWindows.keys)

        val collapsed = reducer.reduce(expanded, Msg.ToggleGroup(groupId = 6)).state()
        assertTrue(collapsed.expandedGroupWindows.isEmpty())
    }

    // === LoadMoreGroup ===

    @Test
    fun `load more group - window widened by chunk`() {
        val expanded = baseState(
            groups = listOf(GroupUiItem(id = 5, name = "Быт", count = 30)),
            windows = mapOf(
                5L to GroupWindowState(
                    window = 10,
                    loadedWords = (10L downTo 1L).map { term(it) },
                    isLoading = false,
                    hasMore = true,
                ),
            ),
        )

        val result = reducer.reduce(expanded, Msg.LoadMoreGroup(groupId = 5))

        val win = requireNotNull(result.state().expandedGroupWindows[5L])
        assertEquals(20, win.window)
        assertTrue(win.isLoading)
        assertEquals(
            setOf(GroupsEffect.SetGroupWindow(groupId = 5, limit = 20)),
            result.effects(),
        )
    }

    @Test
    fun `load more group guards - not expanded, loading, all shown`() {
        val notExpanded = reducer.reduce(baseState(), Msg.LoadMoreGroup(groupId = 5))
        assertEquals(baseState(), notExpanded.state())
        assertTrue(notExpanded.effects().isEmpty())

        val loading = baseState(
            windows = mapOf(5L to GroupWindowState(window = 10, isLoading = true)),
        )
        assertEquals(loading, reducer.reduce(loading, Msg.LoadMoreGroup(groupId = 5)).state())

        // Показано всё — футер прячется принудительно.
        val allShown = baseState(
            groups = listOf(GroupUiItem(id = 5, name = "Быт", count = 2)),
            windows = mapOf(
                5L to GroupWindowState(
                    window = 10,
                    loadedWords = listOf(term(2), term(1)),
                    isLoading = false,
                    hasMore = true,
                ),
            ),
        )
        val result = reducer.reduce(allShown, Msg.LoadMoreGroup(groupId = 5))
        assertFalse(requireNotNull(result.state().expandedGroupWindows[5L]).hasMore)
        assertTrue(result.effects().isEmpty())
    }

    // === GroupWindowLoaded / Failed ===

    @Test
    fun `group window loaded - content, spinner off, hasMore recalculated`() {
        val expanded = baseState(
            windows = mapOf(5L to GroupWindowState(window = 10, isLoading = true)),
        )

        val result = reducer.reduce(
            expanded,
            Msg.GroupWindowLoaded(groupId = 5, words = listOf(term(3), term(2), term(1))),
        )

        val win = requireNotNull(result.state().expandedGroupWindows[5L])
        assertEquals(3, win.loadedWords.size)
        assertFalse(win.isLoading)
        // count группы 5 = 3 → показано всё.
        assertFalse(win.hasMore)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `group window loaded for collapsed group - no-op (race)`() {
        val collapsed = baseState()

        val result = reducer.reduce(
            collapsed,
            Msg.GroupWindowLoaded(groupId = 5, words = listOf(term(1))),
        )

        assertEquals(collapsed, result.state())
    }

    @Test
    fun `group window failed - own spinner off, window alive`() {
        val expanded = baseState(
            windows = mapOf(
                5L to GroupWindowState(window = 10, isLoading = true),
            ),
        )

        val result = reducer.reduce(expanded, Msg.GroupWindowFailed(groupId = 5))

        val win = requireNotNull(result.state().expandedGroupWindows[5L])
        assertFalse(win.isLoading)
        assertEquals(10, win.window)

        // Свёрнутая группа — no-op.
        val collapsed = baseState()
        assertEquals(collapsed, reducer.reduce(collapsed, Msg.GroupWindowFailed(5)).state())
    }

    // === applyGroupCounts через SliceLoaded ===

    @Test
    fun `slice growth under open group window - compensated, order before applyGroups`() {
        // Прежний count=3 в state.groups; окно 10 открыто и заполнено.
        val expanded = baseState(
            windows = mapOf(
                5L to GroupWindowState(
                    window = 10,
                    loadedWords = (3L downTo 1L).map { term(it) },
                    isLoading = false,
                    hasMore = false,
                ),
            ),
        )

        // Slice приносит count=4 (слово добавили из карточки). Если бы
        // applyGroupCounts шёл ПОСЛЕ applyGroups — дельта была бы 0 и
        // окно осталось бы 10 без эффекта (тест на порядок, ревью Mate-3).
        val result = reducer.reduce(
            expanded,
            Msg.SliceLoaded(tree(groups = listOf(Triple(5L, "Быт", 4)), allCount = 4)),
        )

        val win = requireNotNull(result.state().expandedGroupWindows[5L])
        assertEquals(11, win.window)
        assertTrue(
            GroupsEffect.SetGroupWindow(groupId = 5, limit = 11) in result.effects(),
        )
        // Новый счётчик доехал и в groups.
        assertEquals(4, result.state().groups.single { it.id == 5L }.count)
    }

    @Test
    fun `slice count zero under open window - window closes to stub`() {
        val expanded = baseState(
            windows = mapOf(
                5L to GroupWindowState(
                    window = 10,
                    loadedWords = listOf(term(1)),
                    isLoading = false,
                    hasMore = false,
                ),
            ),
        )

        val result = reducer.reduce(
            expanded,
            Msg.SliceLoaded(tree(groups = listOf(Triple(5L, "Быт", 0)))),
        )

        val win = requireNotNull(result.state().expandedGroupWindows[5L])
        assertEquals(0, win.window)
        assertTrue(win.loadedWords.isEmpty())
        assertFalse(win.isLoading)
        assertTrue(
            GroupsEffect.SetGroupWindow(groupId = 5, limit = null) in result.effects(),
        )
    }

    @Test
    fun `slice count grows for group expanded as empty - window auto-opens`() {
        val expandedEmpty = baseState(
            windows = mapOf(6L to GroupWindowState(window = 0, isLoading = false)),
        )

        val result = reducer.reduce(
            expandedEmpty,
            Msg.SliceLoaded(
                tree(groups = listOf(Triple(5L, "Быт", 3), Triple(6L, "Дом", 1)), allCount = 4),
            ),
        )

        val win = requireNotNull(result.state().expandedGroupWindows[6L])
        assertEquals(CHUNK_SIZE, win.window)
        assertTrue(win.isLoading)
        assertTrue(
            GroupsEffect.SetGroupWindow(groupId = 6, limit = CHUNK_SIZE) in result.effects(),
        )
    }

    @Test
    fun `slice shrink under open window - hasMore recalculated, window untouched`() {
        val expanded = baseState(
            groups = listOf(GroupUiItem(id = 5, name = "Быт", count = 3)),
            windows = mapOf(
                5L to GroupWindowState(
                    window = 10,
                    loadedWords = (3L downTo 1L).map { term(it) },
                    isLoading = false,
                    hasMore = false,
                ),
            ),
        )

        // Снятие слова: count 3→2; окно остаётся, hasMore пересчитан
        // (loaded=3 > count=2 → false; живое окно само переэмитит 2).
        val result = reducer.reduce(
            expanded,
            Msg.SliceLoaded(tree(groups = listOf(Triple(5L, "Быт", 2)), allCount = 3)),
        )

        val win = requireNotNull(result.state().expandedGroupWindows[5L])
        assertEquals(10, win.window)
        assertFalse(win.hasMore)
        assertTrue(result.effects().none { it is GroupsEffect.SetGroupWindow })
    }

    // === Независимость окон (ревью Test-4) ===

    @Test
    fun `two expanded groups - load more touches only its own window`() {
        val expanded = baseState(
            groups = listOf(
                GroupUiItem(id = 5, name = "Быт", count = 30),
                GroupUiItem(id = 6, name = "Дом", count = 30),
            ),
            windows = mapOf(
                5L to GroupWindowState(window = 10, isLoading = false, hasMore = true),
                6L to GroupWindowState(window = 10, isLoading = false, hasMore = true),
            ),
        )

        val result = reducer.reduce(expanded, Msg.LoadMoreGroup(groupId = 5))

        assertEquals(20, requireNotNull(result.state().expandedGroupWindows[5L]).window)
        assertEquals(10, requireNotNull(result.state().expandedGroupWindows[6L]).window)
        assertEquals(
            setOf(GroupsEffect.SetGroupWindow(groupId = 5, limit = 20)),
            result.effects(),
        )
    }

    @Test
    fun `two expanded groups - emission addresses only its own window`() {
        val expanded = baseState(
            windows = mapOf(
                5L to GroupWindowState(window = 10, isLoading = true),
                6L to GroupWindowState(window = 10, isLoading = true),
            ),
        )

        val result = reducer.reduce(
            expanded,
            Msg.GroupWindowLoaded(groupId = 5, words = listOf(term(1))),
        )

        assertEquals(1, requireNotNull(result.state().expandedGroupWindows[5L]).loadedWords.size)
        assertFalse(requireNotNull(result.state().expandedGroupWindows[5L]).isLoading)
        // Чужое окно не тронуто.
        assertTrue(requireNotNull(result.state().expandedGroupWindows[6L]).loadedWords.isEmpty())
        assertTrue(requireNotNull(result.state().expandedGroupWindows[6L]).isLoading)
    }
}
