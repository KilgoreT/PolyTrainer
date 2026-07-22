package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity для component_options. IS486 (M13→v13 фаза 1).
 *
 * - Опция CHOICE-компонента; на `id` ссылаются `component_types.depends_on_option_id`
 *   (зависимости) и `component_values.option_id` (значения лексем).
 * - `system_key` — стабильный ключ builtin-опции (`noun`, `verb`, ...); null → пользовательская.
 * - `label` — текст пользовательской опции / override builtin; для builtin-опций NULL
 *   (display резолвится из ресурсов по ключу — дом-паттерн builtin-компонентов).
 * - FK CASCADE на `component_types.id` — hard-delete случается только при удалении словаря
 *   (spec §9.5), дерево целиком внутри словаря.
 * - `removed_at` — soft-delete в стиле остальных таблиц.
 */
@Entity(
    tableName = "component_options",
    foreignKeys = [
        ForeignKey(
            entity = ComponentTypeDb::class,
            parentColumns = ["id"],
            childColumns = ["component_type_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("component_type_id"),
    ],
)
data class ComponentOptionDb(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "component_type_id") val componentTypeId: Long,
    @ColumnInfo(name = "system_key") val systemKey: String?,
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "position") val position: Int,
    @ColumnInfo(name = "created_at") val createdAt: Date,
    @ColumnInfo(name = "updated_at") val updatedAt: Date,
    @ColumnInfo(name = "removed_at") val removedAt: Date? = null,
)
