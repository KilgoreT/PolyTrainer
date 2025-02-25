package me.apomazkin.core_db_impl.room.utils

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert

fun SupportSQLiteDatabase.hasTable(tableName: String) {
    val cursor = this.query(
        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
        arrayOf(tableName)
    )
    Assert.assertTrue("Table $tableName not exist", cursor.moveToFirst())
}

fun SupportSQLiteDatabase.hasColumn(tableName: String, column: String) {
    val cursor = this.query("PRAGMA table_info($tableName)")
    val existingColumns = mutableSetOf<String>()
    cursor.use {
        while (it.moveToNext()) {
            val columnName = it.getString(it.getColumnIndexOrThrow("name"))
            existingColumns.add(columnName)
        }
    }
    existingColumns.contains(column)
    Assert.assertTrue(
        "Table $tableName hasn't column $column",
        existingColumns.contains(column)
    )
}

fun SupportSQLiteDatabase.hasColumns(tableName: String, vararg columns: String) {
    columns.forEach {
        this.hasColumn(tableName = tableName, column = it)
    }
}

fun List<ContentValues>.toDatabase(
    database: SupportSQLiteDatabase,
    table: String,
    conflict: Int = SQLiteDatabase.CONFLICT_FAIL,
): SupportSQLiteDatabase {
    this.map { item -> database.insert(table, conflict, item) }
    return database
}

inline fun <reified T> List<T>.checkCount(
    origin: List<T>,
): List<T> {

    Assert.assertTrue(
        "Given count must be ${origin.size}, but here is ${this.size} (${T::class.simpleName})",
        origin.size == this.size
    )
    return this
}

inline fun <reified MIGRATED, reified ORIGIN> List<MIGRATED>.checkData(
    origin: List<ORIGIN>,
    originMatcher: List<ORIGIN>.(MIGRATED) -> ORIGIN?,
    checkMatcher: (migrated: MIGRATED, origin: ORIGIN) -> Boolean,
    afterMigrationState: Boolean = true,
): List<MIGRATED> {
    
    val logTitle =
        if (afterMigrationState) "Migration Test" else "Creating Test"
    
    Assert.assertTrue(
        "Given count must be ${origin.size}, but here is ${this.size} (${MIGRATED::class.simpleName})",
        origin.size == this.size
    )
    
    this.forEach { migratedItem ->
        val originItem = origin.originMatcher(migratedItem)
        Assert.assertNotNull(
            "Cannot find origin [${ORIGIN::class.simpleName}] for $migratedItem",
            originItem
        )
        originItem?.let {
            Log.d("###", "$logTitle: => \nm:$migratedItem\no:$it")
            Assert.assertTrue(
                "Database item $migratedItem doesn't match to origin item $it",
                checkMatcher.invoke(migratedItem, it)
            )
        }
    }
    
    return this
}

inline fun <reified T> List<T>.checkItems(
    origin: List<T>,
    originMatcher: List<T>.(T) -> T?,
    checkMatcher: (migrated: T, origin: T) -> Boolean,
    afterMigrationState: Boolean = true,
): List<T> {
    
    val logTitle = if (afterMigrationState) "Migration Test" else "Creating Test"
    
    Assert.assertTrue(
        "Given count must be ${origin.size}, but here is ${this.size} (${T::class.simpleName})",
        origin.size == this.size
    )
    
    this.forEach { migratedItem ->
        val originItem = origin.originMatcher(migratedItem)
        Assert.assertNotNull(
            "Cannot find origin [${T::class.simpleName}] for $migratedItem",
            originItem
        )
        originItem?.let {
            Log.d("###", "$logTitle: => \nm:$migratedItem\no:$it")
            Assert.assertTrue(
                "Database item doesn't match to origin item",
                checkMatcher.invoke(migratedItem, it)
            )
        }
    }
    
    return this
}