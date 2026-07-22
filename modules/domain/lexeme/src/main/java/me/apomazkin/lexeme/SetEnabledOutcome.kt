package me.apomazkin.lexeme

/**
 * Domain outcome рубильника enabled (IS486, spec §6).
 *
 * - [Success] — флаг записан; каскадов нет (мягкий рубильник: компонент лишь
 *   уходит из предложений, значения живут).
 * - [LastEnabledCore] — попытка выключить последнее включённое ядро словаря
 *   (spec §7.8) — отказ, состояние не изменено.
 * - [Removed] — тип soft-deleted / не найден.
 * - [Failure] — exception на data layer (try-catch на UseCaseImpl).
 */
sealed interface SetEnabledOutcome {
    data class Success(val updated: ComponentType) : SetEnabledOutcome
    data object LastEnabledCore : SetEnabledOutcome
    data object Removed : SetEnabledOutcome
    data class Failure(val cause: Throwable) : SetEnabledOutcome
}
