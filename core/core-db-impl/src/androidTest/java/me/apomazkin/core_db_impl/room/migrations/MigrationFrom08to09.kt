package me.apomazkin.core_db_impl.room.migrations

import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.room.Schema
import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.dataSource.DataProvider
import me.apomazkin.core_db_impl.room.schemable.LexemeV9
import me.apomazkin.core_db_impl.room.utils.checkCount
import me.apomazkin.core_db_impl.room.utils.checkData
import me.apomazkin.core_db_impl.room.utils.checkItems
import me.apomazkin.core_db_impl.room.utils.hasColumns
import me.apomazkin.core_db_impl.room.utils.hasTable
import me.apomazkin.core_db_impl.room.utils.toDatabase
import org.junit.Test

class MigrationFrom08to09 : BaseMigration() {

    override fun getMigrationClass() = migration_8_9
    override fun getCurrentVersion() = CURRENT_VERSION

    @Test
    fun from08to09() {
        runMigrateDbTest(
            onCreate = { database ->
                Schema.LexemeV1
                    .asContentValue(DataProvider.lexemeDbList)
                    .toDatabase(
                        database = database,
                        table = Schema.LexemeV1.tableName
                    )
            },
            afterCreateCheck = { database ->
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
            },
            afterMigrationCheck = { database ->
                database.hasTable(tableName = LexemeV9.tableName)
                database.hasColumns(
                    tableName = LexemeV9.tableName,
                    columns = LexemeV9.columnList
                )
                LexemeV9
                    .getFromDatabase(database)
                    .checkData(
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
            }
        )
    }

    companion object {
        private const val CURRENT_VERSION = 8
    }
}