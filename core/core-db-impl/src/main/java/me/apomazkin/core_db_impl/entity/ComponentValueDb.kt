package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity для component_values. IS481 (M13).
 *
 * - FK CASCADE на `lexemes.id` (удаление лексемы чистит values).
 * - FK CASCADE на `component_types.id` (удаление типа чистит values).
 * - `value` — JSON-сериализованный [me.apomazkin.lexeme.TemplateValues] в M13 envelope
 *   `{"fields": {"value": {"type": "<text|image>", ...}}}` (см. `TemplateValuesJson.kt`).
 * - `createdAt` / `updatedAt` / `removedAt` (NEW M13).
 *
 * UNIQUE `(lexeme_id, component_type_id)` DROPPED в M13 — поддержка `is_multiple=true`.
 * Phantom `Index("lexeme_id")` восстанавливает производительность
 * `getForLexeme(lexemeId)` (после drop UNIQUE leading column больше не покрыт).
 */
@Entity(
    tableName = "component_values",
    foreignKeys = [
        ForeignKey(
            entity = LexemeDb::class,
            parentColumns = ["id"],
            childColumns = ["lexeme_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ComponentTypeDb::class,
            parentColumns = ["id"],
            childColumns = ["component_type_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("component_type_id"),
        Index("lexeme_id"),
    ],
)
data class ComponentValueDb(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "lexeme_id") val lexemeId: Long,
    @ColumnInfo(name = "component_type_id") val componentTypeId: Long,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "created_at") val createdAt: Date,
    @ColumnInfo(name = "updated_at") val updatedAt: Date,
    @ColumnInfo(name = "removed_at") val removedAt: Date? = null,
)
