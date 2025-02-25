package me.apomazkin.core_db_impl.room.migrations

import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.dataSource.DataProvider
import me.apomazkin.core_db_impl.room.schemable.WordV3
import me.apomazkin.core_db_impl.room.schemable.WordV5
import me.apomazkin.core_db_impl.room.schemable.WriteQuizV1
import me.apomazkin.core_db_impl.room.utils.checkCount
import me.apomazkin.core_db_impl.room.utils.toDatabase
import org.junit.Assert
import org.junit.Test

class MigrationFrom04to05 : BaseMigration() {

    override fun getMigrationClass() = migration_4_5
    override fun getCurrentVersion() = CURRENT_VERSION

    @Test
    fun from04to05() {
        runMigrateDbTest(
            onCreate = { database ->
                WordV3
                    .asContentValue(WordV3.data())
                    .toDatabase(
                        database = database,
                        table = WordV3.tableName
                    )
                WriteQuizV1
                    .asContentValue(WriteQuizV1.data())
                    .toDatabase(
                        database = database,
                        table = WriteQuizV1.tableName
                    )
            },
            afterCreateCheck = { database ->
                WordV3
                    .getFromDatabase(database)
                    .checkCount(WordV3.data())
                WriteQuizV1
                    .getFromDatabase(database)
                    .checkCount(WriteQuizV1.data())
            },
            afterMigrationCheck = { database ->
                WordV5
                    .getFromDatabase(database)
                    .forEach { item ->
                        Assert.assertTrue(
                            "WordDb.langId must be 0, but it is ${item.langId}",
                            DataProvider.languageList.first().id?.let { item.langId == it } ?: false
                        )
                    }
                WriteQuizV1
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