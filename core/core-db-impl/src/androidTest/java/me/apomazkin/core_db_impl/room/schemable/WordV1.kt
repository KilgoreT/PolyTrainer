package me.apomazkin.core_db_impl.room.schemable

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.room.PrimaryKey
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.room.base.Schemable
import me.apomazkin.core_db_impl.room.utils.selectAllFromTable

object WordV1 : Schemable<WordDbV1> {
    
    private const val COLUMN_WORD = "word"
    
    override val tableName = "words"
    
    override val columnList: Array<String> = arrayOf(
        columnId,
        COLUMN_WORD,
    )
    
    override fun asContentValue(list: List<WordDbV1>): List<ContentValues> =
        list.map { word ->
            ContentValues().apply {
                put(columnId, word.id)
                put(COLUMN_WORD, word.word)
            }
        }
    
    override fun getFromDatabase(db: SupportSQLiteDatabase): List<WordDbV1> {
        val result = mutableListOf<WordDbV1>()
        val cursor = db.query(selectAllFromTable(tableName))
        if (cursor.moveToNext()) {
            do {
                val id = cursor.getLongOrNull(
                    cursor.getColumnIndex(columnId)
                )
                val word = cursor.getStringOrNull(
                    cursor.getColumnIndex(COLUMN_WORD)
                )
                result.add(
                    WordDbV1(
                        id = id,
                        word = word,
                    )
                )
            } while (cursor.moveToNext())
        }
        return result
    }
    
    override fun data(): List<WordDbV1> {
        return listOf(
            WordDbV1(
                id = 1,
                word = "word",
            ),
            WordDbV1(
                id = 2,
                word = null,
            ),
            WordDbV1(
                id = 3,
                word = "word 3",
            ),
            WordDbV1(
                id = 4,
                word = "word 4",
            ),
        )
    }
}

data class WordDbV1(
    @PrimaryKey(autoGenerate = true)
    val id: Long? = null,
    val word: String? = null,
)