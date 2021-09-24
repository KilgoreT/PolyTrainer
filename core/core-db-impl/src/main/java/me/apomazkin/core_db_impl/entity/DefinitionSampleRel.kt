package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation

data class DefinitionSampleRel(
    @Embedded val definitionDb: DefinitionDb,
    @Relation(
        parentColumn = "id",
        entityColumn = "definitionId"
    )
    val sampleDbList: List<SampleDb>
)
