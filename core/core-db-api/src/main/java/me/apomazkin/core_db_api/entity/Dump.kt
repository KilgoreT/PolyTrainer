package me.apomazkin.core_db_api.entity

data class Dump(
    val dictionaries: List<DictionaryDump>,
    val words: List<WordDump>,
    val definitions: List<DefinitionDump>,
    val writes: List<WriteQuizDump>,
)
