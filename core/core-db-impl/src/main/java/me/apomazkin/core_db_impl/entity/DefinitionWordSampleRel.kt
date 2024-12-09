package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DefinitionWordSampleRel(
    @Embedded val lexemeDb: LexemeDb,
    @Relation(
        parentColumn = "wordId",
        entityColumn = "id"
    )
    val wordDb: WordDb,
    @Relation(
        parentColumn = "id",
        entityColumn = "lexemeId"
    )
    val sampleDbList: List<SampleDb>,
)