package me.apomazkin.core_db_impl.room.schemable

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.room.base.Schemable
import me.apomazkin.core_db_impl.room.utils.selectAllFromTable
import java.util.Date

object LexemeV10 : Schemable<LexemeDb> {
    
    private const val COLUMN_WORD_ID = "word_id"
    private const val COLUMN_TRANSLATION = "translation"
    private const val COLUMN_DEFINITION = "definition"
    private const val COLUMN_WORD_CLASS = "word_class"
    private const val COLUMN_OPTIONS = "options"
    private const val COLUMN_ADD_DATE = "add_date"
    private const val COLUMN_CHANGE_DATE = "change_date"
    
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
    )
    
    override fun asContentValue(list: List<LexemeDb>): List<ContentValues> =
        list.map { lexeme ->
            ContentValues().apply {
                put(columnId, lexeme.id)
                put(COLUMN_WORD_ID, lexeme.wordId)
                put(COLUMN_TRANSLATION, lexeme.translation)
                put(COLUMN_DEFINITION, lexeme.definition)
                put(COLUMN_WORD_CLASS, lexeme.wordClass)
                put(COLUMN_OPTIONS, lexeme.options)
                put(COLUMN_ADD_DATE, lexeme.addDate.time)
                put(COLUMN_CHANGE_DATE, lexeme.changeDate?.time)
            }
        }
    
    override fun getFromDatabase(db: SupportSQLiteDatabase): List<LexemeDb> {
        val result = mutableListOf<LexemeDb>()
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
                result.add(
                    LexemeDb(
                        id = id,
                        wordId = wordId,
                        translation = translation,
                        definition = definition,
                        wordClass = wordClass,
                        options = options,
                        addDate = Date(addDate),
                        changeDate = changeDate?.let { Date(it) }
                    )
                )
            } while (cursor.moveToNext())
        }
        return result
    }
    
    override fun data(): List<LexemeDb> {
        val date = Date(System.currentTimeMillis())
        return listOf(
            LexemeDb(
                id = 1,
                wordId = 0,
                translation = "translation 1",
                definition = null,
                wordClass = null,
                options = 0,
                addDate = date,
                changeDate = null
            ),
            LexemeDb(
                id = 2,
                wordId = 0,
                translation = null,
                definition = "definition 2",
                wordClass = "wordClass",
                options = 0,
                addDate = date,
                changeDate = date
            ),
            LexemeDb(
                id = 3,
                wordId = 1,
                translation = "translation 3",
                definition = "definition 3",
                wordClass = null,
                options = 0,
                addDate = date,
                changeDate = null
            ),
        )
    }
}