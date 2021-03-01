package me.apomazkin.core_db_impl.mapper

import me.apomazkin.core_db_api.entity.Quiz
import me.apomazkin.core_db_impl.entity.QuizDb

class QuizMapper : Mapper<QuizDb, Quiz>() {

    override fun map(value: QuizDb): Quiz {
        val definitionMapper = DefinitionMapper()
        return Quiz(
            value.wordDb.word,
            definitionMapper.map(value.definitionDb)
        )
    }

    override fun reverseMap(value: Quiz): QuizDb {
        TODO("no need")
    }
}