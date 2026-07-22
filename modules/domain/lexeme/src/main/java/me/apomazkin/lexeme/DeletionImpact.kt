package me.apomazkin.lexeme

/**
 * Preview каскадного soft-delete user-defined component_type.
 *
 * Возвращается из `previewDeletionImpact()` и `softDeleteComponent()`
 * (для UI snackbar).
 *
 * - [affectedQuizConfigs] — `quiz_configs.component_refs` ссылается на удаляемый тип;
 *   будут вычищены в одной транзакции с soft-delete.
 * - [affectedPrefs] — `quiz_picker_dict_<id>` pref ссылается на удаляемый ref;
 *   сбрасывается after soft-delete (UseCase composition, prefs в DataStore вне Room).
 *
 * См. scope_analysis.md aspects `quiz_configs_cleanup` + `prefs_cleanup_on_soft_delete`.
 */
data class DeletionImpact(
    val valueCount: Int,
    val dictionariesWithValues: List<Long>,
    val affectedQuizConfigs: List<AffectedQuizConfig>,
    val affectedPrefs: List<Long>,
    /** IS486 (spec §5): компоненты, которые станут degraded (их цель умирает). */
    val degradedComponents: List<ComponentTypeId> = emptyList(),
    /** IS486 (spec §5): значения потомков по цепочке вниз, сбрасываемые каскадом. */
    val descendantValueCount: Int = 0,
)
