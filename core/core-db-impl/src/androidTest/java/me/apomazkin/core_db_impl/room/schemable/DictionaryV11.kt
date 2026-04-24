package me.apomazkin.core_db_impl.room.schemable

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.room.base.Schemable
import me.apomazkin.core_db_impl.room.utils.selectAllFromTable
import java.util.Date

object DictionaryV11 : Schemable<DictionaryDbV11> {

    private const val COLUMN_NUMERIC_CODE = "numericCode"
    private const val COLUMN_NAME = "name"
    private const val COLUMN_ADD_DATE = "addDate"
    private const val COLUMN_CHANGE_DATE = "changeDate"

    override val tableName = "dictionaries"

    override val columnList: Array<String> = arrayOf(
        columnId,
        COLUMN_NUMERIC_CODE,
        COLUMN_NAME,
        COLUMN_ADD_DATE,
        COLUMN_CHANGE_DATE,
    )

    override fun asContentValue(list: List<DictionaryDbV11>): List<ContentValues> =
        list.map { dict ->
            ContentValues().apply {
                put(columnId, dict.id)
                dict.numericCode?.let {
                    put(COLUMN_NUMERIC_CODE, it)
                } ?: putNull(COLUMN_NUMERIC_CODE)
                put(COLUMN_NAME, dict.name)
                put(COLUMN_ADD_DATE, dict.addDate.time)
                dict.changeDate?.let {
                    put(COLUMN_CHANGE_DATE, it.time)
                } ?: putNull(COLUMN_CHANGE_DATE)
            }
        }

    override fun getFromDatabase(db: SupportSQLiteDatabase): List<DictionaryDbV11> {
        val result = mutableListOf<DictionaryDbV11>()
        val cursor = db.query(selectAllFromTable(tableName))
        if (cursor.moveToNext()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndex(columnId))
                val numericCode = cursor.getInt(cursor.getColumnIndex(COLUMN_NUMERIC_CODE))
                    .let { if (cursor.isNull(cursor.getColumnIndex(COLUMN_NUMERIC_CODE))) null else it }
                val name = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_NAME))
                val addDate = cursor.getLong(cursor.getColumnIndex(COLUMN_ADD_DATE))
                val changeDate = cursor.getLongOrNull(cursor.getColumnIndex(COLUMN_CHANGE_DATE))
                result.add(
                    DictionaryDbV11(
                        id = id,
                        numericCode = numericCode,
                        name = name ?: "",
                        addDate = Date(addDate),
                        changeDate = changeDate?.let { Date(it) },
                    )
                )
            } while (cursor.moveToNext())
        }
        return result
    }

    override fun data(): List<DictionaryDbV11> {
        val date = Date(System.currentTimeMillis())
        return listOf(
            DictionaryDbV11(id = 0, numericCode = 826, name = "English", addDate = date, changeDate = null),
            DictionaryDbV11(id = 1, numericCode = 250, name = "French", addDate = date, changeDate = null),
        )
    }
}

data class DictionaryDbV11(
    val id: Long,
    val numericCode: Int?,
    val name: String,
    val addDate: Date,
    val changeDate: Date?,
)
