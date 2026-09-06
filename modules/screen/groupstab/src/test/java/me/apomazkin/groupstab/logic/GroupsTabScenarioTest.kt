package me.apomazkin.groupstab.logic

import me.apomazkin.group.DeleteGroupOutcome
import me.apomazkin.group.DisplayNode
import me.apomazkin.group.DisplayTree
import io.github.kilgoret.mate.Effect
import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import me.apomazkin.wordrow.entity.TermUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * IS493 Э3: СЦЕНАРНЫЕ тесты (конвенция юзера, слой 3): цепочка Msg как
 * юзер-флоу; эффекты reducer'а проверяются на каждом шаге, ответы
 * «замоканного handler'а» (outcomes, эмиссии подписок) подаются вручную
 * следующими Msg — до завершения сценария.
 */
class GroupsTabScenarioTest {

    private val reducer = GroupsTabReducer(logger = NoopLogger)

    private var state = GroupsTabState()

    /** Шаг сценария: прогнать Msg, обновить state, вернуть эффекты. */
    private fun send(message: Msg): Set<Effect> {
        val result = reducer.reduce(state, message)
        state = result.state()
        return result.effects()
    }

    private fun tree(
        groups: List<Pair<Long, String>>,
        wordIds: List<Long> = emptyList(),
    ) = DisplayTree(
        allWords = DisplayNode.AllWords(words = wordIds, count = wordIds.size),
        groups = groups.map { (id, name) ->
            DisplayNode.Group(
                id = id,
                name = name,
                children = emptyList(),
                directWords = emptyList(),
                subtreeWordCount = 0,
            )
        },
    )

    private fun treeCounted(
        groups: List<Triple<Long, String, Int>>,
        wordIds: List<Long> = emptyList(),
    ) = DisplayTree(
        allWords = DisplayNode.AllWords(words = wordIds, count = wordIds.size),
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

    private fun term(id: Long) = TermUiItem(
        id = id,
        wordValue = "w$id",
        dictionaryId = 1L,
        addDate = Date(0),
    )

    // === Сценарий 1: создание группы — дубль, исправление, успех ===

    @Test
    fun `scenario - create group with duplicate then fix`() {
        // Вход на вкладку: словарь отрезолвлен.
        val subscribeEffects = send(Msg.DictionaryChanged(1L))
        assertTrue(GroupsEffect.SubscribeSlice(1L) in subscribeEffects)
        assertTrue(state.isLoading)

        // «Handler»: первая эмиссия combine — одна группа «Дом».
        send(Msg.SliceLoaded(tree(groups = listOf(6L to "Дом"))))
        assertFalse(state.isLoading)
        assertEquals(listOf("Дом"), state.groups.map { it.name })

        // FAB → шторка, ввод дубля, submit.
        send(Msg.OpenCreateSheet)
        send(Msg.SheetInputChanged("дом"))
        assertEquals(listOf("Дом"), state.visibleGroups.map { it.name })
        val createEffects = send(Msg.SubmitSheet)
        assertEquals(setOf<Effect>(GroupsEffect.CreateGroup(1L, "дом")), createEffects)
        assertTrue(requireNotNull(state.sheet).isSubmitting)

        // «Handler»: транзакция вернула дубль (outcome → toMutationMsg).
        send(Msg.MutationRejected(GroupSheetError.DUPLICATE))
        val sheetAfterError = requireNotNull(state.sheet)
        assertEquals(GroupSheetError.DUPLICATE, sheetAfterError.error)
        assertEquals("дом", sheetAfterError.input)
        assertFalse(sheetAfterError.isSubmitting)

        // Исправление ввода гасит ошибку; повторный submit — успех.
        send(Msg.SheetInputChanged("Сад"))
        assertEquals(null, requireNotNull(state.sheet).error)
        val retryEffects = send(Msg.SubmitSheet)
        assertEquals(setOf<Effect>(GroupsEffect.CreateGroup(1L, "Сад")), retryEffects)
        send(Msg.MutationApplied)
        assertEquals(null, state.sheet)

        // «Handler»: живая подписка переэмитит список с новой группой.
        send(Msg.SliceLoaded(tree(groups = listOf(6L to "Дом", 7L to "Сад"))))
        assertEquals(listOf("Дом", "Сад"), state.visibleGroups.map { it.name })
    }

    // === Сценарий 2: rename через kebab ===

    @Test
    fun `scenario - rename group via kebab`() {
        send(Msg.DictionaryChanged(1L))
        send(Msg.SliceLoaded(tree(groups = listOf(5L to "Быт", 6L to "Дом"))))

        send(Msg.OpenKebab(6L))
        assertEquals(6L, state.openMenuGroupId)

        send(Msg.OpenRenameSheet(6L))
        assertEquals(null, state.openMenuGroupId)
        assertEquals("Дом", requireNotNull(state.sheet).input)
        // Предзаполненное имя схлопнуло список до самой группы.
        assertEquals(listOf(6L), state.visibleGroups.map { it.id })

        send(Msg.SheetInputChanged("Дача"))
        val renameEffects = send(Msg.SubmitSheet)
        assertEquals(setOf<Effect>(GroupsEffect.RenameGroup(6L, "Дача")), renameEffects)

        send(Msg.MutationApplied)
        assertEquals(null, state.sheet)

        send(Msg.SliceLoaded(tree(groups = listOf(5L to "Быт", 6L to "Дача"))))
        assertEquals(listOf("Быт", "Дача"), state.visibleGroups.map { it.name })
    }

    // === Сценарий 3: удаление раскрытой группы с конфирмом ===

    @Test
    fun `scenario - delete expanded group with confirm`() {
        send(Msg.DictionaryChanged(1L))
        send(Msg.SliceLoaded(tree(groups = listOf(5L to "Быт", 6L to "Дом"))))
        send(Msg.ToggleGroup(6L))
        assertEquals(setOf(6L), state.expandedGroupWindows.keys)

        send(Msg.OpenKebab(6L))
        send(Msg.RequestDelete(6L))
        assertEquals(6L, state.confirmDelete?.groupId)
        assertEquals(null, state.openMenuGroupId)

        val deleteEffects = send(Msg.ConfirmDelete)
        assertEquals(setOf<Effect>(GroupsEffect.DeleteGroup(6L)), deleteEffects)
        assertEquals(null, state.confirmDelete)

        send(Msg.DeleteOutcomeMsg(DeleteGroupOutcome.Success))
        // «Handler»: живая подписка переэмитит без удалённой — раскрытие
        // мёртвой группы вычищается.
        send(Msg.SliceLoaded(tree(groups = listOf(5L to "Быт"))))
        assertEquals(listOf("Быт"), state.visibleGroups.map { it.name })
        assertTrue(state.expandedGroupWindows.isEmpty())
    }

    // === Сценарий 4: окно «Все» — раскрытие, «Ещё», вставка слова ===

    @Test
    fun `scenario - all node window with load more and live insert`() {
        send(Msg.DictionaryChanged(1L))
        send(Msg.SliceLoaded(tree(groups = emptyList(), wordIds = (12L downTo 1L).toList())))
        assertEquals(12, requireNotNull(state.allNode).count)

        // Раскрытие: окно = CHUNK_SIZE.
        val expandEffects = send(Msg.ToggleAll)
        assertEquals(setOf<Effect>(GroupsEffect.SetWindow(CHUNK_SIZE)), expandEffects)
        // «Handler»: эмиссия окна — первые 10.
        send(Msg.WindowLoaded((12L downTo 3L).map { term(it) }))
        val afterFirst = requireNotNull(state.allNode)
        assertEquals(10, afterFirst.loadedWords.size)
        assertTrue(afterFirst.hasMore)

        // «Ещё»: окно шире.
        val moreEffects = send(Msg.LoadMore)
        assertEquals(setOf<Effect>(GroupsEffect.SetWindow(CHUNK_SIZE * 2)), moreEffects)
        send(Msg.WindowLoaded((12L downTo 1L).map { term(it) }))
        val afterMore = requireNotNull(state.allNode)
        assertEquals(12, afterMore.loadedWords.size)
        assertFalse(afterMore.hasMore)

        // Вставка слова: рост count → компенсация окна (+1).
        val widenEffects = send(
            Msg.SliceLoaded(tree(groups = emptyList(), wordIds = (13L downTo 1L).toList()))
        )
        assertEquals(setOf<Effect>(GroupsEffect.SetWindow(CHUNK_SIZE * 2 + 1)), widenEffects)
        // «Handler»: живое окно доэмитило вставленное слово.
        send(Msg.WindowLoaded((13L downTo 1L).map { term(it) }))
        assertEquals(13, requireNotNull(state.allNode).loadedWords.size)
        assertFalse(requireNotNull(state.allNode).hasMore)
    }

    // === Сценарий 5: смена словаря посреди работы ===

    @Test
    fun `scenario - dictionary switch mid-flow resets everything`() {
        send(Msg.DictionaryChanged(1L))
        send(Msg.SliceLoaded(tree(groups = listOf(5L to "Быт"), wordIds = listOf(1L))))
        send(Msg.ToggleAll)
        send(Msg.ToggleGroup(5L))
        send(Msg.OpenCreateSheet)
        send(Msg.SheetInputChanged("Д"))

        val switchEffects = send(Msg.DictionaryChanged(2L))
        assertTrue(GroupsEffect.SubscribeSlice(2L) in switchEffects)
        assertTrue(GroupsEffect.SetWindow(limit = null) in switchEffects)
        assertTrue(state.isLoading)
        assertEquals(null, state.sheet)
        assertEquals(null, state.allNode)
        assertTrue(state.expandedGroupWindows.isEmpty())
        assertEquals(emptyList<GroupUiItem>(), state.groups)

        // Новый словарь приезжает подпиской.
        send(Msg.SliceLoaded(tree(groups = listOf(9L to "Тест"))))
        assertFalse(state.isLoading)
        assertEquals(listOf("Тест"), state.visibleGroups.map { it.name })
    }

    // === Сценарий 6 (Э5): окна ДВУХ групп — независимость + live add ===

    @Test
    fun `scenario - two group windows independent, live insert opens empty group`() {
        send(Msg.DictionaryChanged(1L))
        // Группа 5 с тремя словами, 6 — пустая.
        send(
            Msg.SliceLoaded(
                treeCounted(
                    groups = listOf(Triple(5L, "Быт", 3), Triple(6L, "Дом", 0)),
                    wordIds = listOf(3L, 2L, 1L),
                )
            )
        )

        // Раскрываем обе: 5 — окно, 6 — «пусто» без подписки.
        val openEffects = send(Msg.ToggleGroup(5L))
        assertEquals(
            setOf<Effect>(GroupsEffect.SetGroupWindow(groupId = 5, limit = CHUNK_SIZE)),
            openEffects,
        )
        assertTrue(send(Msg.ToggleGroup(6L)).isEmpty())

        // «Handler»: эмиссия окна группы 5 — чужого окна не касается.
        send(Msg.GroupWindowLoaded(5L, listOf(term(3), term(2), term(1))))
        assertEquals(3, requireNotNull(state.expandedGroupWindows[5L]).loadedWords.size)
        assertEquals(0, requireNotNull(state.expandedGroupWindows[6L]).window)

        // Live add: слово попало в ПУСТУЮ раскрытую группу 6 — окно
        // автооткрывается (D21.1), окно группы 5 растёт на дельту? нет —
        // её count не менялся.
        val sliceEffects = send(
            Msg.SliceLoaded(
                treeCounted(
                    groups = listOf(Triple(5L, "Быт", 3), Triple(6L, "Дом", 1)),
                    wordIds = listOf(4L, 3L, 2L, 1L),
                )
            )
        )
        assertTrue(
            GroupsEffect.SetGroupWindow(groupId = 6, limit = CHUNK_SIZE) in sliceEffects,
        )
        assertTrue(sliceEffects.none { it == GroupsEffect.SetGroupWindow(groupId = 5, limit = CHUNK_SIZE + 3) })
        assertEquals(CHUNK_SIZE, requireNotNull(state.expandedGroupWindows[6L]).window)

        // «Handler»: окно 6 доэмитило контент.
        send(Msg.GroupWindowLoaded(6L, listOf(term(4))))
        val win6 = requireNotNull(state.expandedGroupWindows[6L])
        assertEquals(1, win6.loadedWords.size)
        assertFalse(win6.hasMore)

        // Снятие последнего слова из 6 под открытым окном — окно
        // закрывается в заглушку «пусто».
        val shrinkEffects = send(
            Msg.SliceLoaded(
                treeCounted(
                    groups = listOf(Triple(5L, "Быт", 3), Triple(6L, "Дом", 0)),
                    wordIds = listOf(4L, 3L, 2L, 1L),
                )
            )
        )
        assertTrue(
            GroupsEffect.SetGroupWindow(groupId = 6, limit = null) in shrinkEffects,
        )
        assertEquals(0, requireNotNull(state.expandedGroupWindows[6L]).window)
        // Окно группы 5 всё это пережило нетронутым.
        assertEquals(3, requireNotNull(state.expandedGroupWindows[5L]).loadedWords.size)
    }

    // === Сценарий 7 (Э6): деструктивное удаление и откат ===

    @Test
    fun `scenario - destructive flow and cancel flow`() {
        send(Msg.DictionaryChanged(1L))
        send(Msg.SliceLoaded(treeCounted(groups = listOf(Triple(5L, "dom", 3)))))

        // Негативный: галка → счётчик → dismiss → повторное открытие
        // со снятой галкой, ничего не удалено.
        send(Msg.RequestDelete(groupId = 5))
        send(Msg.ToggleDeleteWords)
        // Подтверждение ВО ВРЕМЯ счётчика — no-op.
        assertTrue(send(Msg.ConfirmDelete).isEmpty())
        assertEquals(
            setOf<Effect>(GroupsEffect.CancelDeleteCountdown),
            send(Msg.DismissDelete),
        )
        assertEquals(null, state.confirmDelete)
        send(Msg.RequestDelete(groupId = 5))
        assertEquals(ConfirmDeleteState(groupId = 5), state.confirmDelete)

        // Деструктив: галка → тики доехали («handler») → «Удалить всё».
        send(Msg.ToggleDeleteWords)
        repeat(DELETE_COUNTDOWN_SEC) { send(Msg.DeleteCountdownTick) }
        val destroyEffects = send(Msg.ConfirmDelete)
        assertEquals(
            setOf<Effect>(GroupsEffect.DeleteGroupWithWords(groupId = 5)),
            destroyEffects,
        )
        // «Handler»: подписка перерисовала без группы.
        send(
            Msg.DeleteWithWordsOutcomeMsg(
                me.apomazkin.group.DeleteGroupWithWordsOutcome.Success(3),
            )
        )
        send(Msg.SliceLoaded(treeCounted(groups = emptyList())))
        assertTrue(state.groups.isEmpty())
        assertEquals(null, state.confirmDelete)
    }
}
