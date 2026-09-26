package me.apomazkin.groupstab.logic

import me.apomazkin.group.CreateGroupOutcome
import me.apomazkin.group.RenameGroupOutcome
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * IS493 Э3: мапперы границы reducer'а — вход (шторка → эффект) и выход
 * (доменные outcomes → плоские Msg). Раскладка Msg по атомам — в ветках
 * reducer'а (message-тесты).
 */
class MutationMappersTest {
    // === Маппер intent → effect ===

    @Test
    fun `submit create sheet maps to CreateGroup effect`() {
        val sheet = GroupSheetState(mode = GroupSheetMode.Create, input = "Сад")

        assertEquals(
            GroupsEffect.CreateGroup(dictionaryId = 1L, name = "Сад"),
            sheet.toMutationEffect(dictionaryId = 1L),
        )
    }

    @Test
    fun `submit rename sheet maps to RenameGroup effect with mode groupId`() {
        val sheet = GroupSheetState(mode = GroupSheetMode.Rename(groupId = 6L), input = "Дача")

        assertEquals(
            GroupsEffect.RenameGroup(groupId = 6L, name = "Дача"),
            sheet.toMutationEffect(dictionaryId = 1L),
        )
    }

    // === Маппер create outcome → Msg ===

    @Test
    fun `create outcomes map to expected messages`() {
        assertEquals(
            Msg.MutationApplied,
            CreateGroupOutcome.Success(groupId = 1).toMutationMsg(),
        )
        assertEquals(
            Msg.MutationRejected(GroupSheetError.EMPTY),
            CreateGroupOutcome.EmptyName.toMutationMsg(),
        )
        assertEquals(
            Msg.MutationRejected(GroupSheetError.DUPLICATE),
            CreateGroupOutcome.DuplicateSibling.toMutationMsg(),
        )
        assertEquals(
            Msg.MutationRejected(GroupSheetError.RESERVED),
            CreateGroupOutcome.ReservedName.toMutationMsg(),
        )
        assertEquals(Msg.MutationIgnored, CreateGroupOutcome.ParentNotFound.toMutationMsg())
        assertEquals(Msg.MutationIgnored, CreateGroupOutcome.ParentHasWords.toMutationMsg())
    }

    // === Маппер rename outcome → Msg ===

    @Test
    fun `rename outcomes map to expected messages`() {
        assertEquals(Msg.MutationApplied, RenameGroupOutcome.Success.toMutationMsg())
        // NotFound-гонка — поведение Applied: шторку закрыть, список
        // обновит живая подписка.
        assertEquals(Msg.MutationApplied, RenameGroupOutcome.NotFound.toMutationMsg())
        assertEquals(
            Msg.MutationRejected(GroupSheetError.EMPTY),
            RenameGroupOutcome.EmptyName.toMutationMsg(),
        )
        assertEquals(
            Msg.MutationRejected(GroupSheetError.DUPLICATE),
            RenameGroupOutcome.DuplicateSibling.toMutationMsg(),
        )
        assertEquals(
            Msg.MutationRejected(GroupSheetError.RESERVED),
            RenameGroupOutcome.ReservedName.toMutationMsg(),
        )
    }
}
