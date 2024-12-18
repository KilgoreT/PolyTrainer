package me.apomazkin.core_db_impl.room.migrations

import androidx.test.ext.junit.runners.AndroidJUnit4
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.room.Schema
import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.dataSource.DataProvider
import me.apomazkin.core_db_impl.room.utils.checkCount
import me.apomazkin.core_db_impl.room.utils.checkItems
import me.apomazkin.core_db_impl.room.utils.hasColumns
import me.apomazkin.core_db_impl.room.utils.hasTable
import me.apomazkin.core_db_impl.room.utils.toDatabase
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom07to08 : BaseMigration() {

    override fun getMigrationClass() = migration_7_8
    override fun getCurrentVersion() = CURRENT_VERSION

    @Test
    fun from07to08() {
        runMigrateDbTest(
            onCreate = { database ->
                Schema.WordV2
                    .asContentValue(DataProvider.wordList)
                    .toDatabase(
                        database = database,
                        table = Schema.WordV2.tableName
                    )
                Schema.Definition
                    .asContentValue(DataProvider.lexemeDbList)
                    .toDatabase(
                        database = database,
                        table = Schema.Definition.tableName
                    )
                Schema.SampleV1
                    .asContentValue(DataProvider.sampleList)
                    .toDatabase(
                        database = database,
                        table = Schema.SampleV1.tableName
                    )
                Schema.HintV1
                    .asContentValue(DataProvider.hintList)
                    .toDatabase(
                        database = database,
                        table = Schema.HintV1.tableName
                    )
            },
            afterCreateCheck = { database ->
                database.hasTable(tableName = Schema.WordV2.tableName)
                database.hasColumns(
                    tableName = Schema.WordV2.tableName,
                    columns = with(Schema.WordV2) {
                        arrayOf(
                            columnId,
                            COLUMN_WORD,
                            COLUMN_ADD_DATE,
                            COLUMN_CHANGE_DATE,
                        )
                    }
                )
                Schema.WordV2
                    .getFromDatabase(database)
                    .checkCount(DataProvider.wordList)
                    .checkItems(
                        afterMigrationState = false,
                        origin = DataProvider.wordList,
                        originMatcher = { wordDb: WordDb ->
                            DataProvider.wordList.firstOrNull { wordDb.id == it.id }
                        },
                        checkMatcher = { migrated, origin ->
                            migrated.id == origin.id
                                    && migrated.langId == origin.langId
                                    && migrated.value == origin.value
                                    && migrated.addDate == origin.addDate
                                    && migrated.changeDate == origin.changeDate
                                    && migrated.removeDate == origin.removeDate
                        }
                    )

                database.hasTable(tableName = Schema.Definition.tableName)
                database.hasColumns(
                    tableName = Schema.Definition.tableName,
                    columns = with(Schema.Definition) {
                        arrayOf(
                            columnId,
                            COLUMN_WORD_ID,
                            COLUMN_DEFINITION,
                            COLUMN_WORD_CLASS,
                            COLUMN_OPTIONS,
                        )
                    }
                )
                Schema.Definition
                    .getFromDatabase(database)
                    .checkCount(DataProvider.lexemeDbList)
                    .checkItems(
                        afterMigrationState = false,
                        origin = DataProvider.lexemeDbList,
                        originMatcher = { lexemeDb: LexemeDb ->
                            DataProvider.lexemeDbList.firstOrNull { lexemeDb.id == it.id }
                        },
                        checkMatcher = { migrated, origin ->
                            migrated.id == origin.id
                                    && migrated.wordId == origin.wordId
                                    && migrated.definition == origin.definition
                                    && migrated.wordClass == origin.wordClass
                                    && migrated.options == origin.options
                        }
                    )
                database.hasTable(tableName = Schema.SampleV1.tableName)
                database.hasColumns(
                    tableName = Schema.SampleV1.tableName,
                    columns = with(Schema.SampleV1) {
                        arrayOf(
                            columnId,
                            COLUMN_DEFINITION_ID,
                            COLUMN_VALUE,
                            COLUMN_SOURCE,
                            COLUMN_ADD_DATE,
                            COLUMN_CHANGE_DATE,
                        )
                    }
                )
                Schema.SampleV1
                    .getFromDatabase(database)
                    .checkCount(DataProvider.sampleList)
                    .checkItems(
                        afterMigrationState = false,
                        origin = DataProvider.sampleList,
                        originMatcher = { sampleDb ->
                            DataProvider.sampleList.firstOrNull { sampleDb.id == it.id }
                        },
                        checkMatcher = { migrated, origin ->
                            migrated.id == origin.id
                                    && migrated.lexemeId == origin.lexemeId
                                    && migrated.value == origin.value
                                    && migrated.source == origin.source
                                    && migrated.addDate == origin.addDate
                                    && migrated.changeDate == origin.changeDate
                        }
                    )
                database.hasTable(tableName = Schema.HintV1.tableName)
                database.hasColumns(
                    tableName = Schema.HintV1.tableName,
                    columns = with(Schema.HintV1) {
                        arrayOf(
                            columnId,
                            COLUMN_DEFINITION_ID,
                            COLUMN_VALUE,
                            COLUMN_ADD_DATE,
                            COLUMN_CHANGE_DATE,
                        )
                    }
                )
                Schema.HintV1
                    .getFromDatabase(database)
                    .checkCount(DataProvider.hintList)
                    .checkItems(
                        afterMigrationState = false,
                        origin = DataProvider.hintList,
                        originMatcher = { hintDb ->
                            DataProvider.hintList.firstOrNull { hintDb.id == it.id }
                        },
                        checkMatcher = { migrated, origin ->
                            migrated.id == origin.id
                                    && migrated.lexemeId == origin.lexemeId
                                    && migrated.value == origin.value
                                    && migrated.addDate == origin.addDate
                                    && migrated.changeDate == origin.changeDate
                        }
                    )
            },
            afterMigrationCheck = { database ->

                database.hasTable(tableName = Schema.Word.tableName)
                database.hasColumns(
                    tableName = Schema.Word.tableName,
                    columns = with(Schema.Word) {
                        arrayOf(
                            columnId,
                            COLUMN_LANG_ID,
                            COLUMN_VALUE,
                            COLUMN_ADD_DATE,
                            COLUMN_CHANGE_DATE,
                            COLUMN_REMOVE_DATE,
                        )
                    }
                )
                Schema.Word
                    .getFromDatabase(database)
                    .checkCount(DataProvider.wordList)
                    .checkItems(
                        origin = DataProvider.wordList,
                        originMatcher = { wordDb: WordDb ->
                            DataProvider.wordList.firstOrNull { wordDb.id == it.id }
                        },
                        checkMatcher = { migrated, origin ->
                            migrated.value == origin.value
                                    && migrated.langId == origin.langId
                        }
                    )

                database.hasTable(tableName = Schema.LexemeV1.tableName)
                database.hasColumns(
                    tableName = Schema.LexemeV1.tableName,
                    columns = with(Schema.LexemeV1) {
                        arrayOf(
                            columnId,
                            COLUMN_WORD_ID,
                            COLUMN_TRANSLATION,
                            COLUMN_DEFINITION,
                            COLUMN_WORD_CLASS,
                            COLUMN_OPTIONS,
                        )
                    }
                )
                Schema.LexemeV1
                    .getFromDatabase(database)
                    .checkCount(DataProvider.lexemeDbList)
                    .checkItems(
                        origin = DataProvider.lexemeDbList,
                        originMatcher = { migrated ->
                            DataProvider.lexemeDbList.firstOrNull { migrated.id == it.id }
                        },
                        checkMatcher = { migrated, origin ->
                            migrated.id == origin.id
                                    && migrated.wordId == origin.wordId
                                    && migrated.definition == origin.definition
                                    && migrated.wordClass == origin.wordClass
                                    && migrated.options == origin.options
                        }
                    )
                database.hasTable(tableName = Schema.Sample.tableName)
                database.hasColumns(
                    tableName = Schema.Sample.tableName,
                    columns = with(Schema.Sample) {
                        arrayOf(
                            columnId,
                            COLUMN_LEXEME_ID,
                            COLUMN_VALUE,
                            COLUMN_SOURCE,
                            COLUMN_ADD_DATE,
                            COLUMN_CHANGE_DATE,
                            COLUMN_REMOVE_DATE,
                        )
                    }
                )
                Schema.Sample
                    .getFromDatabase(database)
                    .checkCount(DataProvider.sampleList)
                    .checkItems(
                        origin = DataProvider.sampleList,
                        originMatcher = { migrated ->
                            DataProvider.sampleList.firstOrNull { migrated.id == it.id }
                        },
                        checkMatcher = { migrated, origin ->
                            migrated.id == origin.id
                                    && migrated.lexemeId == origin.lexemeId
                                    && migrated.value == origin.value
                                    && migrated.source == origin.source
                                    && migrated.addDate == origin.addDate
                                    && migrated.changeDate == origin.changeDate
                                    && migrated.removeDate == origin.removeDate
                        }
                    )
                database.hasTable(tableName = Schema.Hint.tableName)
                database.hasColumns(
                    tableName = Schema.Hint.tableName,
                    columns = with(Schema.Hint) {
                        arrayOf(
                            columnId,
                            COLUMN_LEXEME_ID,
                            COLUMN_VALUE,
                            COLUMN_ADD_DATE,
                            COLUMN_CHANGE_DATE,
                            COLUMN_REMOVE_DATE
                        )
                    }
                )
                Schema.Hint
                    .getFromDatabase(database)
                    .checkCount(DataProvider.hintList)
                    .checkItems(
                        origin = DataProvider.hintList,
                        originMatcher = { migrated ->
                            DataProvider.hintList.firstOrNull { migrated.id == it.id }
                        },
                        checkMatcher = { migrated, origin ->
                            migrated.id == origin.id
                                    && migrated.lexemeId == origin.lexemeId
                                    && migrated.value == origin.value
                                    && migrated.addDate == origin.addDate
                                    && migrated.changeDate == origin.changeDate
                        }
                    )
            }
        )
    }

    companion object {
        private const val CURRENT_VERSION = 7
    }
}