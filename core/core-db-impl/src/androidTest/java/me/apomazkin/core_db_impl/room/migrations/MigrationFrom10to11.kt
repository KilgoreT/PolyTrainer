package me.apomazkin.core_db_impl.room.migrations

import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.room.Schema
import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.dataSource.DataProvider
import me.apomazkin.core_db_impl.room.schemable.DictionaryDbV11
import me.apomazkin.core_db_impl.room.schemable.DictionaryV11
import me.apomazkin.core_db_impl.room.schemable.LexemeV10
import me.apomazkin.core_db_impl.room.schemable.WordDbV10
import me.apomazkin.core_db_impl.room.schemable.WordDbV11
import me.apomazkin.core_db_impl.room.schemable.WordV10
import me.apomazkin.core_db_impl.room.schemable.WordV11
import me.apomazkin.core_db_impl.room.schemable.WriteQuizDbV11
import me.apomazkin.core_db_impl.room.schemable.WriteQuizV10
import me.apomazkin.core_db_impl.room.schemable.WriteQuizV11
import me.apomazkin.core_db_impl.room.utils.checkData
import me.apomazkin.core_db_impl.room.utils.hasColumn
import me.apomazkin.core_db_impl.room.utils.hasColumns
import me.apomazkin.core_db_impl.room.utils.hasTable
import me.apomazkin.core_db_impl.room.utils.toDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Test cases:
 * 1. Standard case: languages table renamed to dictionaries, data preserved
 * 2. Standard case: words.lang_id renamed to words.dictionary_id, data preserved
 * 3. Standard case: write_quiz.lang_id renamed to write_quiz.dictionary_id, data preserved
 * 4. Standard case: lexemes table unchanged, data preserved
 * 5. Negative case: old table "languages" does not exist after migration
 * 6. Negative case: column "lang_id" does not exist in words after migration
 * 7. Negative case: column "lang_id" does not exist in write_quiz after migration
 * 8. Negative case: column "code" does not exist in dictionaries (removed field)
 * 9. Edge case: empty tables — migration does not crash
 * 10. Standard case: row count preserved for all tables
 * 11. Standard case: FK CASCADE — delete dictionary cascades to words
 * 12. Standard case: index on words.dictionary_id exists after migration
 * 13. Negative case: old index index_words_lang_id does not exist
 * 14. Edge case: language with null name — migrates without crash
 */
class MigrationFrom10to11 : BaseMigration() {

    override fun getMigrationClass() = migration_10_11
    override fun getCurrentVersion() = CURRENT_VERSION

    @Test
    fun from10to11() {
        runMigrateDbTest(
            // Phase 1: Insert test data into v10 schema
            onCreate = { database ->
                Schema.Languages
                    .asContentValue(DataProvider.languageList)
                    .toDatabase(
                        database = database,
                        table = Schema.Languages.tableName
                    )
                WordV10
                    .asContentValue(WordV10.data())
                    .toDatabase(
                        database = database,
                        table = WordV10.tableName
                    )
                LexemeV10
                    .asContentValue(LexemeV10.data())
                    .toDatabase(
                        database = database,
                        table = LexemeV10.tableName
                    )
                WriteQuizV10
                    .asContentValue(WriteQuizV10.data())
                    .toDatabase(
                        database = database,
                        table = WriteQuizV10.tableName
                    )
            },

            // Phase 2: Verify data before migration
            afterCreateCheck = { database ->
                // languages table exists with correct columns
                database.hasTable(tableName = "languages")
                database.hasColumns(
                    tableName = "languages",
                    columns = arrayOf(
                        "id", "numericCode", "code", "name", "addDate", "changeDate"
                    )
                )

                // words table with lang_id column
                database.hasTable(tableName = WordV10.tableName)
                database.hasColumns(
                    tableName = WordV10.tableName,
                    columns = WordV10.columnList
                )
                WordV10
                    .getFromDatabase(database)
                    .checkData(
                        afterMigrationState = false,
                        origin = WordV10.data(),
                        originMatcher = { wordDb: WordDbV10 ->
                            WordV10.data().firstOrNull { wordDb.id == it.id }
                        },
                        checkMatcher = { inDb, origin ->
                            inDb.id == origin.id
                                    && inDb.langId == origin.langId
                                    && inDb.value == origin.value
                        }
                    )

                // write_quiz table with lang_id column
                database.hasTable(tableName = WriteQuizV10.tableName)
                database.hasColumns(
                    tableName = WriteQuizV10.tableName,
                    columns = WriteQuizV10.columnList
                )

                // lexemes table
                database.hasTable(tableName = LexemeV10.tableName)
            },

            // Phase 3: Verify data after migration
            afterMigrationCheck = { database ->

                // === Test case 1: languages → dictionaries, data preserved ===
                database.hasTable(tableName = DictionaryV11.tableName)
                database.hasColumns(
                    tableName = DictionaryV11.tableName,
                    columns = DictionaryV11.columnList
                )
                val dictionaries = DictionaryV11.getFromDatabase(database)
                dictionaries.checkData(
                    origin = DataProvider.languageList,
                    originMatcher = { dict: DictionaryDbV11 ->
                        DataProvider.languageList.firstOrNull { dict.id == it.id }
                    },
                    checkMatcher = { migrated, origin ->
                        migrated.id == origin.id
                                && migrated.numericCode == origin.numericCode
                                && migrated.name == origin.name
                    }
                )

                // === Test case 2: words.lang_id → words.dictionary_id, data preserved ===
                database.hasTable(tableName = WordV11.tableName)
                database.hasColumns(
                    tableName = WordV11.tableName,
                    columns = WordV11.columnList
                )
                val words = WordV11.getFromDatabase(database)
                words.checkData(
                    origin = WordV10.data(),
                    originMatcher = { word: WordDbV11 ->
                        WordV10.data().firstOrNull { word.id == it.id }
                    },
                    checkMatcher = { migrated, origin ->
                        migrated.id == origin.id
                                && migrated.dictionaryId == origin.langId
                                && migrated.value == origin.value
                    }
                )

                // === Test case 3: write_quiz.lang_id → write_quiz.dictionary_id ===
                database.hasTable(tableName = WriteQuizV11.tableName)
                database.hasColumns(
                    tableName = WriteQuizV11.tableName,
                    columns = WriteQuizV11.columnList
                )
                val quizzes = WriteQuizV11.getFromDatabase(database)
                quizzes.checkData(
                    origin = WriteQuizV10.data(),
                    originMatcher = { quiz: WriteQuizDbV11 ->
                        WriteQuizV10.data().firstOrNull { quiz.id == it.id }
                    },
                    checkMatcher = { migrated, origin ->
                        migrated.id == origin.id
                                && migrated.dictionaryId == origin.langId
                                && migrated.lexemeId == origin.lexemeId
                                && migrated.grade == origin.grade
                                && migrated.score == origin.score
                                && migrated.errorCount == origin.errorCount
                    }
                )

                // === Test case 4: lexemes table unchanged ===
                database.hasTable(tableName = LexemeV10.tableName)
                database.hasColumns(
                    tableName = LexemeV10.tableName,
                    columns = LexemeV10.columnList
                )
                val lexemes = LexemeV10.getFromDatabase(database)
                lexemes.checkData(
                    origin = LexemeV10.data(),
                    originMatcher = { lexeme: LexemeDb ->
                        LexemeV10.data().firstOrNull { lexeme.id == it.id }
                    },
                    checkMatcher = { migrated, origin ->
                        migrated.id == origin.id
                                && migrated.wordId == origin.wordId
                                && migrated.translation == origin.translation
                                && migrated.definition == origin.definition
                    }
                )

                // === Test case 5: old table "languages" does NOT exist ===
                val cursor = database.query(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='languages'"
                )
                assertFalse(
                    "Table 'languages' should not exist after migration",
                    cursor.moveToFirst()
                )

                // === Test case 6: column "lang_id" does NOT exist in words ===
                val wordColumns = getColumnNames(database, "words")
                assertFalse(
                    "Column 'lang_id' should not exist in words",
                    wordColumns.contains("lang_id")
                )
                assertTrue(
                    "Column 'dictionary_id' should exist in words",
                    wordColumns.contains("dictionary_id")
                )

                // === Test case 7: column "lang_id" does NOT exist in write_quiz ===
                val quizColumns = getColumnNames(database, "write_quiz")
                assertFalse(
                    "Column 'lang_id' should not exist in write_quiz",
                    quizColumns.contains("lang_id")
                )
                assertTrue(
                    "Column 'dictionary_id' should exist in write_quiz",
                    quizColumns.contains("dictionary_id")
                )

                // === Test case 8: column "code" does NOT exist in dictionaries ===
                val dictColumns = getColumnNames(database, "dictionaries")
                assertFalse(
                    "Column 'code' should not exist in dictionaries",
                    dictColumns.contains("code")
                )

                // === Test case 10: row count preserved ===
                assertEquals(
                    "Dictionaries count should match languages count",
                    DataProvider.languageList.size,
                    dictionaries.size
                )
                assertEquals(
                    "Words count should be preserved",
                    WordV10.data().size,
                    words.size
                )
                assertEquals(
                    "WriteQuiz count should be preserved",
                    WriteQuizV10.data().size,
                    quizzes.size
                )
                assertEquals(
                    "Lexemes count should be preserved",
                    LexemeV10.data().size,
                    lexemes.size
                )

                // === Test case 11: FK CASCADE — delete dictionary → words deleted ===
                database.execSQL("PRAGMA foreign_keys = ON")
                val dictId = dictionaries.first().id
                val wordsBeforeDelete = words.count { it.dictionaryId == dictId }
                assertTrue(
                    "Should have words for dictId=$dictId before delete",
                    wordsBeforeDelete > 0
                )
                database.execSQL("DELETE FROM dictionaries WHERE id = $dictId")
                val wordsAfterDelete = WordV11.getFromDatabase(database)
                val remainingWordsForDict = wordsAfterDelete.filter {
                    it.dictionaryId == dictId
                }
                assertTrue(
                    "Words for deleted dictionary should be cascade-deleted",
                    remainingWordsForDict.isEmpty()
                )

                // === Test case 12: index on dictionary_id exists in words ===
                val indexExists = hasIndex(database, "words", "dictionary_id")
                assertTrue(
                    "Index on words.dictionary_id should exist",
                    indexExists
                )

                // === Test case 13: old index index_words_lang_id does NOT exist ===
                val oldIndexExists = hasIndexByName(
                    database, "index_words_lang_id"
                )
                assertFalse(
                    "Old index index_words_lang_id should not exist",
                    oldIndexExists
                )
            }
        )
    }

    @Test
    fun from10to11_emptyTables() {
        // Test case 9: empty tables — migration does not crash
        runMigrateDbTest(
            onCreate = { _ ->
                // Insert nothing — all tables empty
            },
            afterCreateCheck = { database ->
                database.hasTable(tableName = "languages")
                database.hasTable(tableName = "words")
                database.hasTable(tableName = "write_quiz")
            },
            afterMigrationCheck = { database ->
                database.hasTable(tableName = "dictionaries")
                database.hasTable(tableName = "words")
                database.hasTable(tableName = "write_quiz")

                val dictionaries = DictionaryV11.getFromDatabase(database)
                assertTrue(
                    "Dictionaries should be empty",
                    dictionaries.isEmpty()
                )
                val words = WordV11.getFromDatabase(database)
                assertTrue(
                    "Words should be empty",
                    words.isEmpty()
                )
                val quizzes = WriteQuizV11.getFromDatabase(database)
                assertTrue(
                    "WriteQuiz should be empty",
                    quizzes.isEmpty()
                )
            }
        )
    }

    @Test
    fun from10to11_nullName() {
        // Test case 14: language with null name — migrates without crash
        runMigrateDbTest(
            onCreate = { database ->
                // Insert first language normally
                Schema.Languages
                    .asContentValue(listOf(DataProvider.languageList[0]))
                    .toDatabase(database = database, table = Schema.Languages.tableName)
                // Insert second language with null name via raw ContentValues
                val nullNameLang = android.content.ContentValues().apply {
                    put("id", 1L)
                    put("numericCode", 2)
                    put("code", "ru")
                    putNull("name")
                    put("addDate", System.currentTimeMillis())
                    putNull("changeDate")
                }
                database.insert("languages", android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL, nullNameLang)
            },
            afterCreateCheck = { database ->
                database.hasTable(tableName = "languages")
            },
            afterMigrationCheck = { database ->
                database.hasTable(tableName = "dictionaries")
                val dictionaries = DictionaryV11.getFromDatabase(database)
                assertEquals(
                    "Should have 2 dictionaries",
                    2,
                    dictionaries.size
                )
                val withNullName = dictionaries.first { it.id == 1L }
                assertEquals(
                    "Null name should migrate as empty string",
                    "",
                    withNullName.name
                )
            }
        )
    }

    private fun hasIndex(
        database: androidx.sqlite.db.SupportSQLiteDatabase,
        tableName: String,
        columnName: String,
    ): Boolean {
        val cursor = database.query("PRAGMA index_list($tableName)")
        cursor.use {
            while (it.moveToNext()) {
                val indexName = it.getString(it.getColumnIndexOrThrow("name"))
                val indexInfo = database.query("PRAGMA index_info($indexName)")
                indexInfo.use { info ->
                    while (info.moveToNext()) {
                        val col = info.getString(info.getColumnIndexOrThrow("name"))
                        if (col == columnName) return true
                    }
                }
            }
        }
        return false
    }

    private fun hasIndexByName(
        database: androidx.sqlite.db.SupportSQLiteDatabase,
        indexName: String,
    ): Boolean {
        val cursor = database.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND name=?",
            arrayOf(indexName)
        )
        return cursor.use { it.moveToFirst() }
    }

    private fun getColumnNames(
        database: androidx.sqlite.db.SupportSQLiteDatabase,
        tableName: String,
    ): Set<String> {
        val columns = mutableSetOf<String>()
        val cursor = database.query("PRAGMA table_info($tableName)")
        cursor.use {
            while (it.moveToNext()) {
                columns.add(it.getString(it.getColumnIndexOrThrow("name")))
            }
        }
        return columns
    }

    companion object {
        private const val CURRENT_VERSION = 10
    }
}
