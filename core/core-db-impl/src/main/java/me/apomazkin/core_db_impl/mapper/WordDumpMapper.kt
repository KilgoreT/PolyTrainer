package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.WordDump
import me.apomazkin.core_db_impl.entity.WordDb

fun WordDb.toDumpEntity() = WordDump(
    id = this.id,
    dictionaryId = this.dictionaryId,
    word = this.value,
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<WordDb>.toDumpEntity() = this.map { it.toDumpEntity() }

fun WordDump.toDbEntity() = WordDb(
    id = this.id ?: throw IllegalArgumentException("Word id is null"),
    dictionaryId = this.dictionaryId,
    value = this.word ?: throw IllegalArgumentException("Word value is null"),
    addDate = this.addDate
        ?: throw IllegalArgumentException("Word add date is null"),
    changeDate = this.changeDate,
)

fun List<WordDump>.toDbEntity() = this.map { it.toDbEntity() }
