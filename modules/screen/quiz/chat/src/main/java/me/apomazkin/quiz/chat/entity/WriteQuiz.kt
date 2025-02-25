package me.apomazkin.quiz.chat.entity

import java.util.Date

data class WriteQuiz(
    val id: Long,
    val langId: Long,
    val grade: Int,
    val score: Int,
    val errorCount: Int,
    val addDate: Date,
    val lastSelectDate: Date? = null,
    val lexeme: Lexeme,
    val word: Word,
)

data class WriteQuizUpsertEntity(
    val id: Long,
    val langId: Long,
    val lexemeId: Long,
    val grade: Int,
    val score: Int,
    val errorCount: Int,
    val addDate: Date,
    val lastSelectDate: Date,
)