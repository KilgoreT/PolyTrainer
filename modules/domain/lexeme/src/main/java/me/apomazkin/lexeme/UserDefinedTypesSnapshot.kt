package me.apomazkin.lexeme

/**
 * Snapshot для aggregated view ComponentsManagerScreen.
 *
 * Dedicated data class устраняет неоднозначность `.first/.second` в reducer
 * (F1 iter1 review).
 */
data class UserDefinedTypesSnapshot(
    val types: List<ComponentType>,
    val usage: ComponentUsage,
)
