package me.apomazkin.core_db_impl.room.schemable

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.room.PrimaryKey
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.room.base.Schemable
import me.apomazkin.core_db_impl.room.utils.selectAllFromTable
import java.util.Date

object WordV3 : Schemable<WordDbV3> {
    
    private const val COLUMN_WORD = "word"
    private const val COLUMN_ADD_DATE = "addDate"
    private const val COLUMN_CHANGE_DATE = "changeDate"
    
    override val tableName = "words"
    
    override val columnList: Array<String> = arrayOf(
        columnId,
        COLUMN_WORD,
        COLUMN_ADD_DATE,
        COLUMN_CHANGE_DATE,
    )
    
    override fun asContentValue(list: List<WordDbV3>): List<ContentValues> =
        list.map { word ->
            ContentValues().apply {
                put(columnId, word.id)
                put(COLUMN_WORD, word.word)
                put(COLUMN_ADD_DATE, word.addDate?.time)
                put(COLUMN_CHANGE_DATE, word.changeDate?.time)
            }
        }
    
    override fun getFromDatabase(db: SupportSQLiteDatabase): List<WordDbV3> {
        val result = mutableListOf<WordDbV3>()
        val cursor = db.query(selectAllFromTable(tableName))
        if (cursor.moveToNext()) {
            do {
                val id = cursor.getLongOrNull(
                    cursor.getColumnIndex(columnId)
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
                    WordDbV3(
                        id = id,
                        word = word,
                        addDate = addDate?.let { Date(it) },
                        changeDate = changeDate?.let { Date(it) },
                    )
                )
            } while (cursor.moveToNext())
        }
        return result
    }
    
    override fun data(): List<WordDbV3> {
        val date = Date(System.currentTimeMillis())
        return listOf(
            WordDbV3(
                id = 1,
                word = "word",
                addDate = date,
                changeDate = null,
            ),
            WordDbV3(
                id = 2,
                word = null,
                addDate = null,
                changeDate = null,
            ),
            WordDbV3(
                id = 3,
                word = "word 3",
                addDate = date,
                changeDate = date,
            ),
            WordDbV3(
                id = 4,
                word = "word 4",
                addDate = date,
                changeDate = null,
            ),
        )
    }
}

data class WordDbV3(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val word: String? = null,
    val addDate: Date? = null,
    val changeDate: Date? = null,
)