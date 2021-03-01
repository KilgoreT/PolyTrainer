package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation

data class QuizDb(
    @Embedded
    val definitionDb: DefinitionDb,
    @Relation(
        parentColumn = "wordId",
        entityColumn = "id"
    )
    val wordDb: WordDb
)