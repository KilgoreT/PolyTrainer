package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "writeQuiz")
data class WriteQuizDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val definitionId: Long,
    val grade: Int = 0,
    val score: Int = 0,
)