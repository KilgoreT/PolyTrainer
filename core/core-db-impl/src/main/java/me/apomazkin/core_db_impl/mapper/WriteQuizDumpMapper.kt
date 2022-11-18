package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.WriteQuizDump
import me.apomazkin.core_db_impl.entity.WriteQuizDb

fun WriteQuizDb.toDump() = WriteQuizDump(
    id = id,
    langId = langId,
    definitionId = definitionId,
    grade = grade,
    score = score,
    addDate = addDate,
    lastSelectDate = lastSelectDate
)

fun WriteQuizDump.toDb() = WriteQuizDb(
    id = id,
    langId = langId,
    definitionId = definitionId,
    grade = grade,
    score = score,
    addDate = addDate,
    lastSelectDate = lastSelectDate
)