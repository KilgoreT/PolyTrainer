package me.apomazkin.lexeme

/**
 * Domain outcome CRUD опций CHOICE-компонента (IS486, spec К1–К5).
 *
 * - [Success] — add/rename прошёл; [option] — актуальная строка.
 * - [Deleted] — soft-delete опции; [impact] — фактический комбинированный каскад
 *   (значения-выборы + поддеревья зависимых компонентов).
 * - [Removed] — опция (или её тип) уже soft-deleted / не найдена.
 * - [Failure] — exception на data layer (try-catch на UseCaseImpl).
 */
sealed interface OptionOutcome {
    data class Success(val option: ComponentOption) : OptionOutcome
    data class Deleted(val impact: DeletionImpact) : OptionOutcome
    data object Removed : OptionOutcome

    /** Решение §21.2 (2026-07-20): опции builtin нередактируемы. */
    data object BuiltInProtected : OptionOutcome
    data class Failure(val cause: Throwable) : OptionOutcome
}
