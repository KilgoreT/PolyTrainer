package me.apomazkin.groupstab.logic

import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import me.apomazkin.group.DeleteGroupOutcome
import me.apomazkin.group.DeleteGroupWithWordsOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * IS493 | Удаление группы: kebab, единый конфирм, галка «удалить
 * вместе со словами», счётчик паузы осмысления (тики от handler'а),
 * деструктив, guard'ы гонок.
 */
class GroupDeleteReducerTest {
    private val reducer = GroupsTabReducer(logger = NoopLogger)

    private fun baseState(
        groups: List<GroupUiItem> = listOf(GroupUiItem(id = 5, name = "dom", count = 3)),
        confirm: ConfirmDeleteState? = null,
    ) = GroupsTabState(
        isLoading = false,
        dictionaryId = 1L,
        groups = groups,
        visibleGroups = groups,
        confirmDelete = confirm,
    )

    // === Kebab ===

    @Test
    fun `open and dismiss kebab`() {
        val opened = reducer.reduce(baseState(), Msg.OpenKebab(groupId = 5)).state()
        assertEquals(5L, opened.openMenuGroupId)

        val dismissed = reducer.reduce(opened, Msg.DismissKebab).state()
        assertEquals(null, dismissed.openMenuGroupId)
    }

    @Test
    fun `request delete - confirm shown, kebab closed`() {
        val withMenu = baseState().copy(openMenuGroupId = 5L)

        val result = reducer.reduce(withMenu, Msg.RequestDelete(groupId = 5))

        assertEquals(5L, result.state().confirmDelete?.groupId)
        assertEquals(null, result.state().openMenuGroupId)
    }

    // === Галка + счётчик паузы осмысления ===

    @Test
    fun `toggle delete words - on starts countdown, off cancels`() {
        val confirm = baseState(confirm = ConfirmDeleteState(groupId = 5))

        val on = reducer.reduce(confirm, Msg.ToggleDeleteWords)
        val onState = requireNotNull(on.state().confirmDelete)
        assertTrue(onState.deleteWords)
        assertEquals(DELETE_COUNTDOWN_SEC, onState.countdownLeft)
        // Э4: тикер-подписку включит дифф subscriptions(), эффектов нет.
        assertTrue(on.effects().isEmpty())
        assertTrue(
            GroupsSub.DeleteCountdown(groupId = 5) in on.state().subscriptions(),
        )

        val off = reducer.reduce(on.state(), Msg.ToggleDeleteWords)
        val offState = requireNotNull(off.state().confirmDelete)
        assertFalse(offState.deleteWords)
        assertEquals(0, offState.countdownLeft)
        // Снятие галки гасит тикер тем же диффом — эффектов нет.
        assertTrue(off.effects().isEmpty())
        assertTrue(
            off.state().subscriptions().none { it is GroupsSub.DeleteCountdown },
        )
    }

    @Test
    fun `countdown ticks down, guards on tail ticks`() {
        var state = baseState(confirm = ConfirmDeleteState(groupId = 5))
        state = reducer.reduce(state, Msg.ToggleDeleteWords).state()

        repeat(DELETE_COUNTDOWN_SEC) { i ->
            state = reducer.reduce(state, Msg.DeleteCountdownTick).state()
            assertEquals(
                DELETE_COUNTDOWN_SEC - i - 1,
                requireNotNull(state.confirmDelete).countdownLeft,
            )
        }
        // Хвостовой тик после нуля — no-op.
        val tail = reducer.reduce(state, Msg.DeleteCountdownTick)
        assertEquals(state, tail.state())

        // Тик при снятой галке — no-op.
        val unchecked = baseState(confirm = ConfirmDeleteState(groupId = 5))
        assertEquals(unchecked, reducer.reduce(unchecked, Msg.DeleteCountdownTick).state())
    }

    @Test
    fun `toggle delete words guard - no confirm`() {
        val noConfirm = baseState()
        assertEquals(noConfirm, reducer.reduce(noConfirm, Msg.ToggleDeleteWords).state())
    }

    @Test
    fun `reopened confirm - checkbox fresh (unchecked)`() {
        val checked = baseState(
            confirm = ConfirmDeleteState(groupId = 5, deleteWords = true),
        )
        val dismissed = reducer.reduce(checked, Msg.DismissDelete).state()

        val reopened = reducer.reduce(dismissed, Msg.RequestDelete(groupId = 5)).state()

        assertEquals(ConfirmDeleteState(groupId = 5), reopened.confirmDelete)
    }

    // === ConfirmDelete: ветки ===

    @Test
    fun `confirm without checkbox - plain DeleteGroup as before`() {
        val confirm = baseState(confirm = ConfirmDeleteState(groupId = 5))

        val result = reducer.reduce(confirm, Msg.ConfirmDelete)

        assertEquals(null, result.state().confirmDelete)
        assertEquals(
            setOf(GroupsEffect.DeleteGroup(groupId = 5)),
            result.effects(),
        )
    }

    @Test
    fun `confirm while countdown running - no-op (reducer does not trust UI)`() {
        val counting = baseState(
            confirm = ConfirmDeleteState(groupId = 5, deleteWords = true, countdownLeft = 3),
        )

        val result = reducer.reduce(counting, Msg.ConfirmDelete)

        assertEquals(counting, result.state())
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `confirm with checkbox after countdown - destructive DeleteGroupWithWords`() {
        // Единый диалог: счётчик оттикал (countdownLeft=0) → «Удалить
        // всё» шлёт деструктив сразу.
        val confirm = baseState(
            confirm = ConfirmDeleteState(groupId = 5, deleteWords = true, countdownLeft = 0),
        )

        val result = reducer.reduce(confirm, Msg.ConfirmDelete)

        assertEquals(null, result.state().confirmDelete)
        assertEquals(
            setOf(GroupsEffect.DeleteGroupWithWords(groupId = 5)),
            result.effects(),
        )
    }

    @Test
    fun `checkbox with zero count - ignored, plain delete (Mate-3)`() {
        // count упал до 0 под открытым конфирмом с отмеченной галкой.
        val confirm = baseState(
            groups = listOf(GroupUiItem(id = 5, name = "dom", count = 0)),
            confirm = ConfirmDeleteState(groupId = 5, deleteWords = true),
        )

        val result = reducer.reduce(confirm, Msg.ConfirmDelete)

        assertEquals(null, result.state().confirmDelete)
        assertEquals(
            setOf(GroupsEffect.DeleteGroup(groupId = 5)),
            result.effects(),
        )
    }

    // === Dismiss ===

    @Test
    fun `dismiss delete - dialog closed, no delete effect`() {
        val confirm = baseState(confirm = ConfirmDeleteState(groupId = 5))

        val result = reducer.reduce(confirm, Msg.DismissDelete)

        assertEquals(null, result.state().confirmDelete)
        // Э4: тикер (если был) гасит дифф — конфирм ушёл; эффектов нет.
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `dismiss with checkbox on - closed, no DELETE effects`() {
        val checked = baseState(
            confirm = ConfirmDeleteState(groupId = 5, deleteWords = true, countdownLeft = 2),
        )

        val result = reducer.reduce(checked, Msg.DismissDelete)

        assertEquals(null, result.state().confirmDelete)
        // Эффектов НЕТ (удаления — тем более); тикер погаснет диффом.
        assertTrue(result.effects().isEmpty())
        assertTrue(
            result.state().subscriptions().none { it is GroupsSub.DeleteCountdown },
        )
    }

    // === Итоги удалений ===

    @Test
    fun `delete outcome - state untouched (live subscription updates list)`() {
        val current = baseState()

        val success = reducer.reduce(current, Msg.DeleteOutcomeMsg(DeleteGroupOutcome.Success))
        val notFound = reducer.reduce(current, Msg.DeleteOutcomeMsg(DeleteGroupOutcome.NotFound))

        assertEquals(current, success.state())
        assertEquals(current, notFound.state())
    }

    @Test
    fun `delete with words outcome - no-op, subscription redraws`() {
        val current = baseState()

        val success = reducer.reduce(
            current,
            Msg.DeleteWithWordsOutcomeMsg(DeleteGroupWithWordsOutcome.Success(deletedWords = 3)),
        )
        val notFound = reducer.reduce(
            current,
            Msg.DeleteWithWordsOutcomeMsg(DeleteGroupWithWordsOutcome.NotFound),
        )

        assertEquals(current, success.state())
        assertEquals(current, notFound.state())
        assertTrue(success.effects().isEmpty())
    }
}
