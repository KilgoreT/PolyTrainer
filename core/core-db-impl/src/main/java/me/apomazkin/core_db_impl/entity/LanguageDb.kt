package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "languages")
data class LanguageDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val code: String,
    val name: String? = null,
    val addDate: Date,
    val changeDate: Date? = null,
)