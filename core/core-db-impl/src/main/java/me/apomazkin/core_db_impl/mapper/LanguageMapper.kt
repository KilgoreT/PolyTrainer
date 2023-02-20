package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Language
import me.apomazkin.core_db_impl.entity.LanguageDb

fun LanguageDb.toAppEntity() = Language(
    id = this.id,
    numericCode = this.numericCode,
    code = this.code,
    name = this.name,
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<LanguageDb>.toAppEntity() = this.map { it.toAppEntity() }

fun Language.toDbEntity() = LanguageDb(
    id = this.id,
    numericCode = this.numericCode,
    code = this.code,
    name = this.name,
    addDate = this.addDate,
    changeDate = this.changeDate,
)

fun List<Language>.toDbEntity() = this.map { it.toDbEntity() }
