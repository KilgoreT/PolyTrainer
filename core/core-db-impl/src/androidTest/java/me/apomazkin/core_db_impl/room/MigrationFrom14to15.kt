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
import me.apomazkin.core_db_impl.room.migrations.Migration_014_to_015
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * IS515 migration test M14 → M15: расширенный состав builtin «Часть речи».
 *
 * Кейсы:
 *  - A изолированный 14→15: 6 опций → 12, позиции в целевом порядке,
 *    значение лексемы (option_id) живо;
 *  - B идемпотентность: опция `collocation` уже есть → дубля нет;
 *  - C словарь без «Части речи» не трогается;
 *  - D chained 11→15 — боевой путь пользователя.
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom14to15 {

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

    private val expectedOrder = listOf(
        "noun", "verb", "adjective", "adverb", "pronoun", "numeral",
        "preposition", "interjection", "phrasal_verb", "collocation", "idiom", "phrase",
    )

    private val oldKeys = listOf("noun", "verb", "adjective", "adverb", "preposition", "phrase")

    private fun migrateFrom14(): SQLiteConnection =
        helper.runMigrationsAndValidate(15, listOf(Migration_014_to_015))

    /** Словарь [dictId] с «Частью речи» (тип [typeId]) и 6 опциями v14 (позиции 0..5). */
    private fun SQLiteConnection.seedPosV14(dictId: Long, typeId: Long, extraKeys: List<String> = emptyList()) {
        execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES ($dictId, NULL, 'D$dictId', 0)")
        execSQL(
            "INSERT INTO component_types (id, system_key, dictionary_id, name, template_key, position, " +
                "is_multiple, core, enabled, depends_on_type_id, depends_on_option_id, created_at, updated_at, removed_at) " +
                "VALUES ($typeId, 'part_of_speech', $dictId, NULL, 'choice', 1, 0, 0, 1, NULL, NULL, 0, 0, NULL)",
        )
        (oldKeys + extraKeys).forEachIndexed { index, key ->
            execSQL(
                "INSERT INTO component_options (component_type_id, system_key, label, position, created_at, updated_at, removed_at) " +
                    "VALUES ($typeId, '$key', NULL, $index, 0, 0, NULL)",
            )
        }
    }

    private fun SQLiteConnection.optionKeysByPosition(typeId: Long): List<String> {
        val keys = mutableListOf<String>()
        prepare(
            "SELECT system_key FROM component_options WHERE component_type_id = $typeId ORDER BY position",
        ).use { stmt ->
            while (stmt.step()) keys += stmt.getText(0)
        }
        return keys
    }

    private fun SQLiteConnection.optionPositions(typeId: Long): List<Long> {
        val positions = mutableListOf<Long>()
        prepare(
            "SELECT position FROM component_options WHERE component_type_id = $typeId ORDER BY position",
        ).use { stmt ->
            while (stmt.step()) positions += stmt.getLong(0)
        }
        return positions
    }

    // === Case A — 6 → 12 опций, целевой порядок, значения живы ===
    @Test
    fun caseA_isolated14to15_twelveOptionsInTargetOrder_valueAlive() {
        helper.createDatabase(14).use { v14 ->
            v14.seedPosV14(dictId = 1, typeId = 10)
            v14.execSQL("INSERT INTO words (id, dictionary_id, value, add_date) VALUES (1, 1, 'go', 0)")
            v14.execSQL("INSERT INTO lexemes (id, word_id, word_class, options, add_date, change_date) VALUES (1, 1, NULL, 0, 0, NULL)")
            // Значение лексемы — опция «глагол» (id по порядку вставки: noun=1, verb=2).
            v14.execSQL(
                "INSERT INTO component_values (lexeme_id, component_type_id, value, option_id, created_at, updated_at, removed_at) " +
                    "VALUES (1, 10, '{}', 2, 0, 0, NULL)",
            )
        }
        val v15 = migrateFrom14()

        assertEquals(expectedOrder, v15.optionKeysByPosition(typeId = 10))
        assertEquals((0L..11L).toList(), v15.optionPositions(typeId = 10))
        assertEquals(1, v15.countWhere("component_values", "lexeme_id=1 AND option_id=2"))
        assertEquals(1, v15.countWhere("component_options", "id=2 AND system_key='verb'"))
        v15.close()
    }

    // === Case B — идемпотентность: существующий ключ не дублируется ===
    @Test
    fun caseB_existingNewKey_notDuplicated() {
        helper.createDatabase(14).use { v14 ->
            v14.seedPosV14(dictId = 1, typeId = 10, extraKeys = listOf("collocation"))
        }
        val v15 = migrateFrom14()

        assertEquals(1, v15.countWhere("component_options", "component_type_id=10 AND system_key='collocation'"))
        assertEquals(expectedOrder, v15.optionKeysByPosition(typeId = 10))
        v15.close()
    }

    // === Case C — словарь без «Части речи» не трогается, соседний — да ===
    @Test
    fun caseC_dictionaryWithoutPartOfSpeech_untouched() {
        helper.createDatabase(14).use { v14 ->
            v14.execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES (2, NULL, 'Bare', 0)")
            v14.seedPosV14(dictId = 1, typeId = 10)
        }
        val v15 = migrateFrom14()

        assertEquals(12, v15.countWhere("component_options", "component_type_id=10"))
        assertEquals(0, v15.countWhere("component_types", "dictionary_id=2"))
        assertEquals(12, v15.countWhere("component_options", "1=1"))
        v15.close()
    }

    // === Case D — chained 11→15: боевой путь ===
    @Test
    fun caseD_chained11to15_twelveOptions() {
        helper.createDatabase(11).use { v11 ->
            v11.execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES (1, NULL, 'EN', 0)")
            v11.execSQL("INSERT INTO words (id, dictionary_id, value, add_date) VALUES (1, 1, 'cat', 0)")
            v11.execSQL(
                "INSERT INTO lexemes (id, word_id, translation, definition, options, add_date) " +
                    "VALUES (1, 1, 'кошка', NULL, 0, 0)",
            )
        }
        val v15 = helper.runMigrationsAndValidate(
            15,
            listOf(Migration_011_to_012, Migration_012_to_013, Migration_013_to_014, Migration_014_to_015),
        )

        val posTypes = v15.countWhere("component_types", "system_key='part_of_speech'")
        assertEquals(1, posTypes)
        assertEquals(
            12,
            v15.countWhere(
                "component_options",
                "component_type_id IN (SELECT id FROM component_types WHERE system_key='part_of_speech')",
            ),
        )
        assertEquals(1, v15.countWhere("component_values", "lexeme_id=1"))
        v15.close()
    }

    companion object {
        private const val DB_NAME = "migration-test-15"
    }
}

// Helper — private в соседних тестах; дублируем минимально нужное.
private fun SQLiteConnection.countWhere(table: String, where: String): Int {
    prepare("SELECT COUNT(*) FROM $table WHERE $where").use { stmt ->
        stmt.step()
        return stmt.getLong(0).toInt()
    }
}
