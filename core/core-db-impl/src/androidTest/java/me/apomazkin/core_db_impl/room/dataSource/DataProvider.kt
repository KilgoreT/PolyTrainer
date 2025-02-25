package me.apomazkin.core_db_impl.room.dataSource

import me.apomazkin.core_db_impl.entity.HintDb
import me.apomazkin.core_db_impl.entity.LanguageDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.SampleDb
import java.util.Date

class DataProvider {
    companion object {
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

        val lexemeDbList = listOf<LexemeDb>(
            LexemeDb(
                id = 0L,
                wordId = 0L,
                definition = "Definition Example 0",
                wordClass = "Word Class 0",
                options = 0L,
                addDate = Date(0)
            ),
            LexemeDb(
                id = 1L,
                wordId = 1L,
                definition = "Definition Example 1",
                wordClass = "Word Class 1",
                options = 0L,
                addDate = Date(0)
            ),
            LexemeDb(
                id = 2L,
                wordId = 2L,
                definition = "Definition Example 2",
                wordClass = "Word Class 2",
                options = 0L,
                addDate = Date(0)
            ),
            LexemeDb(
                id = 3L,
                wordId = 2L,
                definition = "Definition Example 3",
                wordClass = "Word Class 3",
                options = 0L,
                addDate = Date(0)
            ),
        )

        val hintList = listOf<HintDb>(
            HintDb(
                id = 0,
                lexemeId = 7,
                value = "000",
                addDate = Date(System.currentTimeMillis()),
                removeDate = Date(System.currentTimeMillis()),
            ),
            HintDb(
                id = 1,
                lexemeId = 7,
                value = "111",
                addDate = Date(System.currentTimeMillis()),
                changeDate = Date(System.currentTimeMillis()),
            ),
            HintDb(
                id = 2,
                lexemeId = 7,
                value = "222",
                addDate = Date(System.currentTimeMillis())
            ),
        )

        val sampleList = listOf<SampleDb>(
            SampleDb(
                id = 1,
                lexemeId = 1,
                value = "111",
                source = "source 1",
                addDate = Date(System.currentTimeMillis()),
            ),
            SampleDb(
                id = 2,
                lexemeId = 1,
                value = "222",
                source = "source 2",
                addDate = Date(System.currentTimeMillis()),
                changeDate = Date(System.currentTimeMillis()),
            ),
            SampleDb(
                id = 3,
                lexemeId = 1,
                value = "333",
                source = "source 3",
                addDate = Date(System.currentTimeMillis()),
            ),
            SampleDb(
                id = 4,
                lexemeId = 1,
                value = "444",
                source = "source 4",
                addDate = Date(System.currentTimeMillis()),
            ),
        )
    }
}