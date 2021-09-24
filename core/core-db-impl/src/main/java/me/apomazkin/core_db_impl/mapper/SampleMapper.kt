package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Sample
import me.apomazkin.core_db_impl.entity.SampleDb

fun SampleDb.toAppEntity() = Sample(
    id = this.id ?: throw IllegalStateException("SampleDb hasn't id field."),
    definitionId = this.definitionId,
    value = this.value,
    source = this.source,
    addDate = this.addDate,
    changeDate = this.changeDate

)

fun List<SampleDb>.toAppEntity() = this.map { it.toAppEntity() }

fun Sample.toDbEntity() = SampleDb(
    id = this.id,
    definitionId = this.definitionId,
    value = this.value,
    source = this.source,
    addDate = this.addDate,
    changeDate = this.changeDate
)

fun List<Sample>.toDbEntity() = this.map { it.toDbEntity() }