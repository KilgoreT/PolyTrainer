package me.apomazkin.quiz.chat.entity

import java.util.Date

data class WriteQuiz(
        val id: Long,
        val langId: Long,
        val grade: Int,
        val score: Int,
        val errorCount: Int,
        val addDate: Date,
        val lastCorrectAnswerDate: Date? = null,
        val lexeme: Lexeme,
        val word: Word,
        val type: QuizType? = null
)

enum class QuizType(
    val value: String
) {
    GRADES("grades"),
    EARLIEST("earliest"),
    ERRORS("errors");
}

data class WriteQuizUpsertEntity(
        val id: Long,
        val langId: Long,
        val lexemeId: Long,
        val grade: Int,
        val score: Int,
        val errorCount: Int,
        val addDate: Date,
        val lastCorrectAnswerDate: Date?,
)