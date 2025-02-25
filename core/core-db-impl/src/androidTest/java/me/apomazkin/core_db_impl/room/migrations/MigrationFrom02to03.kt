package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.schemable.WordV1
import me.apomazkin.core_db_impl.room.schemable.WordV3
import me.apomazkin.core_db_impl.room.schemable.WriteQuizV1
import me.apomazkin.core_db_impl.room.utils.checkCount
import me.apomazkin.core_db_impl.room.utils.toDatabase
import org.junit.Assert
import org.junit.Test

class MigrationFrom02to03 : BaseMigration() {

    override fun getMigrationClass(): Migration = migration_2_3
    override fun getCurrentVersion(): Int = CURRENT_VERSION

    @Test
    fun from02to03() {
        runMigrateDbTest(
            onCreate = { db ->
                WordV1
                    .asContentValue(WordV1.data())
                    .toDatabase(
                        database = db,
                        table = WordV1.tableName
                    )
                WriteQuizV1
                    .asContentValue(WriteQuizV1.data())
                    .toDatabase(
                        database = db,
                        table = WriteQuizV1.tableName,
                    )
            },
            afterCreateCheck = { database ->
                WordV1
                    .getFromDatabase(database)
                    .checkCount(WordV1.data())
                WriteQuizV1
                    .getFromDatabase(database)
                    .checkCount(WriteQuizV1.data())
            },
            afterMigrationCheck = { database ->
                WordV3
                    .getFromDatabase(database)
                    .forEachIndexed { index, word ->
                        Assert.assertTrue(
                            "WordId must be ${WordV1.data()[index].id}, but here is ${word.id}",
                            word.id == WordV1.data()[index].id
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
                WriteQuizV1.getFromDatabase(database)
                    .forEachIndexed { index, writeQuiz ->
                        Assert.assertTrue(
                            "Id must be ${WriteQuizV1.data()[index].id}, but here is ${writeQuiz.id}",
                            writeQuiz.id == WriteQuizV1.data()[index].id
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