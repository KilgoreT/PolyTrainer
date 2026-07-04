package me.apomazkin.core_db_impl.room

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.apomazkin.core_db_impl.room.migrations.MigrationTestFailureException
import me.apomazkin.core_db_impl.room.migrations.Migration_011_to_012
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * IS481 M3 — interrupted migration restart (idempotency).
 *
 * Сценарий: батарея разрядилась / OOM посередине миграции M11→M12. Room
 * откатывает транзакцию, БД остаётся в v11. При повторном запуске приложения
 * миграция выполняется заново и проходит до конца — данные не теряются,
 * UNIQUE/FK violations не возникают.
 *
 * Test-hook (`Migration_011_to_012.migrateImpl(connection, failAfterStep = N)`)
 * инжектирует [MigrationTestFailureException] после конкретного шага.
 * Production `migrate(connection)` всегда передаёт `null` — failure
 * никогда не происходит в реальном приложении.
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom11to12IdempotencyTest {

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

    /**
     * M3 идемпотентность:
     *   Phase 1: v11 + realistic dataset → инжектируем failure на step 7
     *            (INSERT definition data) → Room rollback → assert v11 целый.
     *   Phase 2: перезапустить миграцию без injection → assert v12 валиден.
     */
    @Test
    fun m3_interruptedMigrationRestart_idempotency() {
        // === Setup: v11 с реалистичным dataset ===
        helper.createDatabase(11).use { v11 ->
            // 2 dictionaries
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertDictionary(id = 2, name = "DE")
            // EN: 3 words, mix translation+definition
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.insertWord(id = 2, dictionaryId = 1, value = "dog")
            v11.insertWord(id = 3, dictionaryId = 1, value = "house")
            v11.insertLexeme(id = 1, wordId = 1, translation = "кошка", definition = "pet animal")
            v11.insertLexeme(id = 2, wordId = 2, translation = "собака", definition = null)
            v11.insertLexeme(id = 3, wordId = 3, translation = "дом", definition = "building")
            // DE: 1 word + lexeme только translation
            v11.insertWord(id = 4, dictionaryId = 2, value = "Hund")
            v11.insertLexeme(id = 4, wordId = 4, translation = "собака", definition = null)
        }

        // === Phase 1: миграция с injection failure на step 7 ===
        // Room оборачивает migrate() в транзакцию через savepoint. Здесь
        // мы эмулируем то же — оборачиваем migrateImpl в BEGIN/COMMIT,
        // на исключение делаем ROLLBACK.
        val driver = BundledSQLiteDriver()
        val phase1Conn = driver.open(dbFile.absolutePath)
        var failureCaught = false
        try {
            phase1Conn.execSQL("BEGIN")
            try {
                Migration_011_to_012.migrateImpl(phase1Conn, failAfterStep = 7)
                // Если сюда дошли — test-hook сломан, injection не сработал.
                phase1Conn.execSQL("COMMIT")
                fail("Expected MigrationTestFailureException after step 7, but migration completed")
            } catch (e: MigrationTestFailureException) {
                failureCaught = true
                assertEquals(7, e.step)
                phase1Conn.execSQL("ROLLBACK")
            }
        } finally {
            phase1Conn.close()
        }
        assertTrue("Injection failure must be caught", failureCaught)

        // === Assert post-failure: БД осталась в v11 ===
        val assertConn = driver.open(dbFile.absolutePath)
        try {
            // user_version — Room устанавливает его в самом конце успешной миграции.
            // Поскольку до миграции мы не вызывали runMigrationsAndValidate
            // (только createDatabase v11 + manual rollback), user_version
            // должен остаться установленным helper'ом в 11.
            val userVersion = assertConn.scalarLong("PRAGMA user_version")
            assertEquals(
                "user_version must remain 11 after failed migration",
                11L,
                userVersion,
            )

            // lexemes.translation и .definition колонки на месте.
            assertTrue(
                "lexemes.translation column must still exist",
                assertConn.columnExists("lexemes", "translation"),
            )
            assertTrue(
                "lexemes.definition column must still exist",
                assertConn.columnExists("lexemes", "definition"),
            )

            // Новые таблицы НЕ существуют (rollback убрал CREATE TABLE).
            assertEquals(
                "component_types must not exist after rollback",
                0,
                assertConn.tableCount("component_types"),
            )
            assertEquals(
                "component_values must not exist after rollback",
                0,
                assertConn.tableCount("component_values"),
            )
            assertEquals(
                "quiz_configs must not exist after rollback",
                0,
                assertConn.tableCount("quiz_configs"),
            )

            // Данные v11 целы.
            assertEquals(2, assertConn.countWhere("dictionaries", "1=1"))
            assertEquals(4, assertConn.countWhere("words", "1=1"))
            assertEquals(4, assertConn.countWhere("lexemes", "1=1"))
            assertEquals(
                "translation data preserved",
                "кошка",
                assertConn.scalarText("SELECT translation FROM lexemes WHERE id=1"),
            )
            assertEquals(
                "definition data preserved",
                "pet animal",
                assertConn.scalarText("SELECT definition FROM lexemes WHERE id=1"),
            )
        } finally {
            assertConn.close()
        }

        // === Phase 2: повторный запуск миграции через helper (реальный driver, без injection) ===
        val v12 = helper.runMigrationsAndValidate(12, listOf(Migration_011_to_012))
        try {
            // Schema валиден (validate уже сделал helper). Проверим что данные мигрированы.

            // user_version обновлён.
            assertEquals(12L, v12.scalarLong("PRAGMA user_version"))

            // Колонки translation/definition удалены.
            assertEquals(
                0,
                v12.countWhere(
                    "pragma_table_info('lexemes')",
                    "name IN ('translation','definition')",
                ),
            )

            // Built-in translation тип создан ровно один (UNIQUE на system_key).
            assertEquals(
                1,
                v12.countWhere("component_types", "system_key='translation'"),
            )

            // User-defined Definition только в EN (где есть lexemes с definition).
            assertEquals(
                1,
                v12.countWhere(
                    "component_types",
                    "dictionary_id=1 AND name='Definition' AND system_key IS NULL",
                ),
            )
            assertEquals(
                0,
                v12.countWhere(
                    "component_types",
                    "dictionary_id=2 AND name='Definition'",
                ),
            )

            // 6 component_values total: 4 translations + 2 definitions (lex1, lex3).
            assertEquals(6, v12.countWhere("component_values", "1=1"))
            assertEquals(4, v12.countWhere(
                "component_values cv JOIN component_types ct ON ct.id=cv.component_type_id",
                "ct.system_key='translation'",
            ))
            assertEquals(2, v12.countWhere(
                "component_values cv JOIN component_types ct ON ct.id=cv.component_type_id",
                "ct.name='Definition' AND ct.system_key IS NULL",
            ))

            // Default quiz_configs для всех словарей (F1 invariant).
            assertEquals(2, v12.countWhere("quiz_configs", "quiz_mode='write'"))

            // EN quiz_config — translation + Definition.
            assertEquals(
                2L,
                v12.scalarLong(
                    "SELECT json_array_length(component_refs) FROM quiz_configs " +
                            "WHERE dictionary_id=1 AND quiz_mode='write'",
                ),
            )
            // DE quiz_config — только translation.
            assertEquals(
                1L,
                v12.scalarLong(
                    "SELECT json_array_length(component_refs) FROM quiz_configs " +
                            "WHERE dictionary_id=2 AND quiz_mode='write'",
                ),
            )

            // Данные содержат правильные тексты в финальном M13-envelope (повторный INSERT не сломался).
            assertEquals(
                """{"fields":{"value":{"type":"text","value":"кошка"}}}""",
                v12.scalarText(
                    "SELECT value FROM component_values WHERE lexeme_id=1 AND component_type_id=" +
                            "(SELECT id FROM component_types WHERE system_key='translation')",
                ),
            )
            assertEquals(
                """{"fields":{"value":{"type":"text","value":"pet animal"}}}""",
                v12.scalarText(
                    "SELECT value FROM component_values WHERE lexeme_id=1 AND component_type_id=" +
                            "(SELECT id FROM component_types WHERE name='Definition' AND dictionary_id=1)",
                ),
            )
        } finally {
            v12.close()
        }
    }

    companion object {
        private const val DB_NAME = "migration-test-idempotency"
    }
}

// === Helpers (дублируем приватные хелперы из MigrationFrom11to12.kt) ===
// Причина дублирования: в MigrationFrom11to12.kt они top-level private —
// файл-приватный scope, недоступен из этого файла. Вынесение в shared util
// не делаем чтобы не менять existing test class.

private fun SQLiteConnection.insertDictionary(id: Long, name: String) {
    execSQL(
        "INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES ($id, NULL, '$name', 0)",
    )
}

private fun SQLiteConnection.insertWord(id: Long, dictionaryId: Long, value: String) {
    execSQL(
        "INSERT INTO words (id, dictionary_id, value, add_date) " +
                "VALUES ($id, $dictionaryId, '$value', 0)",
    )
}

private fun SQLiteConnection.insertLexeme(
    id: Long,
    wordId: Long,
    translation: String?,
    definition: String?,
) {
    val t = translation?.let { "'${it.replace("'", "''")}'" } ?: "NULL"
    val d = definition?.let { "'${it.replace("'", "''")}'" } ?: "NULL"
    execSQL(
        "INSERT INTO lexemes (id, word_id, translation, definition, options, add_date) " +
                "VALUES ($id, $wordId, $t, $d, 0, 0)",
    )
}

private fun SQLiteConnection.countWhere(table: String, where: String): Int {
    prepare("SELECT COUNT(*) FROM $table WHERE $where").use { stmt ->
        stmt.step()
        return stmt.getLong(0).toInt()
    }
}

private fun SQLiteConnection.scalarText(sql: String): String {
    prepare(sql).use { stmt ->
        stmt.step()
        return stmt.getText(0)
    }
}

private fun SQLiteConnection.scalarLong(sql: String): Long {
    prepare(sql).use { stmt ->
        stmt.step()
        return stmt.getLong(0)
    }
}

private fun SQLiteConnection.columnExists(table: String, column: String): Boolean {
    prepare("SELECT COUNT(*) FROM pragma_table_info('$table') WHERE name='$column'").use { stmt ->
        stmt.step()
        return stmt.getLong(0) > 0
    }
}

private fun SQLiteConnection.tableCount(name: String): Int {
    prepare("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name='$name'").use { stmt ->
        stmt.step()
        return stmt.getLong(0).toInt()
    }
}
