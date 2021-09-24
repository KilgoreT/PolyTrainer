package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Word
import me.apomazkin.core_db_impl.entity.WordDb

fun WordDb.toAppEntity() = Word(
    id = this.id,
    value = this.word,
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<WordDb>.toAppEntity() = this.map { it.toAppEntity() }

fun Word.toDbEntity() = WordDb(
    id = this.id,
    word = this.value,
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<Word>.toDbEntity() = this.map { it.toDbEntity() }
