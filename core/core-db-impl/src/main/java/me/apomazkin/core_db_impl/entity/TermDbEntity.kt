package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_impl.mapper.toApiEntity

data class TermDbEntity(
    @Embedded val wordDb: WordDb,
    @Relation(
        entity = LexemeDb::class,
        parentColumn = "id",
        entityColumn = "wordId"
    )
    val lexemeListDb: List<LexemeDbEntity>
)

fun TermDbEntity.toApiEntity() = TermApiEntity(
    word = this.wordDb.toApiEntity(),
    lexemes = lexemeListDb.toApiEntity()
)