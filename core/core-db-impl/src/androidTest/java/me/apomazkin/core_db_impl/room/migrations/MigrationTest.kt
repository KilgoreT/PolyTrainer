package me.apomazkin.core_db_impl.room.migrations

import android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL
import androidx.core.database.getLongOrNull
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.apomazkin.core_db_impl.entity.DefinitionDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.room.Database
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        Database::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun migrate1to2() {

        runMigrateDbTest(
            migrationTestHelper = helper,
            currentVersion = 1,
            migration = migration_1_2,
            onCreate = { database ->
                TestDataProvider.getDefinitionListAsContentValues()
                    .forEach { item ->
                        database.insert(
                            Schema.Definition.tableName,
                            CONFLICT_FAIL,
                            item
                        )
                    }
            },
            onCreateCheck = { database ->
                val insertedList = mutableListOf<DefinitionDb>()
                val cursorDefinition =
                    database.query(selectAllFromTable(Schema.Definition.tableName))
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
                        insertedList.add(
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
                assertTrue(
                    "Given definitions count must be ${TestDataProvider.definitionList.size}, but here is ${insertedList.size}",
                    TestDataProvider.definitionList.size == insertedList.size
                )
            },
            onMigrationCheck = { db ->
                val writeQuizList = mutableListOf<WriteQuizDb>()
                val cursorWriteQuiz = db.query(selectAllFromTable(Schema.WriteQuiz.tableName))
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
                        writeQuizList.add(WriteQuizDb(id, definitionId, grade, score))
                    } while (cursorWriteQuiz.moveToNext())
                }

                writeQuizList.forEachIndexed { index, writeQuiz ->
                    assertTrue(
                        "DefinitionId must be ${TestDataProvider.definitionList[index].id}, but here is ${writeQuiz.definitionId}",
                        writeQuiz.definitionId == TestDataProvider.definitionList[index].id
                    )
                }
            }
        )
    }

    @Test
    fun migrate2to3() {

        runMigrateDbTest(
            migrationTestHelper = helper,
            currentVersion = 2,
            migration = migration_2_3,
            onCreate = { database ->
                TestDataProvider.getWordListAsContentValue()
                    .forEach { item -> database.insert(Schema.Word.tableName, CONFLICT_FAIL, item) }
                TestDataProvider.getWordQuizAsContentValue()
                    .forEach { item ->
                        database.insert(
                            Schema.WriteQuiz.tableName,
                            CONFLICT_FAIL,
                            item
                        )
                    }
            },
            onCreateCheck = { database ->
                val insertedList = mutableListOf<WordDb>()
                val cursor = database.query(selectAllFromTable(Schema.Word.tableName))
                if (cursor.moveToNext()) {
                    do {
                        val id = cursor.getLong(cursor.getColumnIndex(Schema.Word.columnId))
                        val value = cursor.getString(cursor.getColumnIndex(Schema.Word.columnWord))
                        insertedList.add(WordDb(id = id, word = value))
                    } while (cursor.moveToNext())
                }
                compareCount(
                    origin = TestDataProvider.wordList,
                    destination = insertedList
                )
            },
            onMigrationCheck = { database ->
                val wordDbList = mutableListOf<WordDb>()
                val cursor = database.query(selectAllFromTable(Schema.Word.tableName))
                if (cursor.moveToNext()) {
                    do {
                        val id = cursor.getLong(
                            cursor.getColumnIndex(Schema.Word.columnId)
                        )
                        val value = cursor.getString(
                            cursor.getColumnIndex(Schema.Word.columnWord)
                        )
                        val addDate = cursor.getLongOrNull(
                            cursor.getColumnIndex(Schema.Word.columnAddDate)
                        )
                        val changeDate = cursor.getLongOrNull(
                            cursor.getColumnIndex(Schema.Word.columnChangeDate)
                        )
                        wordDbList.add(
                            WordDb(
                                id = id,
                                word = value,
                                addDate = addDate?.let { return@let Date(it) },
                                changeDate = changeDate?.let { return@let Date(it) }
                            )
                        )
                    } while (cursor.moveToNext())
                }


                wordDbList.forEachIndexed { index, word ->
                    assertTrue(
                        "WordId must be ${TestDataProvider.wordList[index].id}, but here is ${word.id}",
                        word.id == TestDataProvider.wordList[index].id
                    )
                    assertTrue(
                        "Column addDate must be not null",
                        word.addDate?.time != null
                    )
                    assertTrue(
                        "Column changeDate must be null",
                        word.changeDate?.time == null
                    )
                }

                checkWriteQuiz(database)
            }
        )

    }

    private fun checkWriteQuiz(database: SupportSQLiteDatabase) {
        val writeQuizList = mutableListOf<WriteQuizDb>()
        val cursor = database.query(selectAllFromTable(Schema.WriteQuiz.tableName))
        if (cursor.moveToNext()) {
            do {
                val id = cursor.getLong(
                    cursor.getColumnIndex(Schema.WriteQuiz.columnId)
                )
                val definitionId = cursor.getLong(
                    cursor.getColumnIndex(Schema.WriteQuiz.columnDefinitionId)
                )
                val grade = cursor.getInt(
                    cursor.getColumnIndex(Schema.WriteQuiz.columnGrade)
                )
                val score = cursor.getInt(
                    cursor.getColumnIndex(Schema.WriteQuiz.columnScore)
                )
                val addDate = cursor.getLongOrNull(
                    cursor.getColumnIndex(Schema.WriteQuiz.columnAddDate)
                )
                val lastSelectDate = cursor.getLongOrNull(
                    cursor.getColumnIndex(Schema.WriteQuiz.columnLastSelectDate)
                )
                writeQuizList.add(
                    WriteQuizDb(
                        id = id,
                        definitionId = definitionId,
                        grade = grade,
                        score = score,
                        addDate = addDate?.let { return@let Date(it) },
                        lastSelectDate = lastSelectDate?.let { return@let Date(it) }
                    )
                )
            } while (cursor.moveToNext())
        }

        writeQuizList.forEachIndexed { index, writeQuiz ->
            assertTrue(
                "Id must be ${TestDataProvider.writeQuizList[index].id}, but here is ${writeQuiz.id}",
                writeQuiz.id == TestDataProvider.writeQuizList[index].id
            )
            assertTrue(
                "Column addDate must be not null",
                writeQuiz.addDate?.time != null
            )
            assertTrue(
                "Column lastSelectDate must be null",
                writeQuiz.lastSelectDate?.time == null
            )
        }
    }
}