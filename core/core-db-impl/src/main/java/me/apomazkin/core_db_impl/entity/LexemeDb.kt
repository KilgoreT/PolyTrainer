package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "lexemes")
data class LexemeDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val wordId: Long? = null,
    val translation: String? = null,
    val definition: String? = null,
    val wordClass: String? = null,
    val options: Long = 0,
    val addDate: Date,
    val changeDate: Date? = null,
    val removeDate: Date? = null,
)