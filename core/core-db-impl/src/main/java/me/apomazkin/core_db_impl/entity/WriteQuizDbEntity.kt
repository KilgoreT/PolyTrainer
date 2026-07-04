package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.logger.LexemeLogger

data class WriteQuizDbEntity(
    @Embedded val writeQuizDb: WriteQuizDb,
    @Relation(
        entity = LexemeDb::class,
        parentColumn = "lexeme_id",
        entityColumn = "id"
    )
    val lexemeDbWithWordDbRelation: LexemeDbWithWordDbRelation,
)

fun WriteQuizDbEntity.toApiEntity(logger: LexemeLogger) = WriteQuizComplexEntity(
    quizData = writeQuizDb.toApiEntity(),
    lexemeData = lexemeDbWithWordDbRelation.lexemeDb.toApiEntity(logger),
    wordData = lexemeDbWithWordDbRelation.wordDb.toApiEntity(),
    sampleData = lexemeDbWithWordDbRelation.lexemeDb.sampleDbList.toApiEntity(),
)
