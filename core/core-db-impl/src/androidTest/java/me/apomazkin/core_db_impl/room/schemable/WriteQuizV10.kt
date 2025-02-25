package me.apomazkin.core_db_impl.room.schemable

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.room.base.Schemable
import me.apomazkin.core_db_impl.room.utils.selectAllFromTable
import java.util.Date

object WriteQuizV10 : Schemable<WriteQuizDb> {
    
    private const val COLUMN_LANG_ID = "lang_id"
    private const val COLUMN_LEXEME_ID = "lexeme_id"
    private const val COLUMN_GRADE = "grade"
    private const val COLUMN_SCORE = "score"
    private const val COLUMN_ERROR_COUNT = "error_count"
    private const val COLUMN_ADD_DATE = "add_date"
    private const val COLUMN_LAST_SELECT_DATE = "last_select_date"
    
    override val tableName = "write_quiz"
    
    override val columnList: Array<String> = arrayOf(
        columnId,
        COLUMN_LANG_ID,
        COLUMN_LEXEME_ID,
        COLUMN_GRADE,
        COLUMN_SCORE,
        COLUMN_ERROR_COUNT,
        COLUMN_ADD_DATE,
        COLUMN_LAST_SELECT_DATE,
    )
    
    override fun asContentValue(list: List<WriteQuizDb>): List<ContentValues> =
        list.map { writeQuizDb ->
            ContentValues().apply {
                put(columnId, writeQuizDb.id)
                put(COLUMN_LANG_ID, writeQuizDb.langId)
                put(COLUMN_LEXEME_ID, writeQuizDb.lexemeId)
                put(COLUMN_GRADE, writeQuizDb.grade)
                put(COLUMN_SCORE, writeQuizDb.score)
                put(COLUMN_ERROR_COUNT, writeQuizDb.errorCount)
                put(COLUMN_ADD_DATE, writeQuizDb.addDate.time)
                put(COLUMN_LAST_SELECT_DATE, writeQuizDb.lastSelectDate?.time)
            }
        }
    
    override fun getFromDatabase(db: SupportSQLiteDatabase): List<WriteQuizDb> {
        val result = mutableListOf<WriteQuizDb>()
        val cursorWriteQuiz = db.query(selectAllFromTable(tableName))
        if (cursorWriteQuiz.moveToNext()) {
            do {
                val id = cursorWriteQuiz.getLong(
                    cursorWriteQuiz.getColumnIndex(columnId)
                )
                val langId = cursorWriteQuiz.getLong(
                    cursorWriteQuiz.getColumnIndex(COLUMN_LANG_ID)
                )
                val lexemeId = cursorWriteQuiz.getLong(
                    cursorWriteQuiz.getColumnIndex(COLUMN_LEXEME_ID)
                )
                val grade = cursorWriteQuiz.getInt(
                    cursorWriteQuiz.getColumnIndex(COLUMN_GRADE)
                )
                val score = cursorWriteQuiz.getInt(
                    cursorWriteQuiz.getColumnIndex(COLUMN_SCORE)
                )
                val errorCount = cursorWriteQuiz.getInt(
                    cursorWriteQuiz.getColumnIndex(COLUMN_ERROR_COUNT)
                )
                val addDate = cursorWriteQuiz.getLongOrNull(
                    cursorWriteQuiz.getColumnIndex(COLUMN_ADD_DATE)
                )
                val lastSelectDate = cursorWriteQuiz.getLongOrNull(
                    cursorWriteQuiz.getColumnIndex(COLUMN_LAST_SELECT_DATE)
                )
                result.add(
                    WriteQuizDb(
                        id = id,
                        langId = langId,
                        lexemeId = lexemeId,
                        grade = grade,
                        score = score,
                        errorCount = errorCount,
                        addDate = addDate?.let { return@let Date(it) }
                            ?: Date(),
                        lastSelectDate = lastSelectDate?.let {
                            return@let Date(
                                it
                            )
                        }
                    )
                )
            } while (cursorWriteQuiz.moveToNext())
        }
        return result
    }
    
    override fun data(): List<WriteQuizDb> {
        val date = Date(System.currentTimeMillis())
        return listOf(
            WriteQuizDb(
                id = 0,
                langId = 0,
                lexemeId = 0,
                grade = 0,
                score = 2,
                errorCount = 9,
                addDate = date,
                lastSelectDate = null
            ),
            WriteQuizDb(
                id = 1,
                langId = 0,
                lexemeId = 1,
                grade = 0,
                score = 0,
                errorCount = 0,
                addDate = date,
                lastSelectDate = date
            ),
            WriteQuizDb(
                id = 2,
                langId = 1,
                lexemeId = 0,
                grade = 0,
                score = 0,
                errorCount = 0,
                addDate = date,
                lastSelectDate = null
            ),
            WriteQuizDb(
                id = 3,
                langId = 0,
                lexemeId = 2,
                grade = 0,
                score = 11,
                errorCount = 2,
                addDate = date,
                lastSelectDate = null
            ),
            WriteQuizDb(
                id = 4,
                langId = 1,
                lexemeId = 1,
                grade = 0,
                score = 3,
                errorCount = 27,
                addDate = date,
                lastSelectDate = date
            )
        )
    }
}