package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
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
class MigrationFrom02to03 : BaseMigration() {

    override fun getMigrationClass(): Migration = migration_2_3
    override fun getCurrentVersion(): Int = CURRENT_VERSION

    @Test
    fun from02to03() {
        runMigrateDbTest(
            onCreate = { db ->
                Schema.WordV1
                    .asContentValue(DataProvider.wordList)
                    .toDatabase(
                        database = db,
                        table = Schema.WordV2.tableName
                    )
                Schema.WriteQuizV1
                    .asContentValue(DataProvider.writeQuizList)
                    .toDatabase(
                        database = db,
                        table = Schema.WriteQuizV1.tableName,
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
                Schema.WordV1
                    .getFromDatabase(database)
                    .forEachIndexed { index, word ->
                        Assert.assertTrue(
                            "WordId must be ${DataProvider.wordList[index].id}, but here is ${word.id}",
                            word.id == DataProvider.wordList[index].id
                        )
                        Assert.assertTrue(
                            "Column addDate must be not null",
                            word.addDate?.time != null
                        )
                        Assert.assertTrue(
                            "Column changeDate must be null",
                            word.changeDate?.time == null
                        )
                    }
                Schema.WriteQuizV1.getFromDatabase(database)
                    .forEachIndexed { index, writeQuiz ->
                        Assert.assertTrue(
                            "Id must be ${DataProvider.writeQuizList[index].id}, but here is ${writeQuiz.id}",
                            writeQuiz.id == DataProvider.writeQuizList[index].id
                        )
                        Assert.assertTrue(
                            "Column addDate must be not null",
                            writeQuiz.addDate?.time != null
                        )
                        Assert.assertTrue(
                            "Column lastSelectDate must be null",
                            writeQuiz.lastSelectDate?.time == null
                        )
                    }
            },
        )
    }

    companion object {
        private const val CURRENT_VERSION = 2
    }
}