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
    /** IS486 фаза 3: включая builtin-строки словаря (рубильник в конструкторе, spec §4). */
    val types: List<ComponentTypeApiEntity>,
    val valueCountByType: Map<Long, Int>,
    /** IS486: живые опции CHOICE-типов словаря. */
    val optionsByType: Map<Long, List<ComponentOptionApiEntity>> = emptyMap(),
)
