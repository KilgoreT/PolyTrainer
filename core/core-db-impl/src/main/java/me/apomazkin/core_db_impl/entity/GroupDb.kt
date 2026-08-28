package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import me.apomazkin.core_db_api.entity.GroupApiEntity
import java.util.Date

/**
 * Room entity для dictionary_groups. IS493 (M13).
 *
 * Группа (подсловарь) — узел дерева внутри словаря:
 * - `parentGroupId` null → корневая группа; self-FK CASCADE — прецедент
 *   `depends_on_type_id` (IS486), работает на bundled SQLite.
 * - `name` — хранится как введено; уникальность среди живых сиблингов —
 *   UseCase-валидация (Э3), UNIQUE-индекса нет (NULL parent + soft-delete).
 * - `kind` (Э4, 2026-08-23) — задел под будущую фичу папок: тип узла явный.
 *   В этой фиче всегда `GROUP`; в domain/API НЕ протягивается (мёртвый
 *   параметр до появления папок).
 * - `removedAt` — soft-delete (стиль ComponentTypeDb); FK CASCADE — страховка
 *   hard-пути (удаление словаря).
 *
 * Виртуальная группа «Все» строкой БД НЕ является (architecture А7).
 */
@Entity(
    tableName = "dictionary_groups",
    foreignKeys = [
        ForeignKey(
            entity = DictionaryDb::class,
            parentColumns = ["id"],
            childColumns = ["dictionary_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = GroupDb::class,
            parentColumns = ["id"],
            childColumns = ["parent_group_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("dictionary_id"),
        Index("parent_group_id"),
    ],
)
data class GroupDb(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "dictionary_id") val dictionaryId: Long,
    @ColumnInfo(name = "parent_group_id") val parentGroupId: Long? = null,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "kind", defaultValue = "GROUP") val kind: String = KIND_GROUP,
    @ColumnInfo(name = "created_at") val createdAt: Date,
    @ColumnInfo(name = "updated_at") val updatedAt: Date,
    @ColumnInfo(name = "removed_at") val removedAt: Date? = null,
)

/** Единственный тип узла в этой фиче; папки добавит будущая фича. */
const val KIND_GROUP = "GROUP"

fun GroupDb.toApiEntity() = GroupApiEntity(
    id = id,
    dictionaryId = dictionaryId,
    parentGroupId = parentGroupId,
    name = name,
)
