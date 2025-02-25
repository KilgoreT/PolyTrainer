package me.apomazkin.core_db_impl.room.migrations

import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.schemable.LexemeDbV9
import me.apomazkin.core_db_impl.room.schemable.LexemeV10
import me.apomazkin.core_db_impl.room.schemable.LexemeV9
import me.apomazkin.core_db_impl.room.schemable.WordDbV8
import me.apomazkin.core_db_impl.room.schemable.WordV8
import me.apomazkin.core_db_impl.room.schemable.WriteQuizDbV5
import me.apomazkin.core_db_impl.room.schemable.WriteQuizV10
import me.apomazkin.core_db_impl.room.schemable.WriteQuizV5
import me.apomazkin.core_db_impl.room.utils.checkData
import me.apomazkin.core_db_impl.room.utils.hasColumns
import me.apomazkin.core_db_impl.room.utils.hasTable
import me.apomazkin.core_db_impl.room.utils.toDatabase
import org.junit.Test

class MigrationFrom09to10 : BaseMigration() {
    
    override fun getMigrationClass() = migration_9_10
    override fun getCurrentVersion() = CURRENT_VERSION
    
    @Test
    fun from09to10() {
        runMigrateDbTest(
            onCreate = { database ->
                WordV8
                    .asContentValue(WordV8.data())
                    .toDatabase(
                        database = database,
                        table = WordV8.tableName
                    )
                LexemeV9
                    .asContentValue(LexemeV9.data())
                    .toDatabase(
                        database = database,
                        table = LexemeV9.tableName
                    )
                WriteQuizV5
                    .asContentValue(WriteQuizV5.data())
                    .toDatabase(
                        database = database,
                        table = WriteQuizV5.tableName
                    )
            },
            afterCreateCheck = { database ->
                database.hasTable(tableName = WordV8.tableName)
                database.hasColumns(
                    tableName = WordV8.tableName,
                    columns = WordV8.columnList
                )
                WordV8
                    .getFromDatabase(database)
                    .checkData(
                        afterMigrationState = false,
                        origin = WordV8.data(),
                        originMatcher = { wordDb: WordDbV8 ->
                            WordV8
                                .data()
                                .firstOrNull { wordDb.id == it.id }
                        },
                        checkMatcher = { inDb, origin ->
                            inDb.id == origin.id
                                    && inDb.langId == origin.langId
                                    && inDb.value == origin.value
                            //                                    && inDb.addDate == origin.addDate
                            //                                    && inDb.changeDate == origin.changeDate
                            //                                    && inDb.removeDate == origin.removeDate
                        }
                    )
                
                database.hasTable(tableName = LexemeV9.tableName)
                database.hasColumns(
                    tableName = LexemeV9.tableName,
                    columns = LexemeV9.columnList
                )
                LexemeV9
                    .getFromDatabase(database)
                    .checkData(
                        afterMigrationState = false,
                        origin = LexemeV9.data(),
                        originMatcher = { lexemeDb: LexemeDbV9 ->
                            LexemeV9
                                .data()
                                .firstOrNull { lexemeDb.id == it.id }
                        },
                        checkMatcher = { inDb, origin ->
                            inDb.id == origin.id
                                    && inDb.wordId == origin.wordId
                                    && inDb.translation == origin.translation
                                    && inDb.definition == origin.definition
                                    && inDb.wordClass == origin.wordClass
                                    && inDb.options == origin.options
                        }
                    )
                
                database.hasTable(tableName = WriteQuizV5.tableName)
                database.hasColumns(
                    tableName = WriteQuizV5.tableName,
                    columns = WriteQuizV5.columnList
                )
                WriteQuizV5
                    .getFromDatabase(database)
                    .checkData(
                        afterMigrationState = false,
                        origin = WriteQuizV5.data(),
                        originMatcher = { wordDb: WriteQuizDbV5 ->
                            WriteQuizV5
                                .data()
                                .firstOrNull { wordDb.id == it.id }
                        },
                        checkMatcher = { inDb, origin ->
                            inDb.id == origin.id
                                    && inDb.langId == origin.langId
                                    && inDb.definitionId == origin.definitionId
                                    && inDb.grade == origin.grade
                                    && inDb.score == origin.score
                        }
                    )
            },
            afterMigrationCheck = { database ->
                database.hasTable(tableName = LexemeV10.tableName)
                database.hasColumns(
                    tableName = LexemeV10.tableName,
                    columns = LexemeV10.columnList
                )
                LexemeV10
                    .getFromDatabase(database)
                    .checkData(
                        origin = LexemeV9.data(),
                        originMatcher = { lexemeDb: LexemeDb ->
                            LexemeV9
                                .data()
                                .firstOrNull { lexemeDb.id == it.id }
                        },
                        checkMatcher = { migrated, origin ->
                            migrated.id == origin.id
                                    && migrated.wordId == origin.wordId
                                    && migrated.translation == origin.translation
                                    && migrated.definition == origin.definition
                                    && migrated.wordClass == origin.wordClass
                                    && migrated.options == origin.options
                        }
                    )
                
                database.hasTable(tableName = WriteQuizV10.tableName)
                database.hasColumns(
                    tableName = WriteQuizV10.tableName,
                    columns = WriteQuizV10.columnList,
                )
                WriteQuizV10
                    .getFromDatabase(database)
                    .checkData(
                        origin = WriteQuizV5.data(),
                        originMatcher = { writeQuiz: WriteQuizDb ->
                            WriteQuizV5
                                .data()
                                .firstOrNull { writeQuiz.id == it.id }
                        },
                        checkMatcher = { migrated: WriteQuizDb, origin: WriteQuizDbV5 ->
                            migrated.id == origin.id
                                    && migrated.langId == origin.langId
                                    && migrated.lexemeId == origin.definitionId
                                    && migrated.grade == origin.grade
                                    && migrated.score == origin.score
                                    && migrated.errorCount == 0
                        }
                    )
            }
        )
    }
    
    companion object {
        private const val CURRENT_VERSION = 9
    }
}