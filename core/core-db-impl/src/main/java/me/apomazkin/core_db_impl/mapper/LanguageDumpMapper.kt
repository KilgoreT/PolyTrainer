package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.LanguageDump
import me.apomazkin.core_db_impl.entity.LanguageDb

fun LanguageDb.toDumpEntity() = LanguageDump(
    id = this.id,
    numericCode = this.numericCode,
    code = this.code,
    name = this.name,
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<LanguageDb>.toDumpEntity() = this.map { it.toDumpEntity() }

fun LanguageDump.toDbEntity() = LanguageDb(
    id = this.id,
    numericCode = this.numericCode,
    code = this.code,
    name = this.name,
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<LanguageDump>.toDbEntity() = this.map { it.toDbEntity() }
