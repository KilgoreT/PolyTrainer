package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.WriteQuiz
import me.apomazkin.core_db_impl.entity.WriteQuizDefinitionRel

fun WriteQuizDefinitionRel.toAppData(): WriteQuiz = WriteQuiz(
    id = writeQuizDb.id,
    langId = writeQuizDb.langId,
    definition = definitionWordSampleRel.getDefinition(),
    word = definitionWordSampleRel.wordDb.toAppEntity(),
    grade = writeQuizDb.grade,
    score = writeQuizDb.score,
    addDate = writeQuizDb.addDate,
    lastSelectDate = writeQuizDb.lastSelectDate,
)

fun List<WriteQuizDefinitionRel>.toAppData() = this.map { it.toAppData() }