package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class WordDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val word: String? = null
)