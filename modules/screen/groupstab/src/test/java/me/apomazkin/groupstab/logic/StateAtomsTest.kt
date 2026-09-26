package me.apomazkin.groupstab.logic

import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import me.apomazkin.wordrow.entity.TermUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * IS493 Э3: тесты АТОМАРНЫХ state-экстеншнов (конвенция юзера: слой 1 —
 * экстеншны, слой 2 — message-тесты reducer'а, слой 3 — сценарии).
 * Атом = один технический шаг; каждый возвращает ReducerResult —
 * проверяются и state, и рождённые атомом эффекты.
 *
 * Атомы — member-экстеншны [StateAtoms] (логгер через dispatch
 * receiver), поэтому тест НАСЛЕДУЕТ класс атомов с [NoopLogger].
 */
class StateAtomsTest : StateAtoms(NoopLogger) {
    private fun term(id: Long) =
        TermUiItem(
            id = id,
            wordValue = "w$id",
            dictionaryId = 1L,
            addDate = Date(0),
        )

    private val groups = listOf(
        GroupUiItem(id = 5, name = "Быт", count = 0),
        GroupUiItem(id = 6, name = "Дом", count = 0),
    )

    // === Загрузка / словарь ===

    @Test
    fun `showLoading and hideLoading toggle only isLoading, no effects`() {
        val shown = GroupsTabState(isLoading = false).showLoading()
        assertTrue(shown.state().isLoading)
        assertTrue(shown.effects().isEmpty())

        val hidden = GroupsTabState(isLoading = true).hideLoading()
        assertFalse(hidden.state().isLoading)
    }

    @Test
    fun `markNoDictionary - only flag, other fields untouched`() {
        val result = GroupsTabState(isLoading = true, dictionaryId = 1L).markNoDictionary()

        assertTrue(result.state().hasNoDictionary)
        // Гашение загрузки и сброс словаря — отдельные атомы цепочки.
        assertTrue(result.state().isLoading)
        assertEquals(1L, result.state().dictionaryId)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `selectDictionary, clearDictionary and markDictionaryPresent`() {
        val selected = GroupsTabState(hasNoDictionary = true)
            .selectDictionary(2L)
            .state()
            .markDictionaryPresent()
            .state()

        assertEquals(2L, selected.dictionaryId)
        assertFalse(selected.hasNoDictionary)

        assertEquals(null, selected.clearDictionary().state().dictionaryId)
    }

    // === Узел «Все»: applyAllCount / widenWindowForGrowth ===

    @Test
    fun `applyAllCount without node - creates collapsed node, no effects`() {
        val result = GroupsTabState().applyAllCount(count = 3)

        val node = requireNotNull(result.state().allNode)
        assertEquals(3, node.count)
        assertFalse(node.isExpanded)
        assertTrue(node.hasMore)
        assertTrue(result.effects().isEmpty())

        assertFalse(
            requireNotNull(GroupsTabState().applyAllCount(0).state().allNode).hasMore,
        )
    }

    @Test
    fun `applyAllCount on existing node - count and hasMore recalculated, window untouched`() {
        val result = GroupsTabState(
            allNode = AllNodeState(count = 2, window = 10, loadedWords = listOf(term(1), term(2))),
        ).applyAllCount(count = 3)

        val node = requireNotNull(result.state().allNode)
        assertEquals(3, node.count)
        assertTrue(node.hasMore)
        assertEquals(10, node.window)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `widenWindowForGrowth under expanded window - widened, no effects`() {
        val result = GroupsTabState(
            allNode = AllNodeState(
                count = 12,
                isExpanded = true,
                window = 10,
                loadedWords = (12L downTo 3L).map { term(it) },
            ),
        ).widenWindowForGrowth(newCount = 13)

        val node = requireNotNull(result.state().allNode)
        assertEquals(11, node.window)
        // Э4: новое окно в state → подписку перезапустит дифф subscriptions().
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `widenWindowForGrowth no-ops - collapsed, closed window, no growth`() {
        val collapsed = GroupsTabState(allNode = AllNodeState(count = 3, isExpanded = false))
        assertEquals(collapsed.allNode, collapsed.widenWindowForGrowth(4).state().allNode)
        assertTrue(collapsed.widenWindowForGrowth(4).effects().isEmpty())

        val noWindow = GroupsTabState(allNode = AllNodeState(count = 3, isExpanded = true))
        assertEquals(noWindow.allNode, noWindow.widenWindowForGrowth(4).state().allNode)

        val shrunk = GroupsTabState(
            allNode = AllNodeState(count = 3, isExpanded = true, window = 10),
        )
        assertEquals(shrunk.allNode, shrunk.widenWindowForGrowth(2).state().allNode)
    }

    // === Узел «Все»: раскрытие / окно ===

    @Test
    fun `expandAllNode and collapseAllNode - only flag, no effects`() {
        val expanded = GroupsTabState(allNode = AllNodeState(count = 3)).expandAllNode()
        assertTrue(requireNotNull(expanded.state().allNode).isExpanded)
        assertTrue(expanded.effects().isEmpty())

        val collapsed = expanded.state().collapseAllNode()
        assertFalse(requireNotNull(collapsed.state().allNode).isExpanded)
    }

    @Test
    fun `openWindow - window, spinner, no effects`() {
        val result = GroupsTabState(
            allNode = AllNodeState(count = 12, isExpanded = true, hasMore = true),
        ).openWindow(limit = 10)

        val node = requireNotNull(result.state().allNode)
        assertEquals(10, node.window)
        assertTrue(node.isWindowLoading)
        // Э4: подписку окна включит дифф subscriptions().
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `closeWindow - window dropped, content cleared, subscription off`() {
        val result = GroupsTabState(
            allNode = AllNodeState(
                count = 3,
                isExpanded = false,
                window = 20,
                loadedWords = listOf(term(1)),
                isWindowLoading = true,
            ),
        ).closeWindow()

        val node = requireNotNull(result.state().allNode)
        assertEquals(0, node.window)
        assertTrue(node.loadedWords.isEmpty())
        assertFalse(node.isWindowLoading)
        // Э4: window=0 убирает AllWindow из subscriptions() — дифф погасит.
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `widenWindowBy - step added, spinner, no effects`() {
        val result = GroupsTabState(
            allNode = AllNodeState(count = 30, window = 10, isExpanded = true),
        ).widenWindowBy(step = 10)

        val node = requireNotNull(result.state().allNode)
        assertEquals(20, node.window)
        assertTrue(node.isWindowLoading)
        // Э4: новый лимит перезапустит подписку диффом subscriptions().
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `applyWindowWords - only content replaced, no effects`() {
        val result = GroupsTabState(
            allNode = AllNodeState(count = 3, isExpanded = true, isWindowLoading = true),
        ).applyWindowWords(listOf(term(1), term(2), term(3)))

        val node = requireNotNull(result.state().allNode)
        assertEquals(3, node.loadedWords.size)
        // Спиннер и hasMore — отдельные атомы цепочки.
        assertTrue(node.isWindowLoading)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `recalcHasMore - against count, empty content means count check`() {
        val partial = GroupsTabState(
            allNode = AllNodeState(count = 3, loadedWords = listOf(term(1))),
        ).recalcHasMore()
        assertTrue(requireNotNull(partial.state().allNode).hasMore)

        val full = GroupsTabState(
            allNode = AllNodeState(count = 1, loadedWords = listOf(term(1)), hasMore = true),
        ).recalcHasMore()
        assertFalse(requireNotNull(full.state().allNode).hasMore)

        // После closeWindow контент пуст: hasMore = count > 0.
        val closed = GroupsTabState(
            allNode = AllNodeState(count = 3),
        ).recalcHasMore()
        assertTrue(requireNotNull(closed.state().allNode).hasMore)
    }

    @Test
    fun `markNoMore and hideWindowLoading`() {
        val noMore = GroupsTabState(
            allNode = AllNodeState(count = 3, hasMore = true),
        ).markNoMore()
        assertFalse(requireNotNull(noMore.state().allNode).hasMore)

        val spinnerOff = GroupsTabState(
            allNode = AllNodeState(count = 3, isWindowLoading = true),
        ).hideWindowLoading()
        assertFalse(requireNotNull(spinnerOff.state().allNode).isWindowLoading)
    }

    @Test
    fun `node extensions are no-op without node`() {
        val empty = GroupsTabState()

        assertEquals(empty, empty.expandAllNode().state())
        assertEquals(empty, empty.collapseAllNode().state())
        assertEquals(empty, empty.openWindow(10).state())
        assertTrue(empty.openWindow(10).effects().isEmpty())
        assertEquals(empty, empty.closeWindow().state())
        assertTrue(empty.closeWindow().effects().isEmpty())
        assertEquals(empty, empty.widenWindowBy(10).state())
        assertEquals(empty, empty.widenWindowForGrowth(5).state())
        assertEquals(empty, empty.applyWindowWords(listOf(term(1))).state())
        assertEquals(empty, empty.recalcHasMore().state())
        assertEquals(empty, empty.markNoMore().state())
    }

    // === Группы ===

    @Test
    fun `applyGroups - only groups replaced, no effects`() {
        val windows = mapOf(
            5L to GroupWindowState(window = 0, isLoading = false),
            99L to GroupWindowState(window = 0, isLoading = false),
        )
        val result = GroupsTabState(expandedGroupWindows = windows).applyGroups(groups)

        assertEquals(groups, result.state().groups)
        // Видимые и чистка раскрытых — отдельные атомы цепочки.
        assertEquals(emptyList<GroupUiItem>(), result.state().visibleGroups)
        assertEquals(windows, result.state().expandedGroupWindows)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `purgeDeadExpanded - dead windows closed, no effects`() {
        val result = GroupsTabState(
            groups = groups,
            expandedGroupWindows = mapOf(
                5L to GroupWindowState(window = 0, isLoading = false),
                99L to GroupWindowState(window = 10, isLoading = false),
            ),
        ).purgeDeadExpanded()

        assertEquals(setOf(5L), result.state().expandedGroupWindows.keys)
        // Подписку мёртвой группы погасит дифф subscriptions() (Э4).
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `refreshVisibleGroups - full list without sheet, filtered with input`() {
        val noSheet = GroupsTabState(groups = groups).refreshVisibleGroups()
        assertEquals(groups, noSheet.state().visibleGroups)

        val filtered = GroupsTabState(
            groups = groups,
            sheet = GroupSheetState(mode = GroupSheetMode.Create, input = "дО"),
        ).refreshVisibleGroups()
        assertEquals(listOf(6L), filtered.state().visibleGroups.map { it.id })

        val blankInput = GroupsTabState(
            groups = groups,
            sheet = GroupSheetState(mode = GroupSheetMode.Create, input = "  "),
        ).refreshVisibleGroups()
        assertEquals(groups, blankInput.state().visibleGroups)
    }

    @Test
    fun `clearGroups and collapseAllGroups`() {
        val cleared = GroupsTabState(
            groups = groups,
            expandedGroupWindows = mapOf(5L to GroupWindowState(window = 0, isLoading = false)),
        ).clearGroups()
            .state()
            .collapseAllGroups()
            .state()

        assertEquals(emptyList<GroupUiItem>(), cleared.groups)
        assertTrue(cleared.expandedGroupWindows.isEmpty())
    }

    @Test
    fun `expandGroupEmpty and closeGroupWindow`() {
        val expanded = GroupsTabState().expandGroupEmpty(5L)
        val win = requireNotNull(expanded.state().expandedGroupWindows[5L])
        assertEquals(0, win.window)
        assertFalse(win.isLoading)
        assertTrue(expanded.effects().isEmpty())

        val closed = expanded.state().closeGroupWindow(5L)
        assertTrue(closed.state().expandedGroupWindows.isEmpty())
        // Э4: ключ ушёл из карты → подписку (если была) погасит дифф.
        assertTrue(closed.effects().isEmpty())
    }

    @Test
    fun `openGroupWindow - window, spinner, no effects`() {
        val result = GroupsTabState().openGroupWindow(id = 5L, limit = 10)

        val win = requireNotNull(result.state().expandedGroupWindows[5L])
        assertEquals(10, win.window)
        assertTrue(win.isLoading)
        // Э4: подписку окна группы включит дифф subscriptions().
        assertTrue(result.effects().isEmpty())
    }

    // === Шторка ===

    @Test
    fun `openCreateSheet - only sheet, empty input`() {
        val result = GroupsTabState(groups = groups).openCreateSheet()

        val sheet = requireNotNull(result.state().sheet)
        assertEquals(GroupSheetMode.Create, sheet.mode)
        assertEquals("", sheet.input)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `openRenameSheet - only sheet, input prefilled`() {
        val result = GroupsTabState(groups = groups).openRenameSheet(6L, "Дом")

        val sheet = requireNotNull(result.state().sheet)
        assertEquals(GroupSheetMode.Rename(groupId = 6L), sheet.mode)
        assertEquals("Дом", sheet.input)
    }

    @Test
    fun `closeSheet - only sheet dropped`() {
        val opened = GroupsTabState(groups = groups)
            .openCreateSheet()
            .state()

        assertEquals(null, opened.closeSheet().state().sheet)
    }

    @Test
    fun `updateSheetInput and clearSheetError - separate steps`() {
        val withError = GroupsTabState(
            sheet = GroupSheetState(
                mode = GroupSheetMode.Create,
                error = GroupSheetError.DUPLICATE,
            ),
        )

        val typed = withError.updateSheetInput("б")
        val sheet = requireNotNull(typed.state().sheet)
        assertEquals("б", sheet.input)
        // Гашение ошибки — отдельный атом.
        assertEquals(GroupSheetError.DUPLICATE, sheet.error)

        val cleared = typed.state().clearSheetError()
        assertEquals(null, requireNotNull(cleared.state().sheet).error)
    }

    @Test
    fun `showSheetError - only error, submitting untouched`() {
        val result = GroupsTabState(
            sheet = GroupSheetState(mode = GroupSheetMode.Create, isSubmitting = true),
        ).showSheetError(GroupSheetError.RESERVED)

        val sheet = requireNotNull(result.state().sheet)
        assertEquals(GroupSheetError.RESERVED, sheet.error)
        // Разблокировка submit — отдельный атом (unmarkSubmitting).
        assertTrue(sheet.isSubmitting)
        assertEquals(null, result.state().errorSnackbar)
    }

    @Test
    fun `markSubmitting and unmarkSubmitting`() {
        val submitting = GroupsTabState(
            sheet = GroupSheetState(mode = GroupSheetMode.Create),
        ).markSubmitting()
        assertTrue(requireNotNull(submitting.state().sheet).isSubmitting)

        val unmarked = submitting.state().unmarkSubmitting()
        assertFalse(requireNotNull(unmarked.state().sheet).isSubmitting)
    }

    @Test
    fun `sheet extensions are no-op without sheet`() {
        val empty = GroupsTabState()

        assertEquals(empty, empty.updateSheetInput("x").state())
        assertEquals(empty, empty.clearSheetError().state())
        assertEquals(empty, empty.showSheetError(GroupSheetError.EMPTY).state())
        assertEquals(empty, empty.markSubmitting().state())
        assertEquals(empty, empty.unmarkSubmitting().state())
    }

    @Test
    fun `error snackbar - show and consume`() {
        val shown = GroupsTabState().showErrorSnackbar(GroupSheetError.DUPLICATE)
        assertEquals(GroupSheetError.DUPLICATE, shown.state().errorSnackbar)
        assertTrue(shown.effects().isEmpty())

        assertEquals(
            null,
            shown
                .state()
                .consumeErrorSnackbar()
                .state()
                .errorSnackbar,
        )
    }

    // === Конфирм / кебаб ===

    @Test
    fun `delete confirm - ask and close`() {
        val asked = GroupsTabState().askDeleteConfirm(5L).state()
        assertEquals(ConfirmDeleteState(groupId = 5L), asked.confirmDelete)
        assertEquals(null, asked.closeDeleteConfirm().state().confirmDelete)
    }

    @Test
    fun `kebab - open and close`() {
        val opened = GroupsTabState().openKebab(6L).state()
        assertEquals(6L, opened.openMenuGroupId)
        assertEquals(null, opened.closeKebab().state().openMenuGroupId)
    }
}
