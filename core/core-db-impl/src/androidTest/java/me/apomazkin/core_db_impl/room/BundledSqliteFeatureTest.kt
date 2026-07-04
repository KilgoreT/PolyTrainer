package me.apomazkin.core_db_impl.room

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Smoke androidTest подтверждающий что bundled SQLite driver реально активен и
 * предоставляет SQLite features ≥ 3.45 (acceptance 6.2 из 02_scope.md).
 *
 * Harness — прямой `BundledSQLiteDriver().open(":memory:")` без Room-стека, без
 * MigrationTestHelper, без реальной БД приложения. Цель — независимо доказать,
 * что bundled native binary поставляет фичи, а не Room их симулирует через
 * compat layer.
 */
@RunWith(AndroidJUnit4::class)
class BundledSqliteFeatureTest {

    private lateinit var conn: SQLiteConnection

    @Before
    fun setUp() {
        conn = BundledSQLiteDriver().open(":memory:")
    }

    @After
    fun tearDown() {
        // B3 fix: защита от UninitializedPropertyAccessException
        // если setUp упал до инициализации conn (например UnsatisfiedLinkError).
        if (::conn.isInitialized) {
            conn.close()
        }
    }

    @Test
    fun alterTableDropColumn_isSupported() {
        // Arrange — таблица с двумя колонками
        conn.execSQL("CREATE TABLE t (a INTEGER, b INTEGER)")
        conn.execSQL("INSERT INTO t (a, b) VALUES (1, 2)")

        // Act — DROP COLUMN b (SQLite 3.35+)
        conn.execSQL("ALTER TABLE t DROP COLUMN b")

        // Assert — в pragma_table_info только колонка 'a', нет 'b'
        val columns = mutableListOf<String>()
        conn.prepare("SELECT name FROM pragma_table_info('t')").use { stmt ->
            while (stmt.step()) {
                columns += stmt.getText(0)
            }
        }
        assertEquals(listOf("a"), columns)
    }

    @Test
    fun jsonObject_isSupported() {
        conn.prepare("SELECT json_object('k', 'v') AS r").use { stmt ->
            assertTrue("expected at least one row", stmt.step())
            val result = stmt.getText(0)
            assertEquals("""{"k":"v"}""", result)
        }
    }

    @Test
    fun jsonInsertAppend_isSupported() {
        conn.prepare("""SELECT json_insert(json_array(), '${'$'}[#]', json_object('x', 1)) AS r""")
            .use { stmt ->
                assertTrue(stmt.step())
                val result = stmt.getText(0)
                // Y4 fix: strict equality без .replace для consistency с test 2 (jsonObject).
                // Если bundled SQLite внезапно начнёт форматировать с пробелами — все 3 теста
                // сломаются consistently, регрессия обнаружится сразу.
                assertEquals("""[{"x":1}]""", result)
            }
    }

    @Test
    fun jsonEach_returns3Rows() {
        conn.prepare("SELECT count(*) FROM json_each(json_array(1, 2, 3))").use { stmt ->
            assertTrue(stmt.step())
            val count = stmt.getLong(0)
            assertEquals(3L, count)
        }
    }

    @Test
    fun jsonRemove_isSupported() {
        conn.prepare("""SELECT json_remove(json_array(1, 2, 3), '${'$'}[1]') AS r""").use { stmt ->
            assertTrue(stmt.step())
            val result = stmt.getText(0)
            // Y4 fix: strict equality без .replace для consistency с test 2.
            assertEquals("[1,3]", result)
        }
    }

    @Test
    fun sqliteVersion_isAtLeast3_45() {
        conn.prepare("SELECT sqlite_version()").use { stmt ->
            assertTrue(stmt.step())
            val version = stmt.getText(0)
            // Y2 fix: Log.i удалён — assertion message ниже содержит ту же информацию.
            assertTrue(
                "Expected SQLite >= 3.45, got $version",
                isVersionAtLeast(version, major = 3, minor = 45)
            )
        }
    }

    /**
     * Парсит "3.45.0" / "3.46.1" / "4.0.0" и сравнивает с (major, minor).
     * Lexicographic `version >= "3.45"` ломается на "3.5", поэтому числовое сравнение.
     */
    private fun isVersionAtLeast(version: String, major: Int, minor: Int): Boolean {
        val parts = version.split(".")
        val actualMajor = parts.getOrNull(0)?.toIntOrNull() ?: return false
        val actualMinor = parts.getOrNull(1)?.toIntOrNull() ?: return false
        return when {
            actualMajor > major -> true
            actualMajor < major -> false
            else -> actualMinor >= minor
        }
    }
}
