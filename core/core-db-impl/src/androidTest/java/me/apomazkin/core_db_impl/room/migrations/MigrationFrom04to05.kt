package me.apomazkin.core_db_impl.room.migrations

import androidx.test.ext.junit.runners.AndroidJUnit4
import me.apomazkin.core_db_impl.room.Schema
import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.dataSource.DataProvider
import me.apomazkin.core_db_impl.room.utils.checkCount
import me.apomazkin.core_db_impl.room.utils.toDatabase
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom04to05 : BaseMigration() {

    override fun getMigrationClass() = migration_4_5
    override fun getCurrentVersion() = CURRENT_VERSION

    @Test
    fun from04to05() {
        runMigrateDbTest(
            onCreate = { database ->
                Schema.WordV1
                    .asContentValue(DataProvider.wordList)
                    .toDatabase(
                        database = database,
                        table = Schema.WordV1.tableName
                    )
                Schema.WriteQuizV1
                    .asContentValue(DataProvider.writeQuizList)
                    .toDatabase(
                        database = database,
                        table = Schema.WriteQuizV1.tableName
                    )
            },
            afterCreateCheck = { database ->
                Schema.WordV1
                    .getFromDatabase(database)
                    .checkCount(DataProvider.wordList)
                Schema.WriteQuizV1
                    .getFromDatabase(database)
                    .checkCount(DataProvider.writeQuizList)
            },
            afterMigrationCheck = { database ->
                Schema.WordV2
                    .getFromDatabase(database)
                    .forEach { item ->
                        Assert.assertTrue(
                            "WordDb.langId must be 0, but it is ${item.langId}",
                            DataProvider.languageList.first().id?.let { item.langId == it } ?: false
                        )
                    }
                Schema.WriteQuizV1
                    .getFromDatabase(database)
                    .forEach { item ->
                        Assert.assertTrue(
                            "WriteQuizDb.langId must be 0, but it is ${item.langId}",
                            DataProvider.languageList.first().id?.let { item.langId == it } ?: false
                        )
                    }
            }
        )
    }

    companion object {
        private const val CURRENT_VERSION = 4
    }
}