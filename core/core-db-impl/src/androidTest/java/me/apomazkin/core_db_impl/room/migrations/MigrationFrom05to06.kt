package me.apomazkin.core_db_impl.room.migrations

import me.apomazkin.core_db_impl.room.Schema
import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.dataSource.DataProvider
import me.apomazkin.core_db_impl.room.schemable.WordV5
import me.apomazkin.core_db_impl.room.schemable.WriteQuizV5
import me.apomazkin.core_db_impl.room.utils.checkCount
import me.apomazkin.core_db_impl.room.utils.toDatabase
import org.junit.Test

class MigrationFrom05to06 : BaseMigration() {

    override fun getMigrationClass() = migration_5_6
    override fun getCurrentVersion() = CURRENT_VERSION

    @Test
    fun from05to06() {
        runMigrateDbTest(
            onCreate = { database ->
                WordV5
                    .asContentValue(WordV5.data())
                    .toDatabase(
                        database = database,
                        table = WordV5.tableName
                    )
                WriteQuizV5
                    .asContentValue(WriteQuizV5.data())
                    .toDatabase(
                        database = database,
                        table = WriteQuizV5.tableName
                    )
                Schema.LanguagesV1
                    .asContentValue(DataProvider.languageList)
                    .toDatabase(
                        database = database,
                        table = Schema.LanguagesV1.tableName
                    )
            },
            afterCreateCheck = { database ->
                WordV5
                    .getFromDatabase(database)
                    .checkCount(WordV5.data())
                WriteQuizV5
                    .getFromDatabase(database)
                    .checkCount(WriteQuizV5.data())
                Schema.LanguagesV1
                    .getFromDatabase(database)
                    .checkCount(DataProvider.languageList)
            },
            afterMigrationCheck = { database ->
                Schema.Languages
                    .getFromDatabase(database)
                    .checkCount(DataProvider.languageList)
            }
        )
    }

    companion object {
        private const val CURRENT_VERSION = 5
    }
}