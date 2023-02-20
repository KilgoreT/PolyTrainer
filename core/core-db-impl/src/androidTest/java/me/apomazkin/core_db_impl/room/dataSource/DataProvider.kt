package me.apomazkin.core_db_impl.room.dataSource

import me.apomazkin.core_db_impl.entity.*
import java.util.*

class DataProvider {
    companion object {
        val wordList = listOf(
            WordDb(
                id = 0L,
                langId = 0,
                word = "w000",
            ),
            WordDb(
                id = 1L,
                langId = 0,
                word = "w111",
            ),
            WordDb(
                id = 2L,
                langId = 0,
                word = "w222",
            ),
            WordDb(
                id = 3L,
                langId = 0,
                word = "w333",
            ),
            WordDb(
                id = 4L,
                langId = 0,
                word = "w444",
            ),
        )

        val languageList = listOf(
            LanguageDb(
                id = 0,
                numericCode = 1,
                code = "en",
                name = "English",
                addDate = Date(System.currentTimeMillis())
            ),
            LanguageDb(
                id = 1,
                numericCode = 2,
                code = "ru",
                name = "Russia",
                addDate = Date(System.currentTimeMillis())
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
                langId = 0,
                definitionId = 0,
                grade = 0,
                score = 0,
            ),
            WriteQuizDb(
                id = 1,
                langId = 0,
                definitionId = 1,
                grade = 0,
                score = 0,
            ),
            WriteQuizDb(
                id = 2,
                langId = 0,
                definitionId = 2,
                grade = 0,
                score = 0,
            )
        )

        val hintList = listOf<HintDb>(
            HintDb(
                id = 0,
                definitionId = 7,
                value = "000",
                addDate = Date(System.currentTimeMillis())
            ),
            HintDb(
                id = 1,
                definitionId = 7,
                value = "111",
                addDate = Date(System.currentTimeMillis())
            ),
            HintDb(
                id = 2,
                definitionId = 7,
                value = "222",
                addDate = Date(System.currentTimeMillis())
            ),
        )

        val sampleList = listOf<SampleDb>(
            SampleDb(
                id = 1,
                definitionId = 1,
                value = "111",
                source = "source 1",
                addDate = Date(System.currentTimeMillis())
            ),
            SampleDb(
                id = 2,
                definitionId = 1,
                value = "222",
                source = "source 2",
                addDate = Date(System.currentTimeMillis())
            ),
            SampleDb(
                id = 3,
                definitionId = 1,
                value = "333",
                source = "source 3",
                addDate = Date(System.currentTimeMillis())
            ),
            SampleDb(
                id = 4,
                definitionId = 1,
                value = "444",
                source = "source 4",
                addDate = Date(System.currentTimeMillis())
            ),
        )
    }
}