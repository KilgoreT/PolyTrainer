package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.WriteQuiz
import me.apomazkin.core_db_impl.entity.WriteQuizDb

fun WriteQuiz.toDb() = WriteQuizDb(
    id = id,
    langId = langId,
    definitionId = definition.id ?: throw IllegalStateException("Not found id for WriteQuizDb"),
    grade = grade,
    score = score,
    addDate = addDate,
    lastSelectDate = lastSelectDate,
)