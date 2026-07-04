package me.apomazkin.core_db_api.entity

import me.apomazkin.lexeme.ComponentTypeRef

/**
 * API DTO для QuizConfig. `componentRefs` — domain sealed.
 */
data class QuizConfigApiEntity(
    val id: Long,
    val dictionaryId: Long,
    val quizMode: String,
    val componentRefs: List<ComponentTypeRef>,
)
