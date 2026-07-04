package me.apomazkin.lexeme

/**
 * Domain outcome для `createUserDefinedComponent`.
 *
 * [Success.created] длина = N: 1 для `Scope.Global` / single per-dict, N для
 * `Scope.PerDictionaries(N)`. Reducer получает все entities для optimistic
 * refresh / undo (F2 iter1 review).
 */
sealed interface CreateOutcome {
    data class Success(val created: List<ComponentType>) : CreateOutcome
    data object NameEmpty : CreateOutcome
    data object SameScopeCollision : CreateOutcome
    data object CrossScopeCollision : CreateOutcome
    data class Failure(val cause: Throwable) : CreateOutcome
}
