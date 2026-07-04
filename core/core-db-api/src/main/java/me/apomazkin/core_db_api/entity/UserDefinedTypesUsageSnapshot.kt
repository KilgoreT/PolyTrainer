package me.apomazkin.core_db_api.entity

/**
 * Data-layer snapshot для aggregated view ComponentsManagerScreen.
 *
 * Маппится в domain `UserDefinedTypesSnapshot` + `ComponentUsage`. Raw maps по
 * primitive `Long` key — domain заворачивает в `ComponentTypeId`.
 */
data class UserDefinedTypesUsageSnapshot(
    val types: List<ComponentTypeApiEntity>,
    val valueCountByType: Map<Long, Int>,
    val dictionaryIdsByType: Map<Long, Set<Long>>,
    val dictionaryNames: Map<Long, String>,
)
