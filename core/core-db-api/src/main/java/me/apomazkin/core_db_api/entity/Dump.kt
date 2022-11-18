package me.apomazkin.core_db_api.entity

data class Dump(
    val languages: List<LanguageDump>,
    val words: List<WordDump>,
    val definitions: List<DefinitionDump>,
    val hints: List<HintDump>,
    val samples: List<SampleDump>,
    val writes: List<WriteQuizDump>,
)
