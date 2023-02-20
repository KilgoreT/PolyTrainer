package me.apomazkin.core_db_impl.room.migrations

import androidx.test.ext.junit.runners.AndroidJUnit4
import me.apomazkin.core_db_impl.room.Schema
import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.dataSource.DataProvider
import me.apomazkin.core_db_impl.room.utils.*
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
                DataProvider
                    .languageList
                    .asContentValue()
                    .toDatabase(
                        database = database,
                        table = Schema.Languages.tableName
                    )
            },
            onCreateCheck = { database ->
                database
                    .getLanguagesFromDatabase()
                    .checkCount(DataProvider.languageList)
            },
            onMigrationCheck = { database ->
                database
                    .getLanguagesFromDatabase()
                    .checkCount(DataProvider.languageList)
            }
        )
    }

    companion object {
        private const val CURRENT_VERSION = 6
    }
}