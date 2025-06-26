package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.WriteQuizDump
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import java.util.Date

fun WriteQuizDb.toDump() = WriteQuizDump(
    id = id,
    langId = langId,
    definitionId = lexemeId,
    grade = grade,
    score = score,
    addDate = addDate,
    lastSelectDate = lastCorrectAnswerDate
)

fun WriteQuizDump.toDb() = WriteQuizDb(
        id = id,
        langId = langId,
        lexemeId = definitionId,
        grade = grade,
        score = score,
        addDate = addDate ?: Date(),
        lastCorrectAnswerDate = lastSelectDate
)