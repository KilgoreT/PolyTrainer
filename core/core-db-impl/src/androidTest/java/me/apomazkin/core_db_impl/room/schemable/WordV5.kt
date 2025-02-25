package me.apomazkin.core_db_impl.room.schemable

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.room.PrimaryKey
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.room.base.Schemable
import me.apomazkin.core_db_impl.room.utils.selectAllFromTable
import java.util.Date

object WordV5 : Schemable<WordDbV5> {
    
    private const val COLUMN_LANG_ID = "langId"
    private const val COLUMN_WORD = "word"
    private const val COLUMN_ADD_DATE = "addDate"
    private const val COLUMN_CHANGE_DATE = "changeDate"
    
    override val tableName = "words"
    
    override val columnList: Array<String> = arrayOf(
        columnId,
        COLUMN_LANG_ID,
        COLUMN_WORD,
        COLUMN_ADD_DATE,
        COLUMN_CHANGE_DATE,
    )
    
    override fun asContentValue(list: List<WordDbV5>): List<ContentValues> =
        list.map { word ->
            ContentValues().apply {
                put(columnId, word.id)
                put(COLUMN_LANG_ID, word.langId)
                put(COLUMN_WORD, word.word)
                put(COLUMN_ADD_DATE, word.addDate?.time)
                put(COLUMN_CHANGE_DATE, word.changeDate?.time)
            }
        }
    
    override fun getFromDatabase(db: SupportSQLiteDatabase): List<WordDbV5> {
        val result = mutableListOf<WordDbV5>()
        val cursor = db.query(selectAllFromTable(tableName))
        if (cursor.moveToNext()) {
            do {
                val id = cursor.getLongOrNull(
                    cursor.getColumnIndex(columnId)
                )
                val langId = cursor.getLong(
                    cursor.getColumnIndex(COLUMN_LANG_ID)
                )
                val word = cursor.getStringOrNull(
                    cursor.getColumnIndex(COLUMN_WORD)
                )
                val addDate = cursor.getLongOrNull(
                    cursor.getColumnIndex(COLUMN_ADD_DATE)
                )
                val changeDate = cursor.getLongOrNull(
                    cursor.getColumnIndex(COLUMN_CHANGE_DATE)
                )
                
                result.add(
                    WordDbV5(
                        id = id,
                        langId = langId,
                        word = word,
                        addDate = addDate?.let { Date(it) },
                        changeDate = changeDate?.let { Date(it) },
                    )
                )
            } while (cursor.moveToNext())
        }
        return result
    }
    
    override fun data(): List<WordDbV5> {
        val date = Date(System.currentTimeMillis())
        return listOf(
            WordDbV5(
                id = 1,
                langId = 0,
                word = "word",
                addDate = date,
                changeDate = null,
            ),
            WordDbV5(
                id = 2,
                langId = 0,
                word = null,
                addDate = null,
                changeDate = null,
            ),
            WordDbV5(
                id = 3,
                langId = 1,
                word = "word 3",
                addDate = date,
                changeDate = date,
            ),
            WordDbV5(
                id = 4,
                langId = 1,
                word = "word 4",
                addDate = date,
                changeDate = null,
            ),
        )
    }
}

data class WordDbV5(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val langId: Long = 0,
    val word: String? = null,
    val addDate: Date? = null,
    val changeDate: Date? = null,
)