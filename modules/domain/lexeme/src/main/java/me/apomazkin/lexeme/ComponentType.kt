package me.apomazkin.lexeme

import java.util.Date

@JvmInline
value class ComponentTypeId(val id: Long)

/**
 * Domain entity типа компонента.
 *
 * - `systemKey` null → user-defined; non-null → built-in.
 * - `dictionaryId` null → global; non-null → per-dictionary.
 * - `name` для user-defined обязателен; для built-in — опциональный override
 *   (NULL означает «использовать дефолт из ресурса по `systemKey`»).
 */
data class ComponentType(
    val id: ComponentTypeId,
    val systemKey: BuiltInComponent?,
    val dictionaryId: Long?,
    val name: String?,
    val template: ComponentTemplate,
    val position: Int,
    val isMultiple: Boolean = false,         // NEW (M13)
    val createdAt: Date,                  // NEW (M13)
    val updatedAt: Date,                  // NEW (M13)
    val removedAt: Date? = null,          // RENAME removeDate → removedAt (M13)
)
