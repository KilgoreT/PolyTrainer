package me.apomazkin.core_db_impl.room.schemable

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.room.base.Schemable
import me.apomazkin.core_db_impl.room.utils.selectAllFromTable
import java.util.Date

object WordV11 : Schemable<WordDbV11> {

    private const val COLUMN_DICTIONARY_ID = "dictionary_id"
    private const val COLUMN_VALUE = "value"
    private const val COLUMN_ADD_DATE = "add_date"
    private const val COLUMN_CHANGE_DATE = "change_date"

    override val tableName = "words"

    override val columnList: Array<String> = arrayOf(
        columnId,
        COLUMN_DICTIONARY_ID,
        COLUMN_VALUE,
        COLUMN_ADD_DATE,
        COLUMN_CHANGE_DATE,
    )

    override fun asContentValue(list: List<WordDbV11>): List<ContentValues> =
        list.map { word ->
            ContentValues().apply {
                put(columnId, word.id)
                put(COLUMN_DICTIONARY_ID, word.dictionaryId)
                put(COLUMN_VALUE, word.value)
                put(COLUMN_ADD_DATE, word.addDate?.time)
                word.changeDate?.let {
                    put(COLUMN_CHANGE_DATE, it.time)
                } ?: putNull(COLUMN_CHANGE_DATE)
            }
        }

    override fun getFromDatabase(db: SupportSQLiteDatabase): List<WordDbV11> {
        val result = mutableListOf<WordDbV11>()
        val cursor = db.query(selectAllFromTable(tableName))
        if (cursor.moveToNext()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndex(columnId))
                val dictionaryId = cursor.getLong(cursor.getColumnIndex(COLUMN_DICTIONARY_ID))
                val value = cursor.getStringOrNull(cursor.getColumnIndex(COLUMN_VALUE))
                val addDate = cursor.getLongOrNull(cursor.getColumnIndex(COLUMN_ADD_DATE))
                val changeDate = cursor.getLongOrNull(cursor.getColumnIndex(COLUMN_CHANGE_DATE))
                result.add(
                    WordDbV11(
                        id = id,
                        dictionaryId = dictionaryId,
                        value = value ?: "",
                        addDate = addDate?.let { Date(it) },
                        changeDate = changeDate?.let { Date(it) },
                    )
                )
            } while (cursor.moveToNext())
        }
        return result
    }

    override fun data(): List<WordDbV11> {
        val date = Date(System.currentTimeMillis())
        return listOf(
            WordDbV11(id = 1, dictionaryId = 0, value = "hello", addDate = date, changeDate = null),
            WordDbV11(id = 2, dictionaryId = 0, value = "world", addDate = date, changeDate = date),
            WordDbV11(id = 3, dictionaryId = 1, value = "bonjour", addDate = date, changeDate = null),
            WordDbV11(id = 4, dictionaryId = 1, value = "merci", addDate = date, changeDate = null),
        )
    }
}

data class WordDbV11(
    val id: Long,
    val dictionaryId: Long,
    val value: String,
    val addDate: Date?,
    val changeDate: Date?,
)
