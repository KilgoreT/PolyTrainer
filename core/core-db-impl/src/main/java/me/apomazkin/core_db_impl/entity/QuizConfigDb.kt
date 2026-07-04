package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import me.apomazkin.core_db_api.entity.QuizConfigApiEntity
import me.apomazkin.core_db_impl.mapper.toComponentTypeRefList

/**
 * Room entity для quiz_configs. IS481.
 *
 * Один row на пару (dictionary_id, quiz_mode). `component_refs` — JSON-array
 * сериализованный `List<ComponentTypeRef>` (см. `ComponentTypeRefJson.kt`).
 *
 * FK CASCADE: удаление словаря чистит все его configs.
 */
@Entity(
    tableName = "quiz_configs",
    foreignKeys = [
        ForeignKey(
            entity = DictionaryDb::class,
            parentColumns = ["id"],
            childColumns = ["dictionary_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("dictionary_id"),
        Index(value = ["dictionary_id", "quiz_mode"], unique = true),
    ],
)
data class QuizConfigDb(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "dictionary_id") val dictionaryId: Long,
    @ColumnInfo(name = "quiz_mode") val quizMode: String,
    @ColumnInfo(name = "component_refs") val componentRefs: String,
)

fun QuizConfigDb.toApiEntity(): QuizConfigApiEntity = QuizConfigApiEntity(
    id = id,
    dictionaryId = dictionaryId,
    quizMode = quizMode,
    componentRefs = componentRefs.toComponentTypeRefList(),
)
