package me.apomazkin.core_db_api.entity

import java.util.Date

/**
 * IS486: API DTO для опции CHOICE-компонента.
 *
 * [id] — адрес опции (на него ссылаются зависимости и значения лексем).
 * [componentTypeId] — чья опция.
 * [systemKey] — стабильный ключ builtin-опции (`noun`, `verb`, ...); null → пользовательская.
 * [label] — текст пользовательской опции / override builtin; display = label ?: ресурс(systemKey).
 * [position] — порядок в списке.
 * [removedAt] — soft-delete; live-чтения отдают только null.
 */
data class ComponentOptionApiEntity(
    val id: Long,
    val componentTypeId: Long,
    val systemKey: String?,
    val label: String?,
    val position: Int,
    val removedAt: Date? = null,
)
