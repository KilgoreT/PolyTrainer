package me.apomazkin.core_db_impl.room.base

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.apomazkin.core_db_impl.room.Database
import me.apomazkin.core_db_impl.room.Schema
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class BaseMigration {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        Database::class.java
    )

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    abstract fun getMigrationClass(): Migration
    abstract fun getCurrentVersion(): Int

    /**
     * runMigrateDbTest
     *
     * @param migrationTestHelper - MigrationTestHelper.
     * @param databaseName - database name in test, try to avoid product name.
     * @param currentVersion - current version of database scheme.
     * @param migration - migration class for next version of scheme.
     *
     * @param onCreate - function to put test data to database.
     * @param afterCreateCheck - function to check test data after initiation. for example, check count of rows.
     * @param afterMigrationCheck - function to check data after migration
     */
    fun runMigrateDbTest(
        migrationTestHelper: MigrationTestHelper = helper,
        databaseName: String = Schema.DATABASE_NAME,
        currentVersion: Int = getCurrentVersion(),
        migration: Migration = getMigrationClass(),
        onCreate: (SupportSQLiteDatabase) -> Unit,
        afterCreateCheck: (SupportSQLiteDatabase) -> Unit,
        afterMigrationCheck: (SupportSQLiteDatabase) -> Unit,
    ) {
        var db = migrationTestHelper.createDatabase(databaseName, currentVersion).apply {
            onCreate.invoke(this)
            afterCreateCheck.invoke(this)
        }
        db.close()
        db = migrationTestHelper.runMigrationsAndValidate(
            databaseName,
            currentVersion + NEXT_VERSION,
            true,
            migration
        )
        afterMigrationCheck.invoke(db)
    }

    companion object {
        private const val NEXT_VERSION = 1
    }
}