package me.apomazkin.core_db_impl.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Definition(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val word: String? = null,
    val definition: String? = null
)