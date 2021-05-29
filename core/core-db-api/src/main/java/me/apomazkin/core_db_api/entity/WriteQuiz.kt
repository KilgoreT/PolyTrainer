package me.apomazkin.core_db_api.entity

import java.util.*

data class WriteQuiz(
    val id: Long = 0,
    val definitionId: Long,
    val grade: Int = 0,
    val score: Int = 0,
    val addDate: Date? = null,
    val lastSelectDate: Date? = null,
)