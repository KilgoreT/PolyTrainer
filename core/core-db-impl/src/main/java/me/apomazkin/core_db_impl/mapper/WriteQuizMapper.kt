package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.WriteQuiz
import me.apomazkin.core_db_impl.entity.WriteQuizDb

class WriteQuizMapper : Mapper<WriteQuizDb, WriteQuiz>() {

    override fun map(value: WriteQuizDb) = WriteQuiz(
        value.id,
        value.definitionId,
        value.grade,
        value.score
    )

    override fun reverseMap(value: WriteQuiz) = WriteQuizDb(
        value.id,
        value.definitionId,
        value.grade,
        value.score
    )

}