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
class MigrationFrom03to04 : BaseMigration() {

    override fun getMigrationClass() = migration_3_4
    override fun getCurrentVersion() = CURRENT_VERSION

    @Test
    fun from03to04() {
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
                // check hint insert
                Schema.HintV1
                    .asContentValue(DataProvider.hintList)
                    .toDatabase(
                        database = database,
                        table = Schema.HintV1.tableName
                    )
                Schema.HintV1
                    .getFromDatabase(database)
                    .checkCount(DataProvider.hintList)

                // check that definitions isn't erase
                Schema.Definition
                    .getFromDatabase(database)
                    .checkCount(DataProvider.lexemeDbList)
            }
        )
    }

    companion object {
        private const val CURRENT_VERSION = 3
    }
}