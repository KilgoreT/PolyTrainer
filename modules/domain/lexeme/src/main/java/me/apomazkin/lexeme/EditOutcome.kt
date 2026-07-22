package me.apomazkin.lexeme

/**
 * Domain outcome для `editComponent` (IS481 phase 2).
 *
 * UseCaseImpl-level ветки:
 * - [NameEmpty] — `name.trim().isBlank()` (без обращения к data API).
 * - [TemplateImmutable] — defensive parity (основная проверка на UseCase, F017).
 *   Возвращается БЕЗ обращения к data API если `template != current.template`.
 * - [Failure] — exception на data layer (try-catch на UseCaseImpl).
 *
 * API-level ветки (`LexemeApi.editComponentType` → mapping):
 * - [Success] — UPDATE прошёл; cascade `quiz_configs.component_refs` если name изменился.
 * - [SameScopeCollision] / [CrossScopeCollision] — name занят.
 * - [CardinalityDowngradeBlocked] — downgrade `isMultiple: true → false` заблокирован
 *   при наличии лексем с count > 1; `impactedLexemeIds` — полный список ids в
 *   detrministic порядке (`ORDER BY updated_at DESC, lexeme_id ASC`).
 * - [BuiltInProtected] — `type.system_key IS NOT NULL`.
 * - [Removed] — `type.removed_at IS NOT NULL` (soft-deleted, асимметрия с Create, F004).
 */
sealed interface EditOutcome {
    data class Success(val updated: ComponentType) : EditOutcome
    data object NameEmpty : EditOutcome
    data object SameScopeCollision : EditOutcome
    data object CrossScopeCollision : EditOutcome
    data class CardinalityDowngradeBlocked(val impactedLexemeIds: List<Long>) : EditOutcome

    /** IS486: перепривязка `dependsOn` создала бы цикл (spec §8). */
    data object CycleDetected : EditOutcome

    /** IS486: isMultiple = true для шаблона CHOICE запрещён (spec §7.5). */
    data object MultiForbiddenForChoice : EditOutcome

    /** IS486: перепривязка последнего включённого ядра словаря на не-лексему (spec §7.8). */
    data object LastEnabledCore : EditOutcome

    data object TemplateImmutable : EditOutcome
    data object BuiltInProtected : EditOutcome
    data object Removed : EditOutcome
    data class Failure(val cause: Throwable) : EditOutcome
}
