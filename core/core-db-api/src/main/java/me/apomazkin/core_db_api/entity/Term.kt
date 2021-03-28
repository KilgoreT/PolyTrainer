package me.apomazkin.core_db_api.entity

data class Term(
    val word: Word,
    val definitionList: List<Definition>
)