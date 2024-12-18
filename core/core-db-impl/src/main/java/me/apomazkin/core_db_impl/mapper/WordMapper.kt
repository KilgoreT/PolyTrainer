package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.WordApiEntity
import me.apomazkin.core_db_impl.entity.WordDb

fun WordDb.toApiEntity() = WordApiEntity(
    id = this.id,
    langId = this.langId,
    value = this.value ?: throw IllegalArgumentException("WordDb value is null"),
    addDate = this.addDate ?: throw IllegalArgumentException("WordDb addDate is null"),
    changeDate = this.changeDate,
)

fun List<WordDb>.toApiEntity() = this.map { it.toApiEntity() }

fun WordApiEntity.toDbEntity() = WordDb(
    id = this.id,
    langId = this.langId,
    value = this.value,
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<WordApiEntity>.toDbEntity() = this.map { it.toDbEntity() }
