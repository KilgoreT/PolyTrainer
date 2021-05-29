package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "words")
data class WordDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val word: String? = null,
    val addDate: Date? = null,
    val changeDate: Date? = null,
)