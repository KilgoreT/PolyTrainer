package me.apomazkin.core_db_api.entity

import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.DependencyTarget
import java.util.Date

/**
 * API DTO для ComponentType.
 *
 * `systemKey` / `template` — enum'ы из `modules/domain/lexeme`
 * (data знает domain по A1, MIN-2).
 */
data class ComponentTypeApiEntity(
    val id: Long,
    val systemKey: BuiltInComponent?,
    val dictionaryId: Long?,
    val name: String?,
    val template: ComponentTemplate,
    val position: Int,
    val isMultiple: Boolean = false,
    val core: Boolean = false,               // IS486: ядро
    val enabled: Boolean = true,             // IS486: рубильник
    val dependsOn: DependencyTarget = DependencyTarget.Lexeme,  // IS486: цель зависимости
    val createdAt: Date,
    val updatedAt: Date,
    val removedAt: Date? = null,
)
