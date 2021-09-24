package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DefinitionWordSampleRel(
    @Embedded val definitionDb: DefinitionDb,
    @Relation(
        parentColumn = "wordId",
        entityColumn = "id"
    )
    val wordDb: WordDb,
    @Relation(
        parentColumn = "id",
        entityColumn = "definitionId"
    )
    val sampleDbList: List<SampleDb>,
)