package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import me.apomazkin.core_db_api.entity.WordApiEntity
import java.util.Date

@Entity(
    tableName = "words",
    foreignKeys = [
        ForeignKey(
            entity = LanguageDb::class,
            parentColumns = ["id"],
            childColumns = ["lang_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("lang_id")]
)
data class WordDb(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "lang_id") val langId: Long,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "add_date") val addDate: Date,
    @ColumnInfo(name = "change_date") val changeDate: Date? = null,
)

fun WordDb.toApiEntity() = WordApiEntity(
    id = this.id,
    langId = this.langId,
    value = this.value,
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<WordDb>.toApiEntity() = this.map { it.toApiEntity() }

fun WordApiEntity.toDbEntity() = WordDb(
    id = this.id,
    langId = this.langId,
    value = this.value,
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<WordApiEntity>.toDbEntity() = this.map { it.toDbEntity() }