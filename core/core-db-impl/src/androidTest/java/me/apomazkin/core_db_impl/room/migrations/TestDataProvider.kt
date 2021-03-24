package me.apomazkin.core_db_impl.room.migrations

import android.content.ContentValues
import me.apomazkin.core_db_impl.entity.DefinitionDb

class TestDataProvider {
    companion object {
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

        fun getDefinitionListAsContentValues() = definitionList.map { definition ->
            ContentValues().apply {
                put("id", definition.id)
                put("wordId", definition.wordClass)
                put("definition", definition.definition)
                put("wordClass", definition.wordClass)
                put("options", definition.options)
            }
        }

    }
}