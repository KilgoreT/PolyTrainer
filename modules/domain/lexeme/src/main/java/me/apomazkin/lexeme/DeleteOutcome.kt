package me.apomazkin.lexeme

/**
 * Domain outcome для `softDeleteComponentType`.
 *
 * [Success.impact] — `DeletionImpact` для UI snackbar («N values hidden»).
 * [BuiltInProtected] — попытка удалить built-in (запрещено на SQL-уровне).
 */
sealed interface DeleteOutcome {
    data class Success(val impact: DeletionImpact) : DeleteOutcome
    data object BuiltInProtected : DeleteOutcome

    /** type.removed_at IS NOT NULL — повторный soft-delete; не путать с BuiltInProtected (F004). */
    data object Removed : DeleteOutcome

    data class Failure(val cause: Throwable) : DeleteOutcome
}
