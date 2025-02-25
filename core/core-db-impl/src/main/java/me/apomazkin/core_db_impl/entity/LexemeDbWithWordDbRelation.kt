package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation

data class LexemeDbWithWordDbRelation(
    @Embedded val lexemeDb: LexemeDbEntity,
    @Relation(
        parentColumn = "word_id",
        entityColumn = "id"
    )
    val wordDb: WordDb,
)