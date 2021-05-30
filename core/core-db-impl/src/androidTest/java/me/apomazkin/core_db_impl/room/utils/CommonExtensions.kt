package me.apomazkin.core_db_impl.room.utils

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert


fun List<ContentValues>.toDatabase(
    database: SupportSQLiteDatabase,
    table: String,
    conflict: Int = SQLiteDatabase.CONFLICT_FAIL,
): SupportSQLiteDatabase {
    this.map { item -> database.insert(table, conflict, item) }
    return database
}

fun <T> List<T>.checkCount(
    origin: List<T>,
) {
    Assert.assertTrue(
        "Given count must be ${origin.size}, but here is ${this.size}",
        origin.size == this.size
    )
}