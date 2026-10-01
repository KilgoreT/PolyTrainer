package me.apomazkin.quiz.chat.quiz

import androidx.compose.ui.text.AnnotatedString

interface QuizGame {
    suspend fun loadData()
    fun hasNextQuestion(): Boolean

    /** Текущий вопрос; вызывать только после `hasNextQuestion() == true`. */
    fun nextQuestion(): QuizQuestion
    fun skip()
    fun skipAndGetAnswer(): AnnotatedString
    fun makeAssessment(userAttempt: String): AnnotatedString
    fun summaryGeneral(): AnnotatedString
    fun summaryDetail(): AnnotatedString
    suspend fun saveSession()
    fun getStat(): AnnotatedString?
}
