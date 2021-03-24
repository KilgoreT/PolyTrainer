package me.apomazkin.core_db_api.entity

data class WriteQuiz(
    val id: Long = 0,
    val definitionId: Long,
    val grade: Int = 0,
    val score: Int = 0,
)