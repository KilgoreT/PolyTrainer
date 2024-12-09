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
class MigrationFrom06to07 : BaseMigration() {

    override fun getMigrationClass() = migration_6_7
    override fun getCurrentVersion() = CURRENT_VERSION

    @Test
    fun from06to07() {
        runMigrateDbTest(
            onCreate = { database ->
                Schema.Languages
                    .asContentValue(DataProvider.languageList)
                    .toDatabase(
                        database = database,
                        table = Schema.LanguagesV1.tableName
                    )
            },
            afterCreateCheck = { database ->
                Schema.Languages
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
        private const val CURRENT_VERSION = 6
    }
}