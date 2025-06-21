package me.apomazkin.quiz.chat.quiz

import androidx.compose.ui.text.AnnotatedString

interface QuizGame {
    suspend fun loadData()
    suspend fun loadNextData()
    fun hasSingleSession(): Boolean
    fun hasNextQuestion(): Boolean
    fun nextQuestion(): AnnotatedString
    fun skip()
    fun skipAndGetAnswer(): AnnotatedString
    fun makeAssessment(userAttempt: String): AnnotatedString
    fun summary(all: Boolean = false): List<AnnotatedString>
    suspend fun saveSession()
    fun getStat(): AnnotatedString?
}