package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation

// TODO: to TERM rename
data class WordDefinitionRel(
    @Embedded val wordDb: WordDb,
    @Relation(
        entity = LexemeDb::class,
        parentColumn = "id",
        entityColumn = "wordId"
    )
    val definitionSampleRelList: List<DefinitionSampleRel>
)