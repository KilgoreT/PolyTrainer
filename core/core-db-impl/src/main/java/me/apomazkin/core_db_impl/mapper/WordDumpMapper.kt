package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.WordDump
import me.apomazkin.core_db_impl.entity.WordDb

fun WordDb.toDumpEntity() = WordDump(
    id = this.id,
    langId = this.langId,
    word = this.word,
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<WordDb>.toDumpEntity() = this.map { it.toDumpEntity() }

fun WordDump.toDbEntity() = WordDb(
    id = this.id,
    langId = this.langId,
    word = this.word,
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<WordDump>.toDbEntity() = this.map { it.toDbEntity() }
