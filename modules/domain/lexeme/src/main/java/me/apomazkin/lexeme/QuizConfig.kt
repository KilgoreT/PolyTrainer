package me.apomazkin.lexeme

/**
 * Domain entity конфига квиза. `componentRefs` — упорядоченный список ссылок
 * на типы компонентов используемые при сборке quiz session.
 *
 * Порядок (F4): позиция в списке определяет приоритет рендеринга компонента
 * в quiz session.
 *
 * TODO: вынести в `modules/domain/quiz` в рамках backlog-фичи «Quiz config UX»
 * (AGG-10).
 */
data class QuizConfig(
    val dictionaryId: Long,
    val quizMode: String,
    val componentRefs: List<ComponentTypeRef>,
)
