package me.apomazkin.quiz.chat.deps

import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.WriteQuizUpsertEntity

interface QuizChatUseCase {
    suspend fun getCurrentLangId(): Long
    suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int
    suspend fun getRandomWriteQuizList(
        limit: Int,
        maxGrade: Int,
        langId: Long
    ): List<WriteQuiz>
}