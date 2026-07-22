package me.apomazkin.lexeme

/**
 * Snapshot для PerDictionaryComponentsScreen.
 *
 * Содержит ComponentTypes применимые к словарю (global ∪ per-dict) +
 * valueCountByType внутри этого словаря.
 *
 * IS486 фаза 3: types включают builtin-строки словаря (рубильник enabled
 * в конструкторе, spec §4); [optionsByType] — живые опции CHOICE-типов.
 */
data class PerDictionarySnapshot(
    val dictionaryId: Long,
    val dictionaryName: String,
    val types: List<ComponentType>,
    val valueCountByType: Map<ComponentTypeId, Int>,
    val optionsByType: Map<ComponentTypeId, List<ComponentOption>> = emptyMap(),
)
