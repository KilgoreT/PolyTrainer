package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.apomazkin.core_db_impl.room.Schema
import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.dataSource.DataProvider
import me.apomazkin.core_db_impl.room.utils.*
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
                DataProvider
                    .definitionList
                    .asContentValue()
                    .toDatabase(
                        database = database,
                        table = Schema.Definition.tableName,
                    )
            },
            onCreateCheck = { database ->
                database
                    .getDefinitionsFromDatabase()
                    .checkCount(DataProvider.definitionList)
            },
            onMigrationCheck = { database ->
                database
                    .getWriteQuizFromDatabase()
                    .forEachIndexed { index, writeQuiz ->
                        Assert.assertTrue(
                            "DefinitionId must be ${DataProvider.definitionList[index].id}, but here is ${writeQuiz.definitionId}",
                            writeQuiz.definitionId == DataProvider.definitionList[index].id
                        )
                    }
            }
        )
    }

    companion object {
        private const val CURRENT_VERSION = 1
    }
}