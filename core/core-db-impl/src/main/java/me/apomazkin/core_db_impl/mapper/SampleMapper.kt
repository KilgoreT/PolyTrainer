package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.SampleApiEntity
import me.apomazkin.core_db_api.entity.Source
import me.apomazkin.core_db_impl.entity.SampleDb

fun SampleDb.toApiEntity() = SampleApiEntity(
    id = this.id ?: throw IllegalStateException("SampleDb hasn't id field."),
    lexemeId = this.lexemeId,
    value = this.value,
    source = this.source?.let { Source(it) },
    addDate = this.addDate,
    changeDate = this.changeDate

)

fun List<SampleDb>.toApiEntity() = this.map { it.toApiEntity() }

fun SampleApiEntity.toDbEntity() = SampleDb(
    id = this.id,
    lexemeId = this.lexemeId,
    value = this.value,
    source = this.source?.value,
    addDate = this.addDate,
    changeDate = this.changeDate
)

fun List<SampleApiEntity>.toDbEntity() = this.map { it.toDbEntity() }