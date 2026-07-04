package me.apomazkin.lexeme

/**
 * Aggregated usage компонентов — `flowAllUserDefinedTypesWithUsage()` собирает
 * в одном snapshot, чтобы reducer не делал N+1 запросов по типу.
 *
 * - [valueCountByType] — typeId → суммарно активных component_values.
 * - [dictionaryIdsByType] — typeId → set dictionaryIds, где компонент применим
 *   (для global — все словари).
 * - [dictionaryNames] — dictionaryId → name (для UI badge).
 */
data class ComponentUsage(
    val valueCountByType: Map<ComponentTypeId, Int>,
    val dictionaryIdsByType: Map<ComponentTypeId, Set<Long>>,
    val dictionaryNames: Map<Long, String>,
)
