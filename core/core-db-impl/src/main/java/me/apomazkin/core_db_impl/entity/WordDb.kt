package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// TODO: мигрировать value и addDate в non null
// TODO: id, langId в Int
@Entity(tableName = "words")
data class WordDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val langId: Long = 0,
    val value: String? = null,
    val addDate: Date? = null,
    val changeDate: Date? = null,
    val removeDate: Date? = null,
)