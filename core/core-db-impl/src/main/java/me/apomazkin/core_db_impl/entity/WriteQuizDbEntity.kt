package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity

data class WriteQuizDbEntity(
    @Embedded val writeQuizDb: WriteQuizDb,
    @Relation(
        entity = LexemeDb::class,
        parentColumn = "lexeme_id",
        entityColumn = "id"
    )
    val lexemeDbWithWordDbRelation: LexemeDbWithWordDbRelation,
)

fun WriteQuizDbEntity.toApiEntity() = WriteQuizComplexEntity(
    quizData = writeQuizDb.toApiEntity(),
    lexemeData = lexemeDbWithWordDbRelation.lexemeDb.toApiEntity(),
    wordData = lexemeDbWithWordDbRelation.wordDb.toApiEntity(),
    sampleData = lexemeDbWithWordDbRelation.lexemeDb.sampleDbList.toApiEntity(),
)
