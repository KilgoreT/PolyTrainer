package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.WriteQuizDump
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import java.util.Date

fun WriteQuizDb.toDump() = WriteQuizDump(
    id = id,
    dictionaryId = dictionaryId,
    definitionId = lexemeId,
    grade = grade,
    score = score,
    addDate = addDate,
    lastSelectDate = lastCorrectAnswerDate
)

fun WriteQuizDump.toDb() = WriteQuizDb(
        id = id,
        dictionaryId = this.dictionaryId,
        lexemeId = definitionId,
        grade = grade,
        score = score,
        addDate = addDate ?: Date(),
        lastCorrectAnswerDate = lastSelectDate
)