package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert

private const val NEXT_VERSION = 1

/**
 * runMigrateDbTest
 *
 * @param migrationTestHelper - MigrationTestHelper.
 * @param databaseName - database name in test, try to avoid product name.
 * @param currentVersion - current version of database scheme.
 * @param migration - migration class for next version of scheme.
 *
 * @param onCreate - function to put test data to database.
 * @param onCreateCheck - function to check test data after initiation. for example, check count of rows.
 * @param onMigrationCheck - function to check data after migration
 */
fun runMigrateDbTest(
    migrationTestHelper: MigrationTestHelper,
    databaseName: String = Schema.databaseName,
    currentVersion: Int,
    migration: Migration,
    onCreate: (SupportSQLiteDatabase) -> Unit,
    onCreateCheck: (SupportSQLiteDatabase) -> Unit,
    onMigrationCheck: (SupportSQLiteDatabase) -> Unit,
) {
    var db = migrationTestHelper.createDatabase(databaseName, currentVersion).apply {
        onCreate.invoke(this)
        onCreateCheck.invoke(this)
    }
        ?: throw IllegalArgumentException("Failed to create $databaseName with version: $currentVersion")
    db.close()
    db = migrationTestHelper.runMigrationsAndValidate(
        databaseName,
        currentVersion + NEXT_VERSION,
        true,
        migration
    )
    onMigrationCheck.invoke(db)
}

fun <T> compareCount(
    origin: List<T>,
    destination: List<T>
) {
    Assert.assertTrue(
        "Given count must be ${origin.size}, but here is ${destination.size}",
        origin.size == destination.size
    )
}

fun selectAllFromTable(table: String) = "SELECT * FROM $table"


