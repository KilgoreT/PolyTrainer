package me.apomazkin.quiz.chat.deps

import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.WriteQuizUpsertEntity

interface QuizChatUseCase {
    suspend fun getCurrentDictionaryId(): Long
    suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int
    suspend fun getRandomWriteQuizList(
        limit: Int,
        maxGrade: Int,
        dictionaryId: Long
    ): List<WriteQuiz>
}