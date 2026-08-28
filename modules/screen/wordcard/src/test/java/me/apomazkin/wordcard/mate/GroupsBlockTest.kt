package me.apomazkin.wordcard.mate

import me.apomazkin.group.AddMembershipOutcome
import me.apomazkin.group.RemoveMembershipOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * IS493 Э5 (stage5_plan Фаза 3): блок групп карточки — пикер, галочки,
 * keyed in-flight, подписки, guard'ы экрана, flush-on-back, мапперы.
 */
class GroupsBlockTest {

    private val reducer = WordCardReducer(NoopLogger)

    private fun loadedState(
        block: GroupsBlockState = GroupsBlockState(),
    ) = WordCardState(
        isLoading = false,
        wordState = WordState.Loaded(
            id = 3L,
            dictionaryId = 1L,
            added = Date(0),
            value = "cat",
        ),
        groupsBlock = block,
    )

    private val dictGroups = listOf(GroupUi(5, "Быт"), GroupUi(6, "Дом"))

    // === Пикер ===

    @Test
    fun `open and dismiss picker`() {
        val opened = reducer.reduce(loadedState(), Msg.OpenGroupPicker)
        assertTrue(opened.first.groupsBlock.isPickerOpen)
        assertTrue(opened.second.isEmpty())

        val dismissed = reducer.reduce(opened.first, Msg.DismissGroupPicker)
        assertFalse(dismissed.first.groupsBlock.isPickerOpen)
    }

    // === Подписки ===

    @Test
    fun `subscriptions - dict groups and word memberships land in state`() {
        val withDict = reducer.reduce(loadedState(), Msg.DictGroupsLoaded(dictGroups)).first
        assertEquals(dictGroups, withDict.groupsBlock.dictGroups)

        val withWord = reducer.reduce(withDict, Msg.WordGroupsLoaded(setOf(6L))).first
        assertEquals(setOf(6L), withWord.groupsBlock.wordGroupIds)
        // Чипы — derived: пересечение по алфавиту dictGroups.
        assertEquals(listOf(GroupUi(6, "Дом")), withWord.groupsBlock.chips)
    }

    @Test
    fun `chips appear only after BOTH subscriptions arrive (order race)`() {
        // WordGroupsLoaded ДО DictGroupsLoaded (ревью Test-1).
        val onlyWord = reducer.reduce(loadedState(), Msg.WordGroupsLoaded(setOf(6L))).first
        assertTrue(onlyWord.groupsBlock.chips.isEmpty())

        val both = reducer.reduce(onlyWord, Msg.DictGroupsLoaded(dictGroups)).first
        assertEquals(listOf(GroupUi(6, "Дом")), both.groupsBlock.chips)
    }

    // === Toggle: направление и in-flight ===

    @Test
    fun `toggle non-member - add effect, in-flight marked`() {
        val state = loadedState(
            GroupsBlockState(dictGroups = dictGroups, isPickerOpen = true),
        )

        val result = reducer.reduce(state, Msg.ToggleGroupMembership(groupId = 5))

        assertEquals(setOf(5L), result.first.groupsBlock.inFlight)
        assertEquals(
            setOf<me.apomazkin.mate.Effect>(
                DatasourceEffect.AddMembership(wordId = 3, groupId = 5),
            ),
            result.second,
        )
    }

    @Test
    fun `toggle member - remove effect (direction from db fact)`() {
        val state = loadedState(
            GroupsBlockState(
                dictGroups = dictGroups,
                wordGroupIds = setOf(5L),
                isPickerOpen = true,
            ),
        )

        val result = reducer.reduce(state, Msg.ToggleGroupMembership(groupId = 5))

        assertEquals(
            setOf<me.apomazkin.mate.Effect>(
                DatasourceEffect.RemoveMembership(wordId = 3, groupId = 5),
            ),
            result.second,
        )
    }

    @Test
    fun `toggle spam while in-flight - no-op`() {
        val state = loadedState(
            GroupsBlockState(dictGroups = dictGroups, inFlight = setOf(5L)),
        )

        val result = reducer.reduce(state, Msg.ToggleGroupMembership(groupId = 5))

        assertEquals(state, result.first)
        assertTrue(result.second.isEmpty())
    }

    @Test
    fun `membership done and failed - in-flight cleared`() {
        val inFlight = loadedState(GroupsBlockState(inFlight = setOf(5L, 6L)))

        val done = reducer.reduce(inFlight, Msg.MembershipDone(groupId = 5)).first
        assertEquals(setOf(6L), done.groupsBlock.inFlight)

        val failed = reducer.reduce(done, Msg.MembershipFailed(groupId = 6)).first
        assertTrue(failed.groupsBlock.inFlight.isEmpty())
    }

    @Test
    fun `wordGroupIds changed ONLY by subscription - done does not touch it`() {
        // D22.3: оптимистичных правок нет.
        val inFlight = loadedState(
            GroupsBlockState(dictGroups = dictGroups, inFlight = setOf(5L)),
        )

        val done = reducer.reduce(inFlight, Msg.MembershipDone(groupId = 5)).first

        assertTrue(done.groupsBlock.wordGroupIds.isEmpty())
    }

    @Test
    fun `done after dismiss - in-flight still cleared (Mate-6 trap)`() {
        val dismissed = loadedState(
            GroupsBlockState(inFlight = setOf(5L), isPickerOpen = false),
        )

        val result = reducer.reduce(dismissed, Msg.MembershipDone(groupId = 5)).first

        assertTrue(result.groupsBlock.inFlight.isEmpty())
    }

    // === Guard экрана (ревью Mate-5) ===

    @Test
    fun `picker intents guarded by pending db op`() {
        val pending = loadedState(GroupsBlockState(dictGroups = dictGroups))
            .copy(isPendingDbOp = true)

        val open = reducer.reduce(pending, Msg.OpenGroupPicker)
        assertFalse(open.first.groupsBlock.isPickerOpen)

        val toggle = reducer.reduce(pending, Msg.ToggleGroupMembership(groupId = 5))
        assertTrue(toggle.second.isEmpty())
    }

    @Test
    fun `done is NOT guarded - in-flight clears even while pending`() {
        val pending = loadedState(GroupsBlockState(inFlight = setOf(5L)))
            .copy(isPendingDbOp = true)

        val result = reducer.reduce(pending, Msg.MembershipDone(groupId = 5)).first

        assertTrue(result.groupsBlock.inFlight.isEmpty())
    }

    // === Flush-on-back (ревью UX-1/Arch-1, контракт А11) ===

    @Test
    fun `back while membership in flight - exit deferred until done`() {
        val inFlight = loadedState(GroupsBlockState(inFlight = setOf(5L)))
        assertTrue(inFlight.hasInFlightCommits)

        // Back: isExiting поднимается, но NavigationEffect.Back НЕ эмитится.
        val exiting = reducer.reduce(inFlight, Msg.NavigateBack)
        assertTrue(exiting.first.isExiting)
        assertTrue(
            exiting.second.none { it is me.apomazkin.mate.NavigationEffect },
        )

        // Запись долетела → выход.
        val done = reducer.reduce(exiting.first, Msg.MembershipDone(groupId = 5))
        assertTrue(
            done.second.any { it == me.apomazkin.mate.NavigationEffect.Back },
        )
    }

    // === Мапперы outcome → Msg ===

    @Test
    fun `membership outcome mappers - all flat to done`() {
        assertEquals(Msg.MembershipDone(5), AddMembershipOutcome.Added.toMembershipMsg(5))
        assertEquals(Msg.MembershipDone(5), AddMembershipOutcome.AlreadyIn.toMembershipMsg(5))
        assertEquals(Msg.MembershipDone(5), AddMembershipOutcome.GroupNotFound.toMembershipMsg(5))
        assertEquals(Msg.MembershipDone(5), AddMembershipOutcome.WordNotFound.toMembershipMsg(5))
        assertEquals(Msg.MembershipDone(5), RemoveMembershipOutcome.Removed.toMembershipMsg(5))
        assertEquals(Msg.MembershipDone(5), RemoveMembershipOutcome.NotFound.toMembershipMsg(5))
    }

    // === Сценарий: пикер → галочка → done → подписка → чип ===

    @Test
    fun `scenario - add membership via picker, chip appears from subscription`() {
        var state = loadedState()
        fun send(msg: Msg): Set<me.apomazkin.mate.Effect> {
            val r = reducer.reduce(state, msg)
            state = r.first
            return r.second
        }

        send(Msg.DictGroupsLoaded(dictGroups))
        send(Msg.WordGroupsLoaded(emptySet()))
        send(Msg.OpenGroupPicker)
        assertTrue(state.groupsBlock.isPickerOpen)

        val effects = send(Msg.ToggleGroupMembership(groupId = 5))
        assertEquals(
            setOf<me.apomazkin.mate.Effect>(
                DatasourceEffect.AddMembership(wordId = 3, groupId = 5),
            ),
            effects,
        )
        assertEquals(setOf(5L), state.groupsBlock.inFlight)
        // До эмиссии подписки чипов нет (D22.3).
        assertTrue(state.groupsBlock.chips.isEmpty())

        send(Msg.MembershipDone(groupId = 5))
        assertTrue(state.groupsBlock.inFlight.isEmpty())

        // «Handler»: живая подписка переэмитила членства.
        send(Msg.WordGroupsLoaded(setOf(5L)))
        assertEquals(listOf(GroupUi(5, "Быт")), state.groupsBlock.chips)

        // Снятие: направление remove, чип уходит по подписке.
        val removeEffects = send(Msg.ToggleGroupMembership(groupId = 5))
        assertEquals(
            setOf<me.apomazkin.mate.Effect>(
                DatasourceEffect.RemoveMembership(wordId = 3, groupId = 5),
            ),
            removeEffects,
        )
        send(Msg.MembershipDone(groupId = 5))
        send(Msg.WordGroupsLoaded(emptySet()))
        assertTrue(state.groupsBlock.chips.isEmpty())
    }
}
