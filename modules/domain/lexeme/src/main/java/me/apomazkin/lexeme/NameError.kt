package me.apomazkin.lexeme

/**
 * Доменные ошибки имени user-defined component_type. Используются в UI State обоих
 * экранов конструктора (`CreateDialogState.nameError`, `RenameDialogState.nameError`).
 */
sealed interface NameError {
    /** name.isBlank(). */
    data object Empty : NameError

    /** Active row с тем же именем уже есть в этом scope. */
    data object SameScopeCollision : NameError

    /** Cross-scope коллизия (global ⊥ per-dict invariant, F039). */
    data object CrossScopeCollision : NameError
}
