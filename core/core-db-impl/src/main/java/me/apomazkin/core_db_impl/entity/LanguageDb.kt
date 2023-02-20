package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    tableName = "languages",
    indices = [Index(value = arrayOf("numericCode"), unique = true)]
)
data class LanguageDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val numericCode: Int,
    // TODO: Удалить?
    val code: String,
    // TODO: сделать ненулабельным
    val name: String? = null,
    val addDate: Date,
    val changeDate: Date? = null,
)