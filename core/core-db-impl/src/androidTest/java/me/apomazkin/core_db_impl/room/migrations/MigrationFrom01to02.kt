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
class MigrationFrom01to02 : BaseMigration() {

    override fun getMigrationClass(): Migration = migration_1_2
    override fun getCurrentVersion(): Int = CURRENT_VERSION

    @Test
    fun from01to02() {
        runMigrateDbTest(
            onCreate = { database ->
                Schema.Definition
                    .asContentValue(DataProvider.lexemeDbList)
                    .toDatabase(
                        database = database,
                        table = Schema.Definition.tableName,
                    )
            },
            afterCreateCheck = { database ->
                Schema.Definition
                    .getFromDatabase(database)
                    .checkCount(DataProvider.lexemeDbList)
            },
            afterMigrationCheck = { database ->
                Schema.WriteQuizV1
                    .getFromDatabase(database)
                    .forEachIndexed { index, writeQuiz ->
                        Assert.assertTrue(
                            "DefinitionId must be ${DataProvider.lexemeDbList[index].id}, but here is ${writeQuiz.definitionId}",
                            writeQuiz.definitionId == DataProvider.lexemeDbList[index].id
                        )
                    }
            }
        )
    }

    companion object {
        private const val CURRENT_VERSION = 1
    }
}