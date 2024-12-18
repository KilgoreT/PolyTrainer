package me.apomazkin.core_db_impl.room

import android.content.ContentValues
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.sqlite.db.SupportSQLiteDatabase
import me.apomazkin.core_db_impl.entity.HintDb
import me.apomazkin.core_db_impl.entity.LanguageDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.SampleDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.room.utils.selectAllFromTable
import java.util.Date

object Schema {

    const val DATABASE_NAME = "TestDatabaseName"

    object Word : TableName, ColumnId, ContentValue<WordDb>, FromDatabase<WordDb> {
        override val tableName = "words"
        const val COLUMN_LANG_ID = "langId"
        const val COLUMN_VALUE = "value"
        const val COLUMN_ADD_DATE = "addDate"
        const val COLUMN_CHANGE_DATE = "changeDate"
        const val COLUMN_REMOVE_DATE = "removeDate"

        override fun asContentValue(list: List<WordDb>): List<ContentValues> = list.map { word ->
            ContentValues().apply {
                put(columnId, word.id)
                put(COLUMN_LANG_ID, word.langId)
                put(COLUMN_VALUE, word.value)
                word.changeDate?.let {
                    put(COLUMN_CHANGE_DATE, it.time)
                } ?: putNull(COLUMN_CHANGE_DATE)
            }
        }

        override fun getFromDatabase(db: SupportSQLiteDatabase): List<WordDb> {
            val result = mutableListOf<WordDb>()
            val c = db.query(selectAllFromTable(tableName))
            if (c.moveToNext()) {
                do {
                    val id = c.getLong(c.getColumnIndex(columnId))
                    val langId = c.getLong(c.getColumnIndex(COLUMN_LANG_ID))
                    val value = c.getString(c.getColumnIndex(COLUMN_VALUE))
                    val addDate = c.getLongOrNull(c.getColumnIndex(COLUMN_ADD_DATE))
                    val changeDate = c.getLongOrNull(c.getColumnIndex(COLUMN_CHANGE_DATE))
                    result.add(
                        WordDb(
                            id = id,
                            langId = langId,
                            value = value,
                            addDate = addDate?.let { return@let Date(it) },
                            changeDate = changeDate?.let { return@let Date(it) },
                        )
                    )
                } while (c.moveToNext())
            }
            return result
        }
    }

    object WordV2 : TableName, ColumnId, ContentValue<WordDb>, FromDatabase<WordDb> {
        override val tableName = "words"
        const val COLUMN_LANG_ID = "langId"
        const val COLUMN_WORD = "word"
        const val COLUMN_ADD_DATE = "addDate"
        const val COLUMN_CHANGE_DATE = "changeDate"

        override fun asContentValue(list: List<WordDb>): List<ContentValues> = list.map { word ->
            ContentValues().apply {
                put(columnId, word.id)
                put(COLUMN_LANG_ID, word.langId)
                put(COLUMN_WORD, word.value)
            }
        }

        override fun getFromDatabase(db: SupportSQLiteDatabase): List<WordDb> {
            val result = mutableListOf<WordDb>()
            val c = db.query(selectAllFromTable(tableName))
            if (c.moveToNext()) {
                do {
                    val id = c.getLong(c.getColumnIndex(columnId))
                    val langId = c.getLong(c.getColumnIndex(COLUMN_LANG_ID))
                    val value = c.getString(c.getColumnIndex(COLUMN_WORD))
                    val addDate = c.getLongOrNull(c.getColumnIndex(COLUMN_ADD_DATE))
                    val changeDate = c.getLongOrNull(c.getColumnIndex(COLUMN_CHANGE_DATE))
                    result.add(
                        WordDb(
                            id = id,
                            langId = langId,
                            value = value,
                            addDate = addDate?.let { return@let Date(it) },
                            changeDate = changeDate?.let { return@let Date(it) },
                        )
                    )
                } while (c.moveToNext())
            }
            return result
        }
    }

    object WordV1 : TableName, ColumnId, ContentValue<WordDb>, FromDatabase<WordDb> {
        override val tableName = "words"
        const val COLUMN_WORD = "word"
        const val COLUMN_ADD_DATE = "addDate"
        const val COLUMN_CHANGE_DATE = "changeDate"

        override fun asContentValue(list: List<WordDb>): List<ContentValues> = list.map { word ->
            ContentValues().apply {
                put(columnId, word.id)
                put(COLUMN_WORD, word.value)
            }
        }

        override fun getFromDatabase(db: SupportSQLiteDatabase): List<WordDb> {
            val result = mutableListOf<WordDb>()
            val c = db.query(selectAllFromTable(tableName))
            if (c.moveToNext()) {
                do {
                    val id = c.getLong(c.getColumnIndex(columnId))
                    val value = c.getString(c.getColumnIndex(COLUMN_WORD))
                    val addDate = c.getLongOrNull(c.getColumnIndex(COLUMN_ADD_DATE))
                    val changeDate = c.getLongOrNull(c.getColumnIndex(COLUMN_CHANGE_DATE))
                    result.add(
                        WordDb(
                            id = id,
                            value = value,
                            addDate = addDate?.let { return@let Date(it) },
                            changeDate = changeDate?.let { return@let Date(it) },
                        )
                    )
                } while (c.moveToNext())
            }
            return result
        }
    }

    object Languages : TableName, ColumnId, ContentValue<LanguageDb>, FromDatabase<LanguageDb> {
        override val tableName = "languages"
        const val COLUMN_NUMERIC_CODE = "numericCode"
        const val COLUMN_CODE = "code"
        const val COLUMN_NAME = "name"
        const val COLUMN_ADD_DATE = "addDate"
        const val COLUMN_CHANGE_DATE = "changeDate"

        override fun asContentValue(list: List<LanguageDb>): List<ContentValues> =
            list.map { lang ->
                ContentValues().apply {
                    put(columnId, lang.id)
                    put(COLUMN_NUMERIC_CODE, lang.numericCode)
                    put(COLUMN_CODE, lang.code)
                    put(COLUMN_NAME, lang.name)
                    put(COLUMN_ADD_DATE, lang.addDate.time)
                    lang.changeDate?.let {
                        put(COLUMN_CHANGE_DATE, it.time)
                    } ?: putNull(COLUMN_CHANGE_DATE)
                }
            }

        override fun getFromDatabase(db: SupportSQLiteDatabase): List<LanguageDb> {
            val result = mutableListOf<LanguageDb>()
            val c = db.query(selectAllFromTable(tableName))
            if (c.moveToNext()) {
                do {
                    val id = c.getLong(c.getColumnIndex(columnId))
                    val numericCode =
                        c.getInt(c.getColumnIndex(COLUMN_NUMERIC_CODE))
                    val code = c.getString(c.getColumnIndex(COLUMN_CODE))
                    val name = c.getStringOrNull(c.getColumnIndex(COLUMN_NAME))
                    val addDate = c.getLong(c.getColumnIndex(COLUMN_ADD_DATE))
                    val changeDate = c.getLongOrNull(c.getColumnIndex(COLUMN_CHANGE_DATE))
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
                } while (c.moveToNext())
            }
            return result
        }
    }

    object LanguagesV1 : TableName, ColumnId, ContentValue<LanguageDb>, FromDatabase<LanguageDb> {
        override val tableName = "languages"
        const val COLUMN_CODE = "code"
        const val COLUMN_NAME = "name"
        const val COLUMN_ADD_DATE = "addDate"
        const val COLUMN_CHANGE_DATE = "changeDate"

        override fun asContentValue(list: List<LanguageDb>): List<ContentValues> =
            list.map { lang ->
                ContentValues().apply {
                    put(columnId, lang.id)
                    put(COLUMN_CODE, lang.code)
                    put(COLUMN_NAME, lang.name)
                    put(COLUMN_ADD_DATE, lang.addDate.time)
                    lang.changeDate?.let {
                        put(COLUMN_CHANGE_DATE, it.time)
                    } ?: putNull(COLUMN_CHANGE_DATE)
                }
            }

        override fun getFromDatabase(db: SupportSQLiteDatabase): List<LanguageDb> {
            val result = mutableListOf<LanguageDb>()
            val c = db.query(selectAllFromTable(tableName))
            if (c.moveToNext()) {
                do {
                    val id = c.getLong(c.getColumnIndex(columnId))
                    val code = c.getString(c.getColumnIndex(COLUMN_CODE))
                    val name = c.getStringOrNull(c.getColumnIndex(COLUMN_NAME))
                    val addDate = c.getLong(c.getColumnIndex(COLUMN_ADD_DATE))
                    val changeDate = c.getLongOrNull(c.getColumnIndex(COLUMN_CHANGE_DATE))
                    result.add(
                        LanguageDb(
                            id = id,
                            numericCode = 0,
                            code = code,
                            name = name,
                            addDate = Date(addDate),
                            changeDate = changeDate?.let { return@let Date(it) },
                        )
                    )
                } while (c.moveToNext())
            }
            return result
        }
    }

    object Definition : TableName, ColumnId, ContentValue<LexemeDb>, FromDatabase<LexemeDb> {
        override val tableName = "definitions"
        const val COLUMN_DEFINITION = "definition"
        const val COLUMN_WORD_ID = "wordId"
        const val COLUMN_WORD_CLASS = "wordClass"
        const val COLUMN_OPTIONS = "options"

        override fun asContentValue(list: List<LexemeDb>): List<ContentValues> =
            list.map { definition ->
                ContentValues().apply {
                    put(columnId, definition.id)
                    put(COLUMN_WORD_ID, definition.wordId)
                    put(COLUMN_DEFINITION, definition.definition)
                    put(COLUMN_WORD_CLASS, definition.wordClass)
                    put(COLUMN_OPTIONS, definition.options)
                }
            }

        override fun getFromDatabase(db: SupportSQLiteDatabase): List<LexemeDb> {
            val result = mutableListOf<LexemeDb>()
            val cursorDefinition = db.query(selectAllFromTable(tableName))
            if (cursorDefinition.moveToNext()) {
                do {
                    val id = cursorDefinition.getLong(
                        cursorDefinition.getColumnIndex(columnId)
                    )
                    val wordId = cursorDefinition.getLong(
                        cursorDefinition.getColumnIndex(COLUMN_WORD_ID)
                    )
                    val definition = cursorDefinition.getString(
                        cursorDefinition.getColumnIndex(COLUMN_DEFINITION)
                    )
                    val wordClass = cursorDefinition.getString(
                        cursorDefinition.getColumnIndex(COLUMN_WORD_CLASS)
                    )
                    val options = cursorDefinition.getLong(
                        cursorDefinition.getColumnIndex(COLUMN_OPTIONS)
                    )
                    result.add(
                        LexemeDb(
                            id = id,
                            wordId = wordId,
                            definition = definition,
                            wordClass = wordClass,
                            options = options,
                            addDate = Date(0),
                        )
                    )
                } while (cursorDefinition.moveToNext())
            }
            return result
        }
    }

    object Lexeme : TableName, ColumnId, ContentValue<LexemeDb>, FromDatabase<LexemeDb> {
        override val tableName = "lexemes"
        const val COLUMN_WORD_ID = "wordId"
        const val COLUMN_TRANSLATION = "translation"
        const val COLUMN_DEFINITION = "definition"
        const val COLUMN_WORD_CLASS = "wordClass"
        const val COLUMN_OPTIONS = "options"
        const val COLUMN_ADD_DATE = "addDate"
        const val COLUMN_CHANGE_DATE = "changeDate"
        const val COLUMN_REMOVE_DATE = "removeDate"


        override fun asContentValue(list: List<LexemeDb>): List<ContentValues> =
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

        override fun getFromDatabase(db: SupportSQLiteDatabase): List<LexemeDb> {
            val result = mutableListOf<LexemeDb>()
            val c = db.query(selectAllFromTable(tableName))
            if (c.moveToNext()) {
                do {
                    val id = c.getLong(c.getColumnIndex(columnId))
                    val wordId = c.getLong(c.getColumnIndex(COLUMN_WORD_ID))
                    val definition = c.getString(c.getColumnIndex(COLUMN_DEFINITION))
                    val wordClass = c.getString(c.getColumnIndex(COLUMN_WORD_CLASS))
                    val options = c.getLong(c.getColumnIndex(COLUMN_OPTIONS))
                    val addDate = c.getLong(c.getColumnIndex(COLUMN_ADD_DATE))
                    val changeDate = c.getLongOrNull(c.getColumnIndex(COLUMN_CHANGE_DATE))
                    val removeDate = c.getLongOrNull(c.getColumnIndex(COLUMN_REMOVE_DATE))
                    result.add(
                        LexemeDb(
                            id = id,
                            wordId = wordId,
                            definition = definition,
                            wordClass = wordClass,
                            options = options,
                            addDate = Date(addDate),
                            changeDate = changeDate?.let { return@let Date(it) },
                            removeDate = removeDate?.let { return@let Date(it) }
                        )
                    )
                } while (c.moveToNext())
            }
            return result
        }
    }

    object LexemeV1 : TableName, ColumnId, ContentValue<LexemeDb>, FromDatabase<LexemeDb> {
        override val tableName = "lexemes"
        const val COLUMN_WORD_ID = "wordId"
        const val COLUMN_TRANSLATION = "translation"
        const val COLUMN_DEFINITION = "definition"
        const val COLUMN_WORD_CLASS = "wordClass"
        const val COLUMN_OPTIONS = "options"

        override fun asContentValue(list: List<LexemeDb>): List<ContentValues> =
            list.map { lexemeDb ->
                ContentValues().apply {
                    put(columnId, lexemeDb.id)
                    put(COLUMN_WORD_ID, lexemeDb.wordId)
                    put(COLUMN_TRANSLATION, lexemeDb.translation)
                    put(COLUMN_DEFINITION, lexemeDb.definition)
                    put(COLUMN_WORD_CLASS, lexemeDb.wordClass)
                    put(COLUMN_OPTIONS, lexemeDb.options)
                }
            }

        override fun getFromDatabase(db: SupportSQLiteDatabase): List<LexemeDb> {
            val result = mutableListOf<LexemeDb>()
            val c = db.query(selectAllFromTable(tableName))
            if (c.moveToNext()) {
                do {
                    val id = c.getLong(c.getColumnIndex(columnId))
                    val wordId = c.getLong(c.getColumnIndex(COLUMN_WORD_ID))
                    val definition = c.getString(c.getColumnIndex(COLUMN_DEFINITION))
                    val wordClass = c.getString(c.getColumnIndex(COLUMN_WORD_CLASS))
                    val options = c.getLong(c.getColumnIndex(COLUMN_OPTIONS))
                    result.add(
                        LexemeDb(
                            id = id,
                            wordId = wordId,
                            definition = definition,
                            wordClass = wordClass,
                            options = options,
                            addDate = Date(0),
                        )
                    )
                } while (c.moveToNext())
            }
            return result
        }
    }

    object Sample : TableName, ColumnId, ContentValue<SampleDb>, FromDatabase<SampleDb> {
        override val tableName = "samples"
        const val COLUMN_LEXEME_ID = "lexemeId"
        const val COLUMN_VALUE = "value"
        const val COLUMN_SOURCE = "source"
        const val COLUMN_ADD_DATE = "addDate"
        const val COLUMN_CHANGE_DATE = "changeDate"
        const val COLUMN_REMOVE_DATE = "removeDate"

        override fun asContentValue(list: List<SampleDb>) = list.map { sample ->
            ContentValues().apply {
                put(columnId, sample.id)
                put(COLUMN_LEXEME_ID, sample.lexemeId)
                put(COLUMN_VALUE, sample.value)
                put(COLUMN_SOURCE, sample.source)
                put(COLUMN_ADD_DATE, sample.addDate.time)
                sample.changeDate?.let {
                    put(COLUMN_CHANGE_DATE, it.time)
                } ?: putNull(COLUMN_CHANGE_DATE)
                sample.removeDate?.let {
                    put(COLUMN_REMOVE_DATE, it.time)
                } ?: putNull(COLUMN_REMOVE_DATE)
            }
        }

        override fun getFromDatabase(db: SupportSQLiteDatabase): List<SampleDb> {
            val result = mutableListOf<SampleDb>()
            val cursor = db.query(selectAllFromTable(tableName))
            if (cursor.moveToNext()) {
                do {
                    val id = cursor.getLong(cursor.getColumnIndex(columnId))
                    val lexemeId = cursor.getLong(cursor.getColumnIndex(COLUMN_LEXEME_ID))
                    val value = cursor.getString(cursor.getColumnIndex(COLUMN_VALUE))
                    val source = cursor.getString(cursor.getColumnIndex(COLUMN_SOURCE))
                    val addDate = cursor.getLong(cursor.getColumnIndex(COLUMN_ADD_DATE))
                    val changeDate = cursor.getLongOrNull(cursor.getColumnIndex(COLUMN_CHANGE_DATE))
                    val removeDate = cursor.getLongOrNull(cursor.getColumnIndex(COLUMN_REMOVE_DATE))
                    result.add(
                        SampleDb(
                            id = id,
                            lexemeId = lexemeId,
                            value = value,
                            source = source,
                            addDate = Date(addDate),
                            changeDate = changeDate?.let { Date(changeDate) },
                            removeDate = removeDate?.let { Date(removeDate) },
                        )
                    )
                } while (cursor.moveToNext())
            }
            return result
        }
    }

    object SampleV1 : TableName, ColumnId, ContentValue<SampleDb>, FromDatabase<SampleDb> {
        override val tableName = "sample"
        const val COLUMN_DEFINITION_ID = "definitionId"
        const val COLUMN_VALUE = "value"
        const val COLUMN_SOURCE = "source"
        const val COLUMN_ADD_DATE = "addDate"
        const val COLUMN_CHANGE_DATE = "changeDate"


        override fun asContentValue(list: List<SampleDb>) = list.map { sample ->
            ContentValues().apply {
                put(columnId, sample.id)
                put(COLUMN_DEFINITION_ID, sample.lexemeId)
                put(COLUMN_VALUE, sample.value)
                put(COLUMN_SOURCE, sample.source)
                put(COLUMN_ADD_DATE, sample.addDate.time)
                sample.changeDate?.let {
                    put(COLUMN_CHANGE_DATE, it.time)
                } ?: putNull(COLUMN_CHANGE_DATE)
            }
        }

        override fun getFromDatabase(db: SupportSQLiteDatabase): List<SampleDb> {
            val result = mutableListOf<SampleDb>()
            val cursor = db.query(selectAllFromTable(tableName))
            if (cursor.moveToNext()) {
                do {
                    val id = cursor.getLong(cursor.getColumnIndex(columnId))
                    val definitionId = cursor.getLong(cursor.getColumnIndex(COLUMN_DEFINITION_ID))
                    val value = cursor.getString(cursor.getColumnIndex(COLUMN_VALUE))
                    val source = cursor.getString(cursor.getColumnIndex(COLUMN_SOURCE))
                    val addDate = cursor.getLong(cursor.getColumnIndex(COLUMN_ADD_DATE))
                    val changeDate = cursor.getLongOrNull(cursor.getColumnIndex(COLUMN_CHANGE_DATE))
                    result.add(
                        SampleDb(
                            id = id,
                            lexemeId = definitionId,
                            value = value,
                            source = source,
                            addDate = Date(addDate),
                            changeDate = changeDate?.let { Date(changeDate) }
                        )
                    )
                } while (cursor.moveToNext())
            }
            return result
        }
    }

    object WriteQuiz : TableName, ColumnId, ContentValue<WriteQuizDb>, FromDatabase<WriteQuizDb> {
        override val tableName = "writeQuiz"
        const val COLUMN_LANG_ID = "langId"
        const val COLUMN_DEFINITION_ID = "definitionId"
        const val COLUMN_GRADE = "grade"
        const val COLUMN_SCORE = "score"
        const val COLUMN_ADD_DATE = "addDate"
        const val COLUMN_LAST_SELECT_DATE = "lastSelectDate"

        override fun asContentValue(list: List<WriteQuizDb>): List<ContentValues> =
            list.map { writeQuizDb ->
                ContentValues().apply {
                    put(columnId, writeQuizDb.id)
                    put(COLUMN_LANG_ID, writeQuizDb.langId)
                    put(COLUMN_DEFINITION_ID, writeQuizDb.definitionId)
                    put(COLUMN_GRADE, writeQuizDb.grade)
                    put(COLUMN_SCORE, writeQuizDb.score)
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
                        WriteQuizDb(
                            id = id,
                            langId = langId,
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
    }

    object WriteQuizV1 : TableName, ColumnId, ContentValue<WriteQuizDb>, FromDatabase<WriteQuizDb> {
        override val tableName = "writeQuiz"
        const val COLUMN_DEFINITION_ID = "definitionId"
        const val COLUMN_GRADE = "grade"
        const val COLUMN_SCORE = "score"
        const val COLUMN_ADD_DATE = "addDate"
        const val COLUMN_LAST_SELECT_DATE = "lastSelectDate"

        override fun asContentValue(list: List<WriteQuizDb>): List<ContentValues> =
            list.map { writeQuizDb ->
                ContentValues().apply {
                    put(columnId, writeQuizDb.id)
                    put(COLUMN_DEFINITION_ID, writeQuizDb.definitionId)
                    put(COLUMN_GRADE, writeQuizDb.grade)
                    put(COLUMN_SCORE, writeQuizDb.score)
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
    }

    object Hint : TableName, ColumnId, ContentValue<HintDb>, FromDatabase<HintDb> {
        override val tableName = "hints"
        const val COLUMN_LEXEME_ID = "lexemeId"
        const val COLUMN_VALUE = "value"
        const val COLUMN_ADD_DATE = "addDate"
        const val COLUMN_CHANGE_DATE = "changeDate"
        const val COLUMN_REMOVE_DATE = "removeDate"

        override fun asContentValue(list: List<HintDb>): List<ContentValues> = list.map { hint ->
            ContentValues().apply {
                put(columnId, hint.id)
                put(COLUMN_LEXEME_ID, hint.lexemeId)
                put(COLUMN_VALUE, hint.value)
                put(COLUMN_ADD_DATE, hint.addDate.time)
                hint.changeDate?.let {
                    put(COLUMN_CHANGE_DATE, it.time)
                } ?: putNull(COLUMN_CHANGE_DATE)
                hint.removeDate?.let {
                    put(COLUMN_REMOVE_DATE, it.time)
                } ?: putNull(COLUMN_REMOVE_DATE)
            }
        }

        override fun getFromDatabase(db: SupportSQLiteDatabase): List<HintDb> {
            val result = mutableListOf<HintDb>()
            val c = db.query(selectAllFromTable(tableName))
            if (c.moveToNext()) {
                do {
                    val id = c.getLong(c.getColumnIndex(columnId))
                    val lexemeId = c.getLong(c.getColumnIndex(COLUMN_LEXEME_ID))
                    val value = c.getString(c.getColumnIndex(COLUMN_VALUE))
                    val addDate = c.getLong(c.getColumnIndex(COLUMN_ADD_DATE))
                    val changeDate = c.getLongOrNull(c.getColumnIndex(COLUMN_CHANGE_DATE))
                    val removeDate = c.getLongOrNull(c.getColumnIndex(COLUMN_REMOVE_DATE))
                    result.add(
                        HintDb(
                            id = id,
                            lexemeId = lexemeId,
                            value = value,
                            addDate = Date(addDate),
                            changeDate = changeDate?.let { Date(changeDate) },
                            removeDate = removeDate?.let { Date(removeDate) }
                        )
                    )
                } while (c.moveToNext())
            }
            return result
        }
    }


    object HintV1 : TableName, ColumnId, ContentValue<HintDb>, FromDatabase<HintDb> {
        override val tableName = "hint"
        const val COLUMN_DEFINITION_ID = "definitionId"
        const val COLUMN_VALUE = "value"
        const val COLUMN_ADD_DATE = "addDate"
        const val COLUMN_CHANGE_DATE = "changeDate"

        override fun asContentValue(list: List<HintDb>): List<ContentValues> = list.map { hint ->
            ContentValues().apply {
                put(columnId, hint.id)
                put(COLUMN_DEFINITION_ID, hint.lexemeId)
                put(COLUMN_VALUE, hint.value)
                put(COLUMN_ADD_DATE, hint.addDate.time)
                hint.changeDate?.let {
                    put(COLUMN_CHANGE_DATE, it.time)
                } ?: putNull(COLUMN_CHANGE_DATE)
            }
        }

        override fun getFromDatabase(db: SupportSQLiteDatabase): List<HintDb> {
            val result = mutableListOf<HintDb>()
            val c = db.query(selectAllFromTable(tableName))
            if (c.moveToNext()) {
                do {
                    val id = c.getLong(c.getColumnIndex(columnId))
                    val definitionId = c.getLong(c.getColumnIndex(COLUMN_DEFINITION_ID))
                    val value = c.getString(c.getColumnIndex(COLUMN_VALUE))
                    val addDate = c.getLong(c.getColumnIndex(COLUMN_ADD_DATE))
                    val changeDate = c.getLongOrNull(c.getColumnIndex(COLUMN_CHANGE_DATE))
                    result.add(
                        HintDb(
                            id = id,
                            lexemeId = definitionId,
                            value = value,
                            addDate = Date(addDate),
                            changeDate = changeDate?.let { Date(changeDate) }
                        )
                    )
                } while (c.moveToNext())
            }
            return result
        }
    }

}

interface TableName {
    val tableName: String
}

interface ColumnId {
    val columnId: String
        get() = "id"
}

interface ContentValue<T> {
    fun asContentValue(list: List<T>): List<ContentValues>
}

interface FromDatabase<T> {
    fun getFromDatabase(db: SupportSQLiteDatabase): List<T>
}