package me.apomazkin.core_db_api.entity

/**
 * Data-layer snapshot для PerDictionaryComponentsScreen.
 *
 * Маппится в domain `PerDictionarySnapshot`. Содержит [ComponentTypeApiEntity] (API)
 * и raw map `typeId → count`.
 */
data class DictionaryTypesSnapshot(
    val dictionaryId: Long,
    val dictionaryName: String,
    val types: List<ComponentTypeApiEntity>,
    val valueCountByType: Map<Long, Int>,
)
