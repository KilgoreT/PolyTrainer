package me.apomazkin.core_db_api.entity

import me.apomazkin.lexeme.DeletionImpact

/**
 * Data-layer typed outcomes для CRUD по user-defined component_types.
 *
 * Маппятся в domain `CreateOutcome` / `RenameOutcome` / `DeleteOutcome`.
 * `DeletionImpact` берётся из domain (`me.apomazkin.lexeme.DeletionImpact`) —
 * data знает domain по A1/MIN-2.
 */

sealed interface CreateComponentOutcome {
    /**
     * length = 1 для `Scope.Global` / single per-dict; length = N для
     * `Scope.PerDictionaries(N)`.
     */
    data class Success(val types: List<ComponentTypeApiEntity>) : CreateComponentOutcome
    data object SameScopeCollision : CreateComponentOutcome
    data object CrossScopeCollision : CreateComponentOutcome
}

sealed interface RenameComponentOutcome {
    data class Success(val type: ComponentTypeApiEntity) : RenameComponentOutcome
    data object SameScopeCollision : RenameComponentOutcome
    data object CrossScopeCollision : RenameComponentOutcome
    data object BuiltInProtected : RenameComponentOutcome

    /** type.removed_at IS NOT NULL — soft-deleted (IS481 phase 2, F004). */
    data object Removed : RenameComponentOutcome
}

sealed interface SoftDeleteComponentOutcome {
    data class Success(val impact: DeletionImpact) : SoftDeleteComponentOutcome
    data object BuiltInProtected : SoftDeleteComponentOutcome

    /** IS486 (spec §7.8): удаление последнего включённого ядра словаря — отказ. */
    data object LastEnabledCore : SoftDeleteComponentOutcome

    /** type.removed_at IS NOT NULL — повторный soft-delete (IS481 phase 2, F004). */
    data object Removed : SoftDeleteComponentOutcome
}

/** IS486: outcome рубильника enabled (spec §6, §7.8). */
sealed interface SetEnabledComponentOutcome {
    data class Success(val type: ComponentTypeApiEntity) : SetEnabledComponentOutcome

    /** Попытка выключить последнее включённое ядро словаря (spec §7.8). */
    data object LastEnabledCore : SetEnabledComponentOutcome

    /** type.removed_at IS NOT NULL. */
    data object Removed : SetEnabledComponentOutcome
}

/** IS486: outcome CRUD опций CHOICE-компонента (К1–К5). */
sealed interface OptionCrudOutcome {
    data class Success(val option: ComponentOptionApiEntity) : OptionCrudOutcome

    /** Удаление: фактический комбинированный каскад (значения + degraded-потомки). */
    data class Deleted(val impact: DeletionImpact) : OptionCrudOutcome

    /** Опция уже soft-deleted / не найдена. */
    data object Removed : OptionCrudOutcome

    /**
     * Решение §21.2 (2026-07-20): опции builtin-компонентов нередактируемы —
     * ни add, ни rename, ни delete. Defense-in-depth (UI и так не даёт).
     */
    data object BuiltInProtected : OptionCrudOutcome
}

/**
 * Data-layer outcome для `editComponentType` (IS481 phase 2).
 *
 * 7 вариантов: Success / SameScopeCollision / CrossScopeCollision /
 * CardinalityDowngradeBlocked / TemplateImmutable / BuiltInProtected / Removed.
 *
 * `NameEmpty` / `Failure` отсутствуют на API уровне — валидация и try-catch
 * выполняются на UseCaseImpl (F027).
 */
sealed interface EditComponentOutcome {
    data class Success(val type: ComponentTypeApiEntity) : EditComponentOutcome
    data object SameScopeCollision : EditComponentOutcome
    data object CrossScopeCollision : EditComponentOutcome

    /** IS486: перепривязка создала бы цикл (spec §8). */
    data object CycleDetected : EditComponentOutcome

    /** IS486: isMultiple = true для шаблона CHOICE (spec §7.5). */
    data object MultiForbiddenForChoice : EditComponentOutcome

    /** IS486: перепривязка последнего включённого ядра на не-лексему (spec §7.8). */
    data object LastEnabledCore : EditComponentOutcome

    /**
     * Downgrade `isMultiple: true → false` заблокирован — есть лексемы с count > 1.
     * `impactedLexemeIds` — полный список ids в deterministic порядке
     * (`ORDER BY component_values.updated_at DESC, lexeme_id ASC`).
     */
    data class CardinalityDowngradeBlocked(val impactedLexemeIds: List<Long>) : EditComponentOutcome

    /** Defensive parity — основная проверка на UseCase, API возвращает defense-in-depth. */
    data object TemplateImmutable : EditComponentOutcome
    data object BuiltInProtected : EditComponentOutcome
    data object Removed : EditComponentOutcome
}

/**
 * Wrapper для read-only preview impact (отдельный data-layer outcome не нужен —
 * preview не имеет typed-ошибок: либо `null` (типа нет), либо `DeletionImpact`).
 * Тип-alias для читаемости; не sealed.
 */
typealias PreviewDeletionOutcome = DeletionImpact?
