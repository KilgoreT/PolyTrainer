package me.apomazkin.core_db_impl.room.migrations

import androidx.test.ext.junit.runners.AndroidJUnit4
import me.apomazkin.core_db_impl.room.Schema
import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.dataSource.DataProvider
import me.apomazkin.core_db_impl.room.utils.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom03to04 : BaseMigration() {

    override fun getMigrationClass() = migration_3_4
    override fun getCurrentVersion() = CURRENT_VERSION

    @Test
    fun from03to04() {
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
                // check hint insert
                DataProvider
                    .hintList
                    .asContentValue()
                    .toDatabase(
                        database = database,
                        table = Schema.Hint.tableName
                    )
                    .getHintsFromDatabase()
                    .checkCount(DataProvider.hintList)

                // check that definitions isn't erase
                database
                    .getDefinitionsFromDatabase()
                    .checkCount(DataProvider.definitionList)
            }
        )
    }

    companion object {
        private const val CURRENT_VERSION = 3
    }
}