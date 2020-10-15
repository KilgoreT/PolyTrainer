package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "definitions")
data class DefinitionDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val wordId: Long? = null,
    val definition: String? = null,
    val partOfSpeech: String? = null,
    val isTransitive: Boolean? = null,
    val isCountable: Boolean? = null
)