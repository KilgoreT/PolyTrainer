package me.apomazkin.core_db_impl.room.schemable

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.room.base.Schemable
import me.apomazkin.core_db_impl.room.utils.selectAllFromTable
import java.util.Date

object WriteQuizV1 : Schemable<WriteQuizDbV5> {
    
    private const val COLUMN_DEFINITION_ID = "definitionId"
    private const val COLUMN_GRADE = "grade"
    private const val COLUMN_SCORE = "score"
    private const val COLUMN_ADD_DATE = "addDate"
    private const val COLUMN_LAST_SELECT_DATE = "lastSelectDate"
    
    override val tableName = "writeQuiz"
    
    override val columnList: Array<String> = arrayOf(
        columnId,
        COLUMN_DEFINITION_ID,
        COLUMN_GRADE,
        COLUMN_SCORE,
        COLUMN_ADD_DATE,
        COLUMN_LAST_SELECT_DATE,
    )
    
    override fun asContentValue(list: List<WriteQuizDbV5>): List<ContentValues> =
        list.map { writeQuizDb: WriteQuizDbV5 ->
            ContentValues().apply {
                put(columnId, writeQuizDb.id)
                put(COLUMN_DEFINITION_ID, writeQuizDb.definitionId)
                put(COLUMN_GRADE, writeQuizDb.grade)
                put(COLUMN_SCORE, writeQuizDb.score)
            }
        }
    
    override fun getFromDatabase(db: SupportSQLiteDatabase): List<WriteQuizDbV5> {
        val result = mutableListOf<WriteQuizDbV5>()
        val cursorWriteQuiz = db.query(selectAllFromTable(tableName))
        if (cursorWriteQuiz.moveToNext()) {
            do {
                val id = cursorWriteQuiz.getLong(
                    cursorWriteQuiz.getColumnIndex(columnId)
                )
                val definitionId = cursorWriteQuiz.getLong(
                    cursorWriteQuiz.getColumnIndex(COLUMN_DEFINITION_ID)
                )
                val grade = cursorWriteQuiz.getInt(
                    cursorWriteQuiz.getColumnIndex(COLUMN_GRADE)
                )
                val score = cursorWriteQuiz.getInt(
                    cursorWriteQuiz.getColumnIndex(COLUMN_SCORE)
                )
                val addDate = cursorWriteQuiz.getLongOrNull(
                    cursorWriteQuiz.getColumnIndex(COLUMN_ADD_DATE)
                )
                val lastSelectDate = cursorWriteQuiz.getLongOrNull(
                    cursorWriteQuiz.getColumnIndex(COLUMN_LAST_SELECT_DATE)
                )
                result.add(
                    WriteQuizDbV5(
                        id = id,
                        definitionId = definitionId,
                        grade = grade,
                        score = score,
                        addDate = addDate?.let { Date(it) },
                        lastSelectDate = lastSelectDate?.let { Date(it) }
                    )
                )
            } while (cursorWriteQuiz.moveToNext())
        }
        return result
    }
    
    override fun data(): List<WriteQuizDbV5> {
        val date = Date(System.currentTimeMillis())
        return listOf(
            WriteQuizDbV5(
                id = 0,
                definitionId = 0,
                grade = 1,
                score = 4,
                addDate = null,
                lastSelectDate = null
            ),
            WriteQuizDbV5(
                id = 1,
                definitionId = 5,
                grade = 4,
                score = 1,
                addDate = date,
                lastSelectDate = date
            ),
            WriteQuizDbV5(
                id = 2,
                definitionId = 2,
                grade = 1,
                score = 0,
                addDate = date,
                lastSelectDate = null
            ),
            WriteQuizDbV5(
                id = 3,
                definitionId = 8,
                grade = 2,
                score = 1,
                addDate = date,
                lastSelectDate = null
            ),
            WriteQuizDbV5(
                id = 4,
                definitionId = 1,
                grade = 0,
                score = 3,
                addDate = date,
                lastSelectDate = date
            ),
        )
    }
}