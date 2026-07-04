package me.apomazkin.lexeme

/**
 * Подзапись `DeletionImpact.affectedQuizConfigs`. Каждый элемент — quiz_config row,
 * у которого `component_refs` ссылается на удаляемый ComponentType.
 *
 * `quizMode` хранится как [String] (data-convention, F078); enum может появиться позже.
 */
data class AffectedQuizConfig(
    val dictionaryId: Long,
    val quizMode: String,
)
