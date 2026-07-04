package me.apomazkin.lexeme

/**
 * Domain outcome для `renameComponentType`.
 *
 * [BuiltInProtected] — попытка переименовать built-in (запрещено на SQL-уровне
 * `WHERE system_key IS NULL`).
 */
sealed interface RenameOutcome {
    data class Success(val type: ComponentType) : RenameOutcome
    data object NameEmpty : RenameOutcome
    data object SameScopeCollision : RenameOutcome
    data object CrossScopeCollision : RenameOutcome
    data object BuiltInProtected : RenameOutcome

    /** type.removed_at IS NOT NULL — soft-deleted; не путать с BuiltInProtected (F004). */
    data object Removed : RenameOutcome

    data class Failure(val cause: Throwable) : RenameOutcome
}
