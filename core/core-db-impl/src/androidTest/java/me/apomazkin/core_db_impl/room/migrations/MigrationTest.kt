package me.apomazkin.core_db_impl.room.migrations

import android.database.sqlite.SQLiteDatabase.CONFLICT_FAIL
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.apomazkin.core_db_impl.entity.DefinitionDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.room.Database
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DATABASE_NAME = "TestDatabaseName"
private const val TABLE_DEFINITIONS = "definitions"

private const val DEFINITION_COLUMN_ID = "id"
private const val DEFINITION_COLUMN_DEFINITION = "definition"
private const val DEFINITION_COLUMN_WORD_ID = "wordId"
private const val DEFINITION_COLUMN_WORD_CLASS = "wordClass"
private const val DEFINITION_COLUMN_OPTIONS = "options"

private const val WRITEQUIZ_COLUMN_ID = "id"
private const val WRITEQUIZ_COLUMN_DEFINITION_ID = "definitionId"
private const val WRITEQUIZ_COLUMN_GRADE = "grade"
private const val WRITEQUIZ_COLUMN_SCORE = "score"


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

        var db = helper.createDatabase(TEST_DATABASE_NAME, 1).apply {
            TestDataProvider.getDefinitionListAsContentValues()
                .forEach { item -> insert(TABLE_DEFINITIONS, CONFLICT_FAIL, item) }
        }

        val insertedList = mutableListOf<DefinitionDb>()
        val cursorDefinition = db.query("SELECT * FROM definitions")
        if (cursorDefinition.moveToNext()) {
            do {
                val id = cursorDefinition.getLong(
                    cursorDefinition.getColumnIndex(DEFINITION_COLUMN_ID)
                )
                val wordId = cursorDefinition.getLong(
                    cursorDefinition.getColumnIndex(DEFINITION_COLUMN_WORD_ID)
                )
                val definition = cursorDefinition.getString(
                    cursorDefinition.getColumnIndex(DEFINITION_COLUMN_DEFINITION)
                )
                val wordClass = cursorDefinition.getString(
                    cursorDefinition.getColumnIndex(DEFINITION_COLUMN_WORD_CLASS)
                )
                val options = cursorDefinition.getLong(
                    cursorDefinition.getColumnIndex(DEFINITION_COLUMN_OPTIONS)
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

        db.close()

        db = helper.runMigrationsAndValidate(TEST_DATABASE_NAME, 2, true, migration_1_2)
        val writeQuizList = mutableListOf<WriteQuizDb>()
        val cursorWriteQuiz = db.query("SELECT * FROM writeQuiz")
        if (cursorWriteQuiz.moveToNext()) {
            do {
                val id = cursorWriteQuiz.getLong(
                    cursorWriteQuiz.getColumnIndex(WRITEQUIZ_COLUMN_ID)
                )
                val definitionId = cursorWriteQuiz.getLong(
                    cursorWriteQuiz.getColumnIndex(WRITEQUIZ_COLUMN_DEFINITION_ID)
                )
                val grade = cursorWriteQuiz.getInt(
                    cursorWriteQuiz.getColumnIndex(WRITEQUIZ_COLUMN_GRADE)
                )
                val score = cursorWriteQuiz.getInt(
                    cursorWriteQuiz.getColumnIndex(WRITEQUIZ_COLUMN_SCORE)
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
}