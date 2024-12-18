package me.apomazkin.core_db_api.entity


data class TermApiEntity(
    val word: WordApiEntity,
    val lexemes: List<LexemeApiEntity>
)