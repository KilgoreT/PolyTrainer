package me.apomazkin.core_db_impl.room.schemable

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.room.base.Schemable
import me.apomazkin.core_db_impl.room.utils.selectAllFromTable
import java.util.Date

object WriteQuizV11 : Schemable<WriteQuizDbV11> {

    private const val COLUMN_DICTIONARY_ID = "dictionary_id"
    private const val COLUMN_LEXEME_ID = "lexeme_id"
    private const val COLUMN_GRADE = "grade"
    private const val COLUMN_SCORE = "score"
    private const val COLUMN_ERROR_COUNT = "error_count"
    private const val COLUMN_ADD_DATE = "add_date"
    private const val COLUMN_LAST_SELECT_DATE = "last_select_date"

    override val tableName = "write_quiz"

    override val columnList: Array<String> = arrayOf(
        columnId,
        COLUMN_DICTIONARY_ID,
        COLUMN_LEXEME_ID,
        COLUMN_GRADE,
        COLUMN_SCORE,
        COLUMN_ERROR_COUNT,
        COLUMN_ADD_DATE,
        COLUMN_LAST_SELECT_DATE,
    )

    override fun asContentValue(list: List<WriteQuizDbV11>): List<ContentValues> =
        list.map { quiz ->
            ContentValues().apply {
                put(columnId, quiz.id)
                put(COLUMN_DICTIONARY_ID, quiz.dictionaryId)
                put(COLUMN_LEXEME_ID, quiz.lexemeId)
                put(COLUMN_GRADE, quiz.grade)
                put(COLUMN_SCORE, quiz.score)
                put(COLUMN_ERROR_COUNT, quiz.errorCount)
                put(COLUMN_ADD_DATE, quiz.addDate.time)
                quiz.lastSelectDate?.let {
                    put(COLUMN_LAST_SELECT_DATE, it.time)
                } ?: putNull(COLUMN_LAST_SELECT_DATE)
            }
        }

    override fun getFromDatabase(db: SupportSQLiteDatabase): List<WriteQuizDbV11> {
        val result = mutableListOf<WriteQuizDbV11>()
        val cursor = db.query(selectAllFromTable(tableName))
        if (cursor.moveToNext()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndex(columnId))
                val dictionaryId = cursor.getLong(cursor.getColumnIndex(COLUMN_DICTIONARY_ID))
                val lexemeId = cursor.getLong(cursor.getColumnIndex(COLUMN_LEXEME_ID))
                val grade = cursor.getInt(cursor.getColumnIndex(COLUMN_GRADE))
                val score = cursor.getInt(cursor.getColumnIndex(COLUMN_SCORE))
                val errorCount = cursor.getInt(cursor.getColumnIndex(COLUMN_ERROR_COUNT))
                val addDate = cursor.getLongOrNull(cursor.getColumnIndex(COLUMN_ADD_DATE))
                val lastSelectDate = cursor.getLongOrNull(cursor.getColumnIndex(COLUMN_LAST_SELECT_DATE))
                result.add(
                    WriteQuizDbV11(
                        id = id,
                        dictionaryId = dictionaryId,
                        lexemeId = lexemeId,
                        grade = grade,
                        score = score,
                        errorCount = errorCount,
                        addDate = addDate?.let { Date(it) } ?: Date(),
                        lastSelectDate = lastSelectDate?.let { Date(it) },
                    )
                )
            } while (cursor.moveToNext())
        }
        return result
    }

    override fun data(): List<WriteQuizDbV11> {
        val date = Date(System.currentTimeMillis())
        return listOf(
            WriteQuizDbV11(id = 0, dictionaryId = 0, lexemeId = 0, grade = 0, score = 2, errorCount = 9, addDate = date, lastSelectDate = null),
            WriteQuizDbV11(id = 1, dictionaryId = 0, lexemeId = 1, grade = 0, score = 0, errorCount = 0, addDate = date, lastSelectDate = date),
            WriteQuizDbV11(id = 2, dictionaryId = 1, lexemeId = 0, grade = 0, score = 0, errorCount = 0, addDate = date, lastSelectDate = null),
            WriteQuizDbV11(id = 3, dictionaryId = 0, lexemeId = 2, grade = 0, score = 11, errorCount = 2, addDate = date, lastSelectDate = null),
            WriteQuizDbV11(id = 4, dictionaryId = 1, lexemeId = 1, grade = 0, score = 3, errorCount = 27, addDate = date, lastSelectDate = date),
        )
    }
}

data class WriteQuizDbV11(
    val id: Long,
    val dictionaryId: Long,
    val lexemeId: Long,
    val grade: Int,
    val score: Int,
    val errorCount: Int,
    val addDate: Date,
    val lastSelectDate: Date?,
)
