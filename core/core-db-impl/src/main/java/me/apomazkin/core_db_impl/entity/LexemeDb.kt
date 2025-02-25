package me.apomazkin.core_db_impl.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "lexemes",
    foreignKeys = [
        ForeignKey(
            entity = WordDb::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("word_id")]
)
data class LexemeDb(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "word_id") val wordId: Long,
    @ColumnInfo(name = "translation") val translation: String? = null,
    @ColumnInfo(name = "definition") val definition: String? = null,
    @ColumnInfo(name = "word_class") val wordClass: String? = null,
    @ColumnInfo(name = "options") val options: Long = 0,
    @ColumnInfo(name = "add_date") val addDate: Date,
    @ColumnInfo(name = "change_date") val changeDate: Date? = null,
)