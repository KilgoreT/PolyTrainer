package me.apomazkin.core_db_impl.room.migrations

import androidx.test.ext.junit.runners.AndroidJUnit4
import me.apomazkin.core_db_impl.room.Schema
import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.dataSource.DataProvider
import me.apomazkin.core_db_impl.room.utils.checkCount
import me.apomazkin.core_db_impl.room.utils.toDatabase
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom05to06 : BaseMigration() {

    override fun getMigrationClass() = migration_5_6
    override fun getCurrentVersion() = CURRENT_VERSION

    @Test
    fun from05to06() {
        runMigrateDbTest(
            onCreate = { database ->
                Schema.WordV2
                    .asContentValue(DataProvider.wordList)
                    .toDatabase(
                        database = database,
                        table = Schema.WordV2.tableName
                    )
                Schema.WriteQuiz
                    .asContentValue(DataProvider.writeQuizList)
                    .toDatabase(
                        database = database,
                        table = Schema.WriteQuizV1.tableName
                    )
                Schema.LanguagesV1
                    .asContentValue(DataProvider.languageList)
                    .toDatabase(
                        database = database,
                        table = Schema.LanguagesV1.tableName
                    )
            },
            afterCreateCheck = { database ->
                Schema.WordV2
                    .getFromDatabase(database)
                    .checkCount(DataProvider.wordList)
                Schema.WriteQuiz
                    .getFromDatabase(database)
                    .checkCount(DataProvider.writeQuizList)
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