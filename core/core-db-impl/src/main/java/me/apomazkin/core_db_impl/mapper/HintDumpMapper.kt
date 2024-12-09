package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.HintDump
import me.apomazkin.core_db_impl.entity.HintDb

fun HintDb.toDumpEntity() = HintDump(
    id = id,
    lexemeId = lexemeId,
    value = value,
    addDate = addDate,
    changeDate = changeDate
)

fun List<HintDb>.toDumpEntity() = this.map { it.toDumpEntity() }

fun HintDump.toDbEntity() = HintDb(
    id = id,
    lexemeId = lexemeId,
    value = value,
    addDate = addDate,
    changeDate = changeDate
)

fun List<HintDump>.toDbEntity() = this.map { it.toDbEntity() }