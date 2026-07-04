package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.logger.LexemeLogger

data class TermDbEntity(
    @Embedded val wordDb: WordDb,
    @Relation(
        entity = LexemeDb::class,
        parentColumn = "id",
        entityColumn = "word_id"
    )
    val lexemeListDb: List<LexemeDbEntity>
)

fun TermDbEntity.toApiEntity(logger: LexemeLogger) = TermApiEntity(
    word = this.wordDb.toApiEntity(),
    lexemes = lexemeListDb.toApiEntity(logger)
)
