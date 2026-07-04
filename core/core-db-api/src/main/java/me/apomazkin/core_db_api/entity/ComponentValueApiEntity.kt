package me.apomazkin.core_db_api.entity

import me.apomazkin.lexeme.TemplateValues
import java.util.Date

/**
 * API DTO для ComponentValue.
 *
 * `type` — full embedded `ComponentTypeApiEntity` (multi-level @Relation).
 * `data` — sealed [TemplateValues] из domain (M13: replaces `ComponentValueData`).
 *
 * Все active queries фильтруют `removedAt IS NULL` (aspect `dao_convention`, F031).
 */
data class ComponentValueApiEntity(
    val id: Long,
    val lexemeId: Long,
    val type: ComponentTypeApiEntity,
    val data: TemplateValues,
    val createdAt: Date,
    val updatedAt: Date,
    val removedAt: Date? = null,
)
