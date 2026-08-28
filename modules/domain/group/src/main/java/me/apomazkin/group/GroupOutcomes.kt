package me.apomazkin.group

/**
 * IS493 Э3 (D12.2): typed outcomes мутаций групп (§2.5).
 *
 * Заделы (недостижимые в Э3 ветки — чтобы when-потребители не
 * переписывались позже):
 * - [CreateGroupOutcome.ParentNotFound] — Э4 (подгруппы; parent отсутствует
 *   или soft-deleted, А17);
 * - [CreateGroupOutcome.ParentHasWords] — Э6 (инвариант «папка XOR лист»,
 *   А18: у родителя есть прямые слова — подгруппу создавать нельзя).
 */
sealed interface CreateGroupOutcome {
    data class Success(val groupId: Long) : CreateGroupOutcome
    data object EmptyName : CreateGroupOutcome
    data object DuplicateSibling : CreateGroupOutcome
    data object ReservedName : CreateGroupOutcome
    data object ParentNotFound : CreateGroupOutcome
    data object ParentHasWords : CreateGroupOutcome
}

/** NotFound = отсутствует ∪ soft-deleted (А17, распространено ревью D-5). */
sealed interface RenameGroupOutcome {
    data object Success : RenameGroupOutcome
    data object EmptyName : RenameGroupOutcome
    data object DuplicateSibling : RenameGroupOutcome
    data object ReservedName : RenameGroupOutcome
    data object NotFound : RenameGroupOutcome
}

sealed interface DeleteGroupOutcome {
    data object Success : DeleteGroupOutcome
    data object NotFound : DeleteGroupOutcome
}

/**
 * IS493 Э6 (D30.2): исход ДЕСТРУКТИВНОГО удаления группы вместе со
 * словами. [Success.deletedWords] — фактическое число удалённых слов на
 * момент транзакции (может отличаться от цифры конфирма при гонке).
 * [NotFound] — группа мертва ∪ отсутствует; слова НЕ тронуты.
 */
sealed interface DeleteGroupWithWordsOutcome {
    data class Success(val deletedWords: Int) : DeleteGroupWithWordsOutcome
    data object NotFound : DeleteGroupWithWordsOutcome
}

/**
 * IS493 Э5 (D20.2): исход добавления слова в группу.
 * [Added]/[AlreadyIn] — успех (UI не различает, дубль гасится составным
 * PK); [GroupNotFound] — группа мертва ИЛИ чужого словаря;
 * [WordNotFound] — слово удалено (hard). NotFound-исходы тихие: галочку
 * и чипы поправляет живая подписка wordGroups.
 */
sealed interface AddMembershipOutcome {
    data object Added : AddMembershipOutcome
    data object AlreadyIn : AddMembershipOutcome
    data object GroupNotFound : AddMembershipOutcome
    data object WordNotFound : AddMembershipOutcome
}

/**
 * IS493 Э5 (D20.2): исход снятия слова с группы.
 * [NotFound] (строки membership нет) — идемпотентный успех: снимать
 * нечего, цель достигнута.
 */
sealed interface RemoveMembershipOutcome {
    data object Removed : RemoveMembershipOutcome
    data object NotFound : RemoveMembershipOutcome
}
