package me.apomazkin.core_db_impl.room.migrations

import android.content.ContentValues
import me.apomazkin.core_db_impl.entity.DefinitionDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb

class TestDataProvider {
    companion object {
        val wordList = listOf(
            WordDb(
                id = 0L,
                word = "w000",
            ),
            WordDb(
                id = 1L,
                word = "w111",
            ),
            WordDb(
                id = 2L,
                word = "w222",
            ),
            WordDb(
                id = 3L,
                word = "w333",
            ),
            WordDb(
                id = 4L,
                word = "w444",
            ),
        )

        val definitionList = listOf<DefinitionDb>(
            DefinitionDb(
                id = 0L,
                wordId = 0L,
                definition = "Definition Example 0",
                wordClass = "Word Class 0",
                options = 0L
            ),
            DefinitionDb(
                id = 1L,
                wordId = 1L,
                definition = "Definition Example 1",
                wordClass = "Word Class 1",
                options = 0L
            ),
            DefinitionDb(
                id = 2L,
                wordId = 2L,
                definition = "Definition Example 2",
                wordClass = "Word Class 2",
                options = 0L
            ),
            DefinitionDb(
                id = 3L,
                wordId = 2L,
                definition = "Definition Example 3",
                wordClass = "Word Class 3",
                options = 0L
            ),
        )

        val writeQuizList = listOf<WriteQuizDb>(
            WriteQuizDb(
                id = 0,
                definitionId = 0,
                grade = 0,
                score = 0,
            ),
            WriteQuizDb(
                id = 1,
                definitionId = 1,
                grade = 0,
                score = 0,
            ),
            WriteQuizDb(
                id = 2,
                definitionId = 2,
                grade = 0,
                score = 0,
            )
        )

        fun getWordListAsContentValue() = wordList.map { word ->
            ContentValues().apply {
                put("id", word.id)
                put("word", word.word)
            }
        }

        fun getDefinitionListAsContentValues() = definitionList.map { definition ->
            ContentValues().apply {
                put("id", definition.id)
                put("wordId", definition.wordClass)
                put("definition", definition.definition)
                put("wordClass", definition.wordClass)
                put("options", definition.options)
            }
        }

        fun getWordQuizAsContentValue() = writeQuizList.map { writeQuizDb ->
            ContentValues().apply {
                put("id", writeQuizDb.id)
                put("definitionId", writeQuizDb.definitionId)
                put("grade", writeQuizDb.grade)
                put("score", writeQuizDb.score)
            }
        }

    }
}