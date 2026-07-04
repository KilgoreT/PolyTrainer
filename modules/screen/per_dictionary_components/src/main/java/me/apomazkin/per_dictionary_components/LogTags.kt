package me.apomazkin.per_dictionary_components

/**
 * Log tags для `PerDictionaryComponentsScreen` business слоя.
 *
 * Используется в `PerDictionaryComponentsUseCaseImpl` (CRUD operations error logging)
 * и в Mate-обвязке (FlowHandler / DatasourceEffectHandler).
 */
object LogTags {
    const val DICT_COMPONENTS = "###DICT_COMPONENTS###"
}
