package me.apomazkin.components_manager.deps

import kotlinx.coroutines.flow.Flow
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.UserDefinedTypesSnapshot

/**
 * UseCase API для `ComponentsManagerScreen` — aggregated CRUD view над user-defined
 * component_types по всем словарям.
 *
 * Built-in компоненты в этом UseCase не фигурируют (`system_key IS NULL`-фильтр
 * на data-слое; см. ui_placement.md § Общий view).
 */
interface ComponentsManagerUseCase {

    /**
     * Реактивная подписка на все user-defined component_types + aggregated usage.
     * Один dedicated snapshot — без N+1 запросов из reducer.
     */
    fun flowAllUserDefinedTypes(): Flow<UserDefinedTypesSnapshot>

    /**
     * Создать user-defined component_type.
     *
     * Validate name (non-blank) → two-prong SELECT (same-scope / cross-scope) →
     * INSERT row(s). Для `Scope.PerDictionaries(N)` создаётся N rows в одной
     * транзакции.
     *
     * @return [CreateOutcome.Success] с list созданных entities (length = N) либо typed error.
     */
    suspend fun createUserDefinedComponent(
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
        scope: Scope,
    ): CreateOutcome

    /**
     * Read-only preview: valueCount + dictionariesWithValues + affectedQuizConfigs +
     * affectedPrefs. UI вызывает перед confirm-dialog.
     */
    suspend fun previewDeletionImpact(typeId: ComponentTypeId): DeletionImpact?

    /**
     * Soft-delete + cascade cleanup `quiz_configs.component_refs` (в транзакции) +
     * сброс `quiz_picker_dict_<id>` prefs (вне транзакции, на UseCase-уровне).
     * `component_values` остаются — скрываются через JOIN на parent.removed_at.
     */
    suspend fun softDeleteComponent(typeId: ComponentTypeId): DeleteOutcome

    /**
     * Edit existing user-defined component_type — name / template / isMultiple (IS481 phase 2).
     *
     * Business rules (UseCaseImpl-level):
     * - `name.trim().isBlank()` → [EditOutcome.NameEmpty] (без обращения к data API).
     * - `template != current.template` → [EditOutcome.TemplateImmutable] (F017,
     *   без обращения к data API).
     * - exception (CancellationException пробрасывается) → [EditOutcome.Failure].
     *
     * API-level (см. `LexemeApi.editComponentType`):
     * - `removed_at IS NOT NULL` → [EditOutcome.Removed] (F012).
     * - `system_key IS NOT NULL` → [EditOutcome.BuiltInProtected].
     * - same/cross-scope name collision → [EditOutcome.SameScopeCollision] /
     *   [EditOutcome.CrossScopeCollision].
     * - cardinality downgrade `isMultiple: true → false` при impacted lexemes
     *   → [EditOutcome.CardinalityDowngradeBlocked].
     * - success → [EditOutcome.Success] (cascade `quiz_configs.component_refs`
     *   если name изменился).
     */
    suspend fun editComponent(
        typeId: ComponentTypeId,
        name: String,
        template: ComponentTemplate,
        isMultiple: Boolean,
    ): EditOutcome

    /**
     * Reactive subscription на список словарей (для multi-dict scope picker в Create-диалоге).
     * Делегирует на `dictionaryApi.flowDictionaryList()` без mapping (F026).
     */
    fun flowDictionaries(): Flow<List<DictionaryApiEntity>>
}
