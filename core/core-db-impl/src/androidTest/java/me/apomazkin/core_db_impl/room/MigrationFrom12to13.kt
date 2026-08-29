package me.apomazkin.core_db_impl.room

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.apomazkin.core_db_impl.room.migrations.Migration_011_to_012
import me.apomazkin.core_db_impl.room.migrations.Migration_012_to_013
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * IS493 migration test M12 → M13: dictionary_groups + word_groups.
 *
 * Кейсы (stage2_plan.md 1.3, D6.5):
 *  - изолированный 12→13: схема валидна против 13.json, данные v12 живы;
 *  - chained 11→13 — боевой путь пользователя (деплой-тег 0.1.5 на v11);
 *  - cascade: удаление слова/словаря/родительской группы чистит
 *    word_groups / dictionary_groups (PRAGMA foreign_keys=ON в тестовом
 *    соединении — как Case F в MigrationFrom11to12).
 *
 * Idempotency-теста нет осознанно (В4): чистые CREATE IF NOT EXISTS
 * идемпотентны по построению, Room оборачивает migrate() в транзакцию.
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom12to13 {

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

    private fun migrateFrom12(): SQLiteConnection =
        helper.runMigrationsAndValidate(13, listOf(Migration_012_to_013))

    private fun migrateFrom11(): SQLiteConnection =
        helper.runMigrationsAndValidate(13, listOf(Migration_011_to_012, Migration_012_to_013))

    // === Case A — изолированный 12→13: схема валидна, данные v12 живы ===
    @Test
    fun caseA_isolated12to13_schemaValid_dataAlive() {
        helper.createDatabase(12).use { v12 ->
            v12.execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES (1, NULL, 'EN', 0)")
            v12.execSQL("INSERT INTO words (id, dictionary_id, value, add_date) VALUES (1, 1, 'cat', 0)")
        }
        val v13 = migrateFrom12()

        // Новые таблицы существуют и пусты.
        assertEquals(1, v13.countWhere("sqlite_master", "type='table' AND name='dictionary_groups'"))
        assertEquals(1, v13.countWhere("sqlite_master", "type='table' AND name='word_groups'"))
        assertEquals(0, v13.countWhere("dictionary_groups", "1=1"))
        assertEquals(0, v13.countWhere("word_groups", "1=1"))
        // Индексы: ровно нужное множество.
        assertEquals(1, v13.countWhere("sqlite_master", "type='index' AND name='index_dictionary_groups_dictionary_id'"))
        assertEquals(1, v13.countWhere("sqlite_master", "type='index' AND name='index_dictionary_groups_parent_group_id'"))
        assertEquals(1, v13.countWhere("sqlite_master", "type='index' AND name='index_word_groups_group_id'"))
        assertEquals(0, v13.countWhere("sqlite_master", "type='index' AND name='index_word_groups_word_id'"))
        // Данные v12 живы.
        assertEquals(1, v13.countWhere("words", "id=1 AND value='cat'"))
        v13.close()
    }

    // === Case B — chained 11→13: боевой путь (деплой-тег на v11) ===
    @Test
    fun caseB_chained11to13_dataMigrated_newTablesEmpty() {
        helper.createDatabase(11).use { v11 ->
            v11.execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES (1, NULL, 'EN', 0)")
            v11.execSQL("INSERT INTO words (id, dictionary_id, value, add_date) VALUES (1, 1, 'cat', 0)")
            v11.execSQL(
                "INSERT INTO lexemes (id, word_id, translation, definition, options, add_date) " +
                    "VALUES (1, 1, 'кошка', NULL, 0, 0)"
            )
        }
        val v13 = migrateFrom11()

        // 11→12 отработала: translation мигрирован в component_values.
        assertEquals(1, v13.countWhere("component_values", "lexeme_id=1"))
        assertEquals(1, v13.countWhere("component_types", "system_key='translation' AND dictionary_id=1"))
        // 12→13 отработала: таблицы групп есть и пусты.
        assertEquals(0, v13.countWhere("dictionary_groups", "1=1"))
        assertEquals(0, v13.countWhere("word_groups", "1=1"))
        v13.close()
    }

    // === Case C — cascade: удаление слова чистит word_groups ===
    @Test
    fun caseC_cascade_wordDelete_cleansMembership() {
        helper.createDatabase(12).use { v12 ->
            v12.execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES (1, NULL, 'EN', 0)")
            v12.execSQL("INSERT INTO words (id, dictionary_id, value, add_date) VALUES (1, 1, 'cat', 0)")
        }
        val v13 = migrateFrom12()
        v13.execSQL("PRAGMA foreign_keys=ON")
        v13.execSQL(
            "INSERT INTO dictionary_groups (id, dictionary_id, parent_group_id, name, created_at, updated_at) " +
                "VALUES (1, 1, NULL, 'Дом', 0, 0)"
        )
        v13.execSQL("INSERT INTO word_groups (word_id, group_id, created_at) VALUES (1, 1, 0)")
        assertEquals(1, v13.countWhere("word_groups", "1=1"))

        v13.execSQL("DELETE FROM words WHERE id=1")

        assertEquals(0, v13.countWhere("word_groups", "1=1"))
        assertEquals(1, v13.countWhere("dictionary_groups", "id=1"))
        v13.close()
    }

    // === Case D — cascade: удаление словаря чистит группы и membership ===
    @Test
    fun caseD_cascade_dictionaryDelete_cleansGroups() {
        helper.createDatabase(12).use { v12 ->
            v12.execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES (1, NULL, 'EN', 0)")
            v12.execSQL("INSERT INTO words (id, dictionary_id, value, add_date) VALUES (1, 1, 'cat', 0)")
        }
        val v13 = migrateFrom12()
        v13.execSQL("PRAGMA foreign_keys=ON")
        v13.execSQL(
            "INSERT INTO dictionary_groups (id, dictionary_id, parent_group_id, name, created_at, updated_at) " +
                "VALUES (1, 1, NULL, 'Дом', 0, 0)"
        )
        v13.execSQL("INSERT INTO word_groups (word_id, group_id, created_at) VALUES (1, 1, 0)")

        v13.execSQL("DELETE FROM dictionaries WHERE id=1")

        assertEquals(0, v13.countWhere("dictionary_groups", "1=1"))
        assertEquals(0, v13.countWhere("word_groups", "1=1"))
        assertEquals(0, v13.countWhere("words", "1=1"))
        v13.close()
    }

    // === Case E — cascade self-FK: удаление родителя сносит подгруппу ===
    @Test
    fun caseE_cascade_parentGroupDelete_cleansSubtree() {
        helper.createDatabase(12).use { v12 ->
            v12.execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES (1, NULL, 'EN', 0)")
        }
        val v13 = migrateFrom12()
        v13.execSQL("PRAGMA foreign_keys=ON")
        v13.execSQL(
            "INSERT INTO dictionary_groups (id, dictionary_id, parent_group_id, name, created_at, updated_at) " +
                "VALUES (1, 1, NULL, 'Дом', 0, 0)"
        )
        v13.execSQL(
            "INSERT INTO dictionary_groups (id, dictionary_id, parent_group_id, name, created_at, updated_at) " +
                "VALUES (2, 1, 1, 'Кухня', 0, 0)"
        )

        v13.execSQL("DELETE FROM dictionary_groups WHERE id=1")

        assertEquals(0, v13.countWhere("dictionary_groups", "1=1"))
        v13.close()
    }

    // === Case G — Э4-задел под папки: kind с дефолтом GROUP ===
    // Вставка БЕЗ kind (все боевые insert'ы Э3 его не знают) обязана
    // получить 'GROUP' — иначе будущая фича папок не отличит типы узлов.
    @Test
    fun caseG_kindColumn_defaultsToGroup() {
        helper.createDatabase(12).use { v12 ->
            v12.execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES (1, NULL, 'EN', 0)")
        }
        val v13 = migrateFrom12()
        v13.execSQL(
            "INSERT INTO dictionary_groups (id, dictionary_id, parent_group_id, name, created_at, updated_at) " +
                "VALUES (1, 1, NULL, 'Дом', 0, 0)"
        )

        assertEquals(1, v13.countWhere("dictionary_groups", "id=1 AND kind='GROUP'"))
        v13.close()
    }

    // === Case F — составной PK: дубль membership отклоняется ===
    @Test
    fun caseF_compositePk_duplicateMembershipRejected() {
        helper.createDatabase(12).use { v12 ->
            v12.execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES (1, NULL, 'EN', 0)")
            v12.execSQL("INSERT INTO words (id, dictionary_id, value, add_date) VALUES (1, 1, 'cat', 0)")
        }
        val v13 = migrateFrom12()
        v13.execSQL(
            "INSERT INTO dictionary_groups (id, dictionary_id, parent_group_id, name, created_at, updated_at) " +
                "VALUES (1, 1, NULL, 'Дом', 0, 0)"
        )
        v13.execSQL("INSERT INTO word_groups (word_id, group_id, created_at) VALUES (1, 1, 0)")
        var thrown = false
        try {
            v13.execSQL("INSERT INTO word_groups (word_id, group_id, created_at) VALUES (1, 1, 5)")
        } catch (_: Exception) {
            thrown = true
        }
        assertEquals("composite PK must reject duplicate", true, thrown)
        v13.close()
    }

    companion object {
        private const val DB_NAME = "migration-test-13"
    }
}

// Helpers countWhere/scalar* — private в MigrationFrom11to12.kt; дублируем
// минимально нужное (файловая видимость private-extension не позволяет reuse).
private fun SQLiteConnection.countWhere(table: String, where: String): Int {
    prepare("SELECT COUNT(*) FROM $table WHERE $where").use { stmt ->
        stmt.step()
        return stmt.getLong(0).toInt()
    }
}
