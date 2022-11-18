package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.SampleDump
import me.apomazkin.core_db_impl.entity.SampleDb

fun SampleDb.toDumpEntity() = SampleDump(
    id = this.id ?: throw IllegalStateException("SampleDb hasn't id field."),
    definitionId = this.definitionId,
    value = this.value,
    source = this.source,
    addDate = this.addDate,
    changeDate = this.changeDate

)

fun List<SampleDb>.toDumpEntity() = this.map { it.toDumpEntity() }

fun SampleDump.toDbEntity() = SampleDb(
    id = this.id,
    definitionId = this.definitionId,
    value = this.value,
    source = this.source,
    addDate = this.addDate,
    changeDate = this.changeDate
)

fun List<SampleDump>.toDbEntity() = this.map { it.toDbEntity() }