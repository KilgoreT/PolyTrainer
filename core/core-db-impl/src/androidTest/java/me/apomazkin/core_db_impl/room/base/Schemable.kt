package me.apomazkin.core_db_impl.room.base

import android.content.ContentValues
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.Date
import kotlin.math.abs

interface Schemable<T> :
        TableName,
        ColumnId,
        ColumnListable,
        ContentValue<T>,
        FromDatabase<T>,
        DataProvider<T>


interface TableName {
    val tableName: String
}

interface ColumnId {
    val columnId: String
        get() = "id"
}

interface ColumnListable {
    val columnList: Array<String>
}

interface ContentValue<T> {
    fun asContentValue(list: List<T>): List<ContentValues>
}

interface FromDatabase<T> {
    fun getFromDatabase(db: SupportSQLiteDatabase): List<T>
}

interface DataProvider<T> {
    fun data(): List<T>
}

/**
 * Сравнивает две даты с точностью до секунды
 * Потому что в базе данных дата похоже округляется.
 */
fun Date?.isEqualTo(other: Date?): Boolean {
    return if (this != null && other == null) {
        true
    } else if (this == null || other == null) {
        false
    } else {
        abs(this.time - other.time) <= 1000
    }
}