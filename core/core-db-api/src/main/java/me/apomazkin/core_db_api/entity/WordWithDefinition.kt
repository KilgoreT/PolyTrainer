package me.apomazkin.core_db_api.entity

data class WordWithDefinition(
    val word: Word,
    val definitionList: List<Definition>
)