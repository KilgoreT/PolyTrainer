package me.apomazkin.core_db_api.entity

data class Term(
    val word: Word,
    val definitionList: List<Definition>
)

data class TermMate(
    val word: Word,
    val defList: List<DefinitionMate>
)

data class DefinitionMate(
    val id: Long,
    val wordId: Long,
    val value: String,
    val category: String,
)