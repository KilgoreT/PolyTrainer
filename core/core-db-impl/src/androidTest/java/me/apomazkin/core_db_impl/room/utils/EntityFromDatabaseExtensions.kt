package me.apomazkin.core_db_impl.room.utils

import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.entity.*
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

fun SupportSQLiteDatabase.getLanguagesFromDatabase(): List<LanguageDb> {
    val result = mutableListOf<LanguageDb>()
    val cursor = this.query(selectAllFromTable(Schema.Languages.tableName))
    if (cursor.moveToNext()) {
        do {
            val id = cursor.getLong(cursor.getColumnIndex(Schema.Languages.columnId))
            val numericCode =
                cursor.getInt(cursor.getColumnIndex(Schema.Languages.columnNumericCode))
            val code = cursor.getString(cursor.getColumnIndex(Schema.Languages.columnCode))
            val name = cursor.getStringOrNull(cursor.getColumnIndex(Schema.Languages.columnName))
            val addDate = cursor.getLong(cursor.getColumnIndex(Schema.Languages.columnAddDate))
            val changeDate =
                cursor.getLongOrNull(cursor.getColumnIndex(Schema.Languages.columnChangeDate))
            result.add(
                LanguageDb(
                    id = id,
                    numericCode = numericCode,
                    code = code,
                    name = name,
                    addDate = Date(addDate),
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