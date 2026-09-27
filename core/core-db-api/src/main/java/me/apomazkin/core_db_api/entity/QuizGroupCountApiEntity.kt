package me.apomazkin.core_db_api.entity

/**
 * IS500: живая группа словаря со счётчиком слов для квиз-пикера.
 *
 * [wordCount] — семантика уровня 1: число слов группы, имеющих хотя бы
 * одну лексему (инвариант «лексема ⇔ write_quiz»); слова без лексем не
 * считаются. Группа без пригодных слов приходит со счётчиком 0 — порог
 * годности применяет домен, не data-слой.
 */
data class QuizGroupCountApiEntity(
    val groupId: Long,
    val name: String,
    val wordCount: Int,
)
