package me.apomazkin.groupstab.logic

import me.apomazkin.group.CreateGroupOutcome
import me.apomazkin.group.RenameGroupOutcome

/**
 * IS493 Э3: мапперы границы reducer'а для мутаций имени группы —
 * зеркальная пара: на входе шторка → эффект ([toMutationEffect]), на
 * выходе доменный outcome → ПЛОСКИЙ Msg ([toMutationMsg], зовёт handler
 * ДО отправки — правка юзера: простое сообщение → простое изменение
 * стейта, без макарон-when в ветках).
 */

/**
 * Сборка эффекта мутации из отправляемой шторки (intent → effect):
 * Create → [GroupsEffect.CreateGroup], Rename → [GroupsEffect.RenameGroup]
 * (groupId — из mode шторки).
 * @param dictionaryId текущий словарь вкладки (нужен только create).
 */
fun GroupSheetState.toMutationEffect(dictionaryId: Long): GroupsEffect =
    when (val mode = mode) {
        is GroupSheetMode.Create ->
            GroupsEffect.CreateGroup(
                dictionaryId = dictionaryId,
                name = input,
            )

        is GroupSheetMode.Rename ->
            GroupsEffect.RenameGroup(
                groupId = mode.groupId,
                name = input,
            )
    }

/** Таблица соответствий исходов создания (домен → плоский Msg). */
fun CreateGroupOutcome.toMutationMsg(): Msg =
    when (this) {
        is CreateGroupOutcome.Success -> Msg.MutationApplied
        is CreateGroupOutcome.EmptyName -> Msg.MutationRejected(GroupSheetError.EMPTY)
        is CreateGroupOutcome.DuplicateSibling -> Msg.MutationRejected(GroupSheetError.DUPLICATE)
        is CreateGroupOutcome.ReservedName -> Msg.MutationRejected(GroupSheetError.RESERVED)
        // Недостижимые в Э3 заделы (Э4/Э6).
        is CreateGroupOutcome.ParentNotFound -> Msg.MutationIgnored
        is CreateGroupOutcome.ParentHasWords -> Msg.MutationIgnored
    }

/** Таблица соответствий исходов переименования (домен → плоский Msg). */
fun RenameGroupOutcome.toMutationMsg(): Msg =
    when (this) {
        is RenameGroupOutcome.Success -> Msg.MutationApplied
        // Группа исчезла (гонка) — поведение как Applied: шторку закрыть,
        // список обновит живая подписка (D15.2).
        is RenameGroupOutcome.NotFound -> Msg.MutationApplied
        is RenameGroupOutcome.EmptyName -> Msg.MutationRejected(GroupSheetError.EMPTY)
        is RenameGroupOutcome.DuplicateSibling -> Msg.MutationRejected(GroupSheetError.DUPLICATE)
        is RenameGroupOutcome.ReservedName -> Msg.MutationRejected(GroupSheetError.RESERVED)
    }
