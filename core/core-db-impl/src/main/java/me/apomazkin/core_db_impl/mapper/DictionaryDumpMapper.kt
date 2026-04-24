package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.DictionaryDump
import me.apomazkin.core_db_impl.entity.DictionaryDb

fun DictionaryDb.toDumpEntity() = DictionaryDump(
    id = this.id,
    numericCode = this.numericCode ?: 0,
    code = "",
    name = this.name,
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<DictionaryDb>.toDumpEntity() = this.map { it.toDumpEntity() }

fun DictionaryDump.toDbEntity() = DictionaryDb(
    id = this.id,
    numericCode = this.numericCode,
    name = this.name ?: "",
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<DictionaryDump>.toDbEntity() = this.map { it.toDbEntity() }
