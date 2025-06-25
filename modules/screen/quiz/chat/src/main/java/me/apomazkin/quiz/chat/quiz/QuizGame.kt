package me.apomazkin.quiz.chat.quiz

import androidx.compose.ui.text.AnnotatedString

interface QuizGame {
    suspend fun loadData()
    fun hasNextQuestion(): Boolean
    fun nextQuestion(): AnnotatedString
    fun skip()
    fun skipAndGetAnswer(): AnnotatedString
    fun makeAssessment(userAttempt: String): AnnotatedString
    fun summaryGeneral(): AnnotatedString
    fun summaryDetail(): AnnotatedString
    suspend fun saveSession()
    fun getStat(): AnnotatedString?
}