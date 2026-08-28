package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.Date

/**
 * Room entity для word_groups. IS493 (M13).
 *
 * Membership «слово W привязано к группе G»:
 * - PK составной `(word_id, group_id)` — первая не-autoincrement PK-форма
 *   в проекте; порядок колонок PK значим для сверки с 13.json.
 * - `INDEX(group_id)` обязателен: PK покрывает только префикс word_id.
 *   «Симметричный» индекс по word_id НЕ создавать — Room его не экспортирует.
 * - Строки удаляются hard-delete (в транзакции удаления группы) либо
 *   FK CASCADE (удаление слова/словаря/группы).
 */
@Entity(
    tableName = "word_groups",
    primaryKeys = ["word_id", "group_id"],
    foreignKeys = [
        ForeignKey(
            entity = WordDb::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GroupDb::class,
            parentColumns = ["id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("group_id"),
    ],
)
data class WordGroupDb(
    @ColumnInfo(name = "word_id") val wordId: Long,
    @ColumnInfo(name = "group_id") val groupId: Long,
    @ColumnInfo(name = "created_at") val createdAt: Date,
)
