package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation

data class WriteQuizDefinitionRel(
    @Embedded val writeQuizDb: WriteQuizDb,
    @Relation(
        entity = LexemeDb::class,
        parentColumn = "definitionId",
        entityColumn = "id"
    )
    val definitionWordSampleRel: DefinitionWordSampleRel
)
