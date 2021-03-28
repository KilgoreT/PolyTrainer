package me.apomazkin.core_db_impl.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TermDb(
    @Embedded val wordDb: WordDb,
    @Relation(
        parentColumn = "id",
        entityColumn = "wordId"
    )
    val definitionDbList: List<DefinitionDb>
)