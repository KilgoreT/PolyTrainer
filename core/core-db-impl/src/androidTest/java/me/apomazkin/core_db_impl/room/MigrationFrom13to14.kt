package me.apomazkin.core_db_impl.room

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.apomazkin.core_db_impl.room.migrations.Migration_011_to_012
import me.apomazkin.core_db_impl.room.migrations.Migration_012_to_013
import me.apomazkin.core_db_impl.room.migrations.Migration_013_to_014
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * IS508 migration test M13 → M14: unique-индекс write_quiz.lexeme_id.
 *
 * Кейсы:
 *  - A изолированный 13→14: схема валидна против 14.json, индекс
 *    уникальный, данные v13 живы (легальная база — дублей нет, шаг
 *    схлопывания no-op);
 *  - B повреждённая база: две квиз-строки на одну лексему → после
 *    миграции одна, с MIN(id); прочие лексемы не тронуты;
 *  - C после миграции повторная вставка строки на ту же лексему
 *    отвергается индексом;
 *  - D chained 11→14 — боевой путь пользователя с деплой-тега 0.1.5.
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom13to14 {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val dbFile: File = instrumentation.targetContext.getDatabasePath(DB_NAME)

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        instrumentation = instrumentation,
        file = dbFile,
        driver = BundledSQLiteDriver(),
        databaseClass = Database::class,
    )

    @After
    fun cleanUp() {
        listOf("", "-shm", "-wal", "-journal").forEach { suffix ->
            File(dbFile.path + suffix).takeIf { it.exists() }?.delete()
        }
    }

    private fun migrateFrom13(): SQLiteConnection =
        helper.runMigrationsAndValidate(14, listOf(Migration_013_to_014))

    private fun migrateFrom11(): SQLiteConnection =
        helper.runMigrationsAndValidate(
            14,
            listOf(Migration_011_to_012, Migration_012_to_013, Migration_013_to_014),
        )

    /** Словарь 1, слово 1, лексемы 1 и 2 — на v13. */
    private fun SQLiteConnection.seedV13() {
        execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES (1, NULL, 'EN', 0)")
        execSQL("INSERT INTO words (id, dictionary_id, value, add_date) VALUES (1, 1, 'cat', 0)")
        execSQL("INSERT INTO lexemes (id, word_id, word_class, options, add_date, change_date) VALUES (1, 1, NULL, 0, 0, NULL)")
        execSQL("INSERT INTO lexemes (id, word_id, word_class, options, add_date, change_date) VALUES (2, 1, NULL, 0, 0, NULL)")
    }

    private fun SQLiteConnection.insertQuiz(id: Long, lexemeId: Long, errorCount: Int = 0) {
        execSQL(
            "INSERT INTO write_quiz (id, dictionary_id, lexeme_id, grade, score, error_count, add_date, last_select_date) " +
                "VALUES ($id, 1, $lexemeId, 0, 0, $errorCount, 0, NULL)",
        )
    }

    // === Case A — изолированный 13→14: индекс уникальный, данные живы ===
    @Test
    fun caseA_isolated13to14_uniqueIndex_dataAlive() {
        helper.createDatabase(13).use { v13 ->
            v13.seedV13()
            v13.insertQuiz(id = 10, lexemeId = 1)
            v13.insertQuiz(id = 11, lexemeId = 2)
        }
        val v14 = migrateFrom13()

        assertEquals(1, v14.countWhere("sqlite_master", "type='index' AND name='index_write_quiz_lexeme_id'"))
        assertTrue(v14.indexSql("index_write_quiz_lexeme_id").contains("UNIQUE"))
        assertEquals(2, v14.countWhere("write_quiz", "1=1"))
        assertEquals(1, v14.countWhere("write_quiz", "id=10 AND lexeme_id=1"))
        v14.close()
    }

    // === Case B — дубли схлопываются до MIN(id) ===
    @Test
    fun caseB_duplicateRows_collapsedToOldest() {
        helper.createDatabase(13).use { v13 ->
            v13.seedV13()
            v13.insertQuiz(id = 10, lexemeId = 1, errorCount = 3)
            v13.insertQuiz(id = 11, lexemeId = 1, errorCount = 7) // дубль
            v13.insertQuiz(id = 12, lexemeId = 1) // ещё дубль
            v13.insertQuiz(id = 20, lexemeId = 2)
        }
        val v14 = migrateFrom13()

        assertEquals(1, v14.countWhere("write_quiz", "lexeme_id=1"))
        assertEquals(1, v14.countWhere("write_quiz", "id=10 AND lexeme_id=1 AND error_count=3"))
        assertEquals(1, v14.countWhere("write_quiz", "id=20 AND lexeme_id=2"))
        assertEquals(2, v14.countWhere("write_quiz", "1=1"))
        v14.close()
    }

    // === Case C — после миграции вторая строка на лексему отвергается ===
    @Test
    fun caseC_afterMigration_secondRowForLexemeRejected() {
        helper.createDatabase(13).use { v13 ->
            v13.seedV13()
            v13.insertQuiz(id = 10, lexemeId = 1)
        }
        val v14 = migrateFrom13()

        val thrown = runCatching { v14.insertQuiz(id = 11, lexemeId = 1) }.isFailure

        assertTrue("unique index must reject duplicate lexeme_id", thrown)
        assertEquals(1, v14.countWhere("write_quiz", "lexeme_id=1"))
        v14.close()
    }

    // === Case D — chained 11→14: боевой путь ===
    @Test
    fun caseD_chained11to14_quizRowsAlive_indexUnique() {
        helper.createDatabase(11).use { v11 ->
            v11.execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES (1, NULL, 'EN', 0)")
            v11.execSQL("INSERT INTO words (id, dictionary_id, value, add_date) VALUES (1, 1, 'cat', 0)")
            v11.execSQL(
                "INSERT INTO lexemes (id, word_id, translation, definition, options, add_date) " +
                    "VALUES (1, 1, 'кошка', NULL, 0, 0)",
            )
            v11.execSQL(
                "INSERT INTO write_quiz (id, dictionary_id, lexeme_id, grade, score, error_count, add_date, last_select_date) " +
                    "VALUES (10, 1, 1, 0, 0, 0, 0, NULL)",
            )
        }
        val v14 = migrateFrom11()

        assertEquals(1, v14.countWhere("write_quiz", "id=10 AND lexeme_id=1"))
        assertTrue(v14.indexSql("index_write_quiz_lexeme_id").contains("UNIQUE"))
        assertEquals(1, v14.countWhere("component_values", "lexeme_id=1"))
        v14.close()
    }

    companion object {
        private const val DB_NAME = "migration-test-14"
    }
}

// Helpers — private в соседних тестах; дублируем минимально нужное
// (файловая видимость private-extension не позволяет reuse).
private fun SQLiteConnection.countWhere(table: String, where: String): Int {
    prepare("SELECT COUNT(*) FROM $table WHERE $where").use { stmt ->
        stmt.step()
        return stmt.getLong(0).toInt()
    }
}

private fun SQLiteConnection.indexSql(name: String): String {
    prepare("SELECT sql FROM sqlite_master WHERE type='index' AND name='$name'").use { stmt ->
        stmt.step()
        return stmt.getText(0)
    }
}
