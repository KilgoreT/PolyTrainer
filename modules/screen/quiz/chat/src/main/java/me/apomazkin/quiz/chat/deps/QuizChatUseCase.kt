package me.apomazkin.quiz.chat.deps

import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.QuizConfig
import me.apomazkin.quiz.chat.entity.WriteQuiz
import me.apomazkin.quiz.chat.entity.WriteQuizUpsertEntity

interface QuizChatUseCase {
    suspend fun getCurrentDictionaryId(): Long?
    suspend fun updateWriteQuiz(entity: List<WriteQuizUpsertEntity>): Int
    suspend fun getRandomWriteQuizList(
        limit: Int,
        maxGrade: Int,
        dictionaryId: Long
    ): List<WriteQuiz>

    /**
     * IS481 (AGG-5, F2 fix). Lookup quiz config. null → row отсутствует
     * (F1 invariant нарушение, не crash). Domain `QuizConfig` (AGG-10).
     * Вызывается раз на quiz session start в `QuizGameImpl.fetchData` (F5 — no N+1).
     */
    suspend fun getQuizConfig(
        dictionaryId: Long,
        quizMode: String = "write",
    ): QuizConfig?

    /**
     * IS481 quiz picker (AGG-12). Domain `ComponentType` словаря, отсортированы
     * по `position`. Empty list — не null. Прокси над `LexemeApi.getComponentTypes`.
     */
    suspend fun getAvailableTypes(dictionaryId: Long): List<ComponentType>

    /**
     * IS481 quiz picker. Persistent ref словаря. null = не сохранён либо
     * corrupted pref (unknown built-in key / malformed format). Resolve default
     * — caller (reducer / QuizGameImpl).
     */
    suspend fun getQuizPickerSelection(dictionaryId: Long): ComponentTypeRef?

    /**
     * IS481 quiz picker. Persist ref словаря через `PrefsProvider` raw-string API.
     * Encoding internal: `builtin:<key>` / `user:<name>`.
     */
    suspend fun setQuizPickerSelection(dictionaryId: Long, ref: ComponentTypeRef)
}
