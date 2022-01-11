package me.apomazkin.core_db_impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "writeQuiz")
data class WriteQuizDb(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val langId: Long = 0,
    val definitionId: Long,
    val grade: Int = 0,
    val score: Int = 0,
    val addDate: Date? = null,
    val lastSelectDate: Date? = null,
)