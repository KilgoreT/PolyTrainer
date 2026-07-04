package me.apomazkin.lexeme

/**
 * Snapshot для PerDictionaryComponentsScreen.
 *
 * Содержит ComponentTypes применимые к словарю (global ∪ per-dict) +
 * valueCountByType внутри этого словаря.
 */
data class PerDictionarySnapshot(
    val dictionaryId: Long,
    val dictionaryName: String,
    val types: List<ComponentType>,
    val valueCountByType: Map<ComponentTypeId, Int>,
)
