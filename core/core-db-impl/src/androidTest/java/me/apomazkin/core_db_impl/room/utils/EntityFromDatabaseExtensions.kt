package me.apomazkin.core_db_impl.room.utils

import androidx.core.database.getLongOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.entity.DefinitionDb
import me.apomazkin.core_db_impl.entity.HintDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.room.Schema
import java.util.*


fun SupportSQLiteDatabase.getWordsFromDatabase(): List<WordDb> {
    val result = mutableListOf<WordDb>()
    val cursor = this.query(selectAllFromTable(Schema.Word.tableName))
    if (cursor.moveToNext()) {
        do {
            val id = cursor.getLong(cursor.getColumnIndex(Schema.Word.columnId))
            val value = cursor.getString(cursor.getColumnIndex(Schema.Word.columnWord))
            val addDate = cursor.getLongOrNull(cursor.getColumnIndex(Schema.Word.columnAddDate))
            val changeDate =
                cursor.getLongOrNull(cursor.getColumnIndex(Schema.Word.columnChangeDate))
            result.add(
                WordDb(
                    id = id,
                    word = value,
                    addDate = addDate?.let { return@let Date(it) },
                    changeDate = changeDate?.let { return@let Date(it) },
                )
            )
        } while (cursor.moveToNext())
    }
    return result
}

fun SupportSQLiteDatabase.getDefinitionsFromDatabase(): List<DefinitionDb> {
    val result = mutableListOf<DefinitionDb>()
    val cursorDefinition = this.query(selectAllFromTable(Schema.Definition.tableName))
    if (cursorDefinition.moveToNext()) {
        do {
            val id = cursorDefinition.getLong(
                cursorDefinition.getColumnIndex(Schema.Definition.columnId)
            )
            val wordId = cursorDefinition.getLong(
                cursorDefinition.getColumnIndex(Schema.Definition.columnWordId)
            )
            val definition = cursorDefinition.getString(
                cursorDefinition.getColumnIndex(Schema.Definition.columnDefinition)
            )
            val wordClass = cursorDefinition.getString(
                cursorDefinition.getColumnIndex(Schema.Definition.columnWordClass)
            )
            val options = cursorDefinition.getLong(
                cursorDefinition.getColumnIndex(Schema.Definition.columnOptions)
            )
            result.add(
                DefinitionDb(
                    id = id,
                    wordId = wordId,
                    definition = definition,
                    wordClass = wordClass,
                    options = options
                )
            )
        } while (cursorDefinition.moveToNext())
    }
    return result
}

fun SupportSQLiteDatabase.getHintsFromDatabase(): List<HintDb> {
    val result = mutableListOf<HintDb>()
    val cursor = this.query(selectAllFromTable(Schema.Hint.tableName))
    if (cursor.moveToNext()) {
        do {
            val id = cursor.getLong(cursor.getColumnIndex(Schema.Hint.columnId))
            val definitionId =
                cursor.getLong(cursor.getColumnIndex(Schema.Hint.columnDefinitionId))
            val value = cursor.getString(cursor.getColumnIndex(Schema.Hint.columnValue))
            val addDate =
                cursor.getLong(cursor.getColumnIndex(Schema.Hint.columnAddDate))
            val changeDate =
                cursor.getLong(cursor.getColumnIndex(Schema.Hint.columnChangeDate))
            result.add(
                HintDb(
                    id = id,
                    definitionId = definitionId,
                    value = value,
                    addDate = Date(addDate),
                    changeDate = Date(changeDate)
                )
            )
        } while (cursor.moveToNext())
    }
    return result
}

fun SupportSQLiteDatabase.getWriteQuizFromDatabase(): List<WriteQuizDb> {
    val result = mutableListOf<WriteQuizDb>()
    val cursorWriteQuiz = this.query(selectAllFromTable(Schema.WriteQuiz.tableName))
    if (cursorWriteQuiz.moveToNext()) {
        do {
            val id = cursorWriteQuiz.getLong(
                cursorWriteQuiz.getColumnIndex(Schema.WriteQuiz.columnId)
            )
            val definitionId = cursorWriteQuiz.getLong(
                cursorWriteQuiz.getColumnIndex(Schema.WriteQuiz.columnDefinitionId)
            )
            val grade = cursorWriteQuiz.getInt(
                cursorWriteQuiz.getColumnIndex(Schema.WriteQuiz.columnGrade)
            )
            val score = cursorWriteQuiz.getInt(
                cursorWriteQuiz.getColumnIndex(Schema.WriteQuiz.columnScore)
            )
            val addDate = cursorWriteQuiz.getLongOrNull(
                cursorWriteQuiz.getColumnIndex(Schema.WriteQuiz.columnAddDate)
            )
            val lastSelectDate = cursorWriteQuiz.getLongOrNull(
                cursorWriteQuiz.getColumnIndex(Schema.WriteQuiz.columnLastSelectDate)
            )
            result.add(
                WriteQuizDb(
                    id = id,
                    definitionId = definitionId,
                    grade = grade,
                    score = score,
                    addDate = addDate?.let { return@let Date(it) },
                    lastSelectDate = lastSelectDate?.let { return@let Date(it) }
                )
            )
        } while (cursorWriteQuiz.moveToNext())
    }
    return result
}