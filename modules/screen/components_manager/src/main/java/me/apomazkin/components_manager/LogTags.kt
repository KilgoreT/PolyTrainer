package me.apomazkin.components_manager

/**
 * Log tags для `ComponentsManagerScreen` business слоя.
 *
 * Используется в `ComponentsManagerUseCaseImpl` (CRUD operations error logging)
 * и в Mate-обвязке (FlowHandler / DatasourceEffectHandler) — единый prefix
 * для grep'а в logs.
 */
object LogTags {
    const val ALL_COMPONENTS = "###ALL_COMPONENTS###"
}
