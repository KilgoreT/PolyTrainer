package me.apomazkin.core_db_impl.room.schemable

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.room.PrimaryKey
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.room.base.Schemable
import me.apomazkin.core_db_impl.room.utils.selectAllFromTable
import java.util.Date

object WordV8 : Schemable<WordDbV8> {
    
    private const val COLUMN_LANG_ID = "langId"
    private const val COLUMN_VALUE = "value"
    private const val COLUMN_ADD_DATE = "addDate"
    private const val COLUMN_CHANGE_DATE = "changeDate"
    private const val COLUMN_REMOVE_DATE = "removeDate"
    
    override val tableName = "words"
    
    override val columnList: Array<String> = arrayOf(
        columnId,
        COLUMN_LANG_ID,
        COLUMN_VALUE,
        COLUMN_ADD_DATE,
        COLUMN_CHANGE_DATE,
        COLUMN_REMOVE_DATE,
    )
    
    override fun asContentValue(list: List<WordDbV8>): List<ContentValues> =
        list.map { word ->
            ContentValues().apply {
                put(columnId, word.id)
                put(COLUMN_LANG_ID, word.langId)
                put(COLUMN_VALUE, word.value)
                put(COLUMN_ADD_DATE, word.addDate?.time)
                put(COLUMN_CHANGE_DATE, word.changeDate?.time)
                put(COLUMN_REMOVE_DATE, word.removeDate?.time)
            }
        }
    
    override fun getFromDatabase(db: SupportSQLiteDatabase): List<WordDbV8> {
        val result = mutableListOf<WordDbV8>()
        val cursor = db.query(selectAllFromTable(tableName))
        if (cursor.moveToNext()) {
            do {
                val id = cursor.getLongOrNull(
                    cursor.getColumnIndex(columnId)
                )
                val langId = cursor.getLong(
                    cursor.getColumnIndex(COLUMN_LANG_ID)
                )
                val value = cursor.getStringOrNull(
                    cursor.getColumnIndex(COLUMN_VALUE)
                )
                val addDate = cursor.getLongOrNull(
                    cursor.getColumnIndex(COLUMN_ADD_DATE)
                )
                val changeDate = cursor.getLongOrNull(
                    cursor.getColumnIndex(COLUMN_CHANGE_DATE)
                )
                val removeDate = cursor.getLongOrNull(
                    cursor.getColumnIndex(COLUMN_REMOVE_DATE)
                )
                
                result.add(
                    WordDbV8(
                        id = id,
                        langId = langId,
                        value = value,
                        addDate = addDate?.let { Date(it) },
                        changeDate = changeDate?.let { Date(it) },
                        removeDate = removeDate?.let { Date(it) }
                    )
                )
            } while (cursor.moveToNext())
        }
        return result
    }
    
    override fun data(): List<WordDbV8> {
        val date = Date(System.currentTimeMillis())
        return listOf(
            WordDbV8(
                id = 1,
                langId = 0,
                value = "word",
                addDate = date,
                changeDate = null,
                removeDate = null,
            ),
            WordDbV8(
                id = 2,
                langId = 0,
                value = null,
                addDate = null,
                changeDate = null,
                removeDate = null,
            ),
            WordDbV8(
                id = 3,
                langId = 1,
                value = "word 3",
                addDate = date,
                changeDate = date,
                removeDate = null,
            ),
            WordDbV8(
                id = 4,
                langId = 1,
                value = "word 4",
                addDate = date,
                changeDate = null,
                removeDate = date,
            ),
        )
    }
}

data class WordDbV8(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val langId: Long = 0,
    val value: String? = null,
    val addDate: Date? = null,
    val changeDate: Date? = null,
    val removeDate: Date? = null,
)