package me.apomazkin.core_db_impl.room.schemable

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.room.PrimaryKey
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.room.base.Schemable
import me.apomazkin.core_db_impl.room.utils.selectAllFromTable
import java.util.Date

object LexemeV9 : Schemable<LexemeDbV9> {
    
    private const val COLUMN_WORD_ID = "wordId"
    private const val COLUMN_TRANSLATION = "translation"
    private const val COLUMN_DEFINITION = "definition"
    private const val COLUMN_WORD_CLASS = "wordClass"
    private const val COLUMN_OPTIONS = "options"
    private const val COLUMN_ADD_DATE = "addDate"
    private const val COLUMN_CHANGE_DATE = "changeDate"
    private const val COLUMN_REMOVE_DATE = "removeDate"
    
    override val tableName = "lexemes"
    
    override val columnList: Array<String> = arrayOf(
        columnId,
        COLUMN_WORD_ID,
        COLUMN_TRANSLATION,
        COLUMN_DEFINITION,
        COLUMN_WORD_CLASS,
        COLUMN_OPTIONS,
        COLUMN_ADD_DATE,
        COLUMN_CHANGE_DATE,
        COLUMN_REMOVE_DATE,
    )
    
    override fun asContentValue(list: List<LexemeDbV9>): List<ContentValues> =
        list.map { lexemeDb ->
            ContentValues().apply {
                put(columnId, lexemeDb.id)
                put(COLUMN_WORD_ID, lexemeDb.wordId)
                put(COLUMN_TRANSLATION, lexemeDb.translation)
                put(COLUMN_DEFINITION, lexemeDb.definition)
                put(COLUMN_WORD_CLASS, lexemeDb.wordClass)
                put(COLUMN_OPTIONS, lexemeDb.options)
                put(COLUMN_ADD_DATE, lexemeDb.addDate.time)
                lexemeDb.changeDate?.let {
                    put(COLUMN_CHANGE_DATE, it.time)
                } ?: putNull(COLUMN_CHANGE_DATE)
                lexemeDb.removeDate?.let {
                    put(COLUMN_REMOVE_DATE, it.time)
                } ?: putNull(COLUMN_REMOVE_DATE)
            }
        }
    
    override fun getFromDatabase(db: SupportSQLiteDatabase): List<LexemeDbV9> {
        val result = mutableListOf<LexemeDbV9>()
        val cursor = db.query(selectAllFromTable(tableName))
        if (cursor.moveToNext()) {
            do {
                val id = cursor.getLong(
                    cursor.getColumnIndex(columnId)
                )
                val wordId = cursor.getLong(
                    cursor.getColumnIndex(COLUMN_WORD_ID)
                )
                val translation = cursor.getString(
                    cursor.getColumnIndex(COLUMN_TRANSLATION)
                )
                val definition = cursor.getString(
                    cursor.getColumnIndex(COLUMN_DEFINITION)
                )
                val wordClass = cursor.getString(
                    cursor.getColumnIndex(COLUMN_WORD_CLASS)
                )
                val options = cursor.getLong(
                    cursor.getColumnIndex(COLUMN_OPTIONS)
                )
                val addDate = cursor.getLong(
                    cursor.getColumnIndex(COLUMN_ADD_DATE)
                )
                val changeDate = cursor.getLongOrNull(
                    cursor.getColumnIndex(COLUMN_CHANGE_DATE)
                )
                val removeDate = cursor.getLongOrNull(
                    cursor.getColumnIndex(COLUMN_REMOVE_DATE)
                )
                
                result.add(
                    LexemeDbV9(
                        id = id,
                        wordId = wordId,
                        translation = translation,
                        definition = definition,
                        wordClass = wordClass,
                        options = options,
                        addDate = Date(addDate),
                        changeDate = changeDate?.let { Date(it) },
                        removeDate = removeDate?.let { Date(it) },
                    )
                )
            } while (cursor.moveToNext())
        }
        return result
    }
    
    override fun data(): List<LexemeDbV9> {
        val date = Date(System.currentTimeMillis())
        return listOf(
            LexemeDbV9(
                id = 1,
                wordId = 0,
                translation = "translation 1",
                definition = null,
                wordClass = null,
                options = 0,
                addDate = date,
                changeDate = null,
                removeDate = null,
            ),
            LexemeDbV9(
                id = 2,
                wordId = 0,
                translation = null,
                definition = "definition 2",
                wordClass = "wordClass",
                options = 0,
                addDate = date,
                changeDate = date,
                removeDate = null,
            ),
            LexemeDbV9(
                id = 3,
                wordId = 1,
                translation = "translation 3",
                definition = "definition 3",
                wordClass = null,
                options = 0,
                addDate = date,
                changeDate = null,
                removeDate = Date(System.currentTimeMillis())
            ),
        )
    }
}

data class LexemeDbV9(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val wordId: Long,
    val translation: String? = null,
    val definition: String? = null,
    val wordClass: String? = null,
    val options: Long = 0,
    val addDate: Date,
    val changeDate: Date? = null,
    val removeDate: Date? = null,
)