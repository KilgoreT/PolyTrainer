package me.apomazkin.core_db_impl.room

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.apomazkin.core_db_api.entity.DictionaryLanguageDefaults
import me.apomazkin.core_db_impl.room.migrations.Migration_011_to_012
import me.apomazkin.core_db_impl.room.migrations.Migration_012_to_013
import me.apomazkin.core_db_impl.room.migrations.Migration_013_to_014
import me.apomazkin.core_db_impl.room.migrations.Migration_014_to_015
import me.apomazkin.core_db_impl.room.migrations.Migration_015_to_016
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * IS525 migration test M15 → M16: языки словаря.
 *
 * Кейсы:
 *  - A словарь с флагом получает язык страны (заглушка: 484 → es-MX);
 *  - B словарь без флага получает английский;
 *  - C язык перевода у всех — из заглушки, данные словарей живы;
 *  - D chained 11→16 — боевой путь старой установки;
 *  - E правила бросают на код страны — миграция не падает, словарь
 *    получает английский.
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom15to16 {

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

    /** Заглушка правил: Мексика → es-MX, другие страны → xx-<код>, без флага → en; перевод → ru. */
    private val defaults = object : DictionaryLanguageDefaults {
        override fun learningLanguageFor(numericCode: Int?): String = when (numericCode) {
            null -> "en"
            484 -> "es-MX"
            else -> "xx-$numericCode"
        }

        override fun translationLanguage(): String = "ru"
    }

    private fun migrateFrom15(): SQLiteConnection =
        helper.runMigrationsAndValidate(16, listOf(Migration_015_to_016(defaults)))

    private fun migrateFrom11(): SQLiteConnection =
        helper.runMigrationsAndValidate(
            16,
            listOf(
                Migration_011_to_012,
                Migration_012_to_013,
                Migration_013_to_014,
                Migration_014_to_015,
                Migration_015_to_016(defaults),
            ),
        )

    private fun SQLiteConnection.insertDictionary(id: Long, numericCode: Int?, name: String) {
        val code = numericCode?.toString() ?: "NULL"
        execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES ($id, $code, '$name', 0)")
    }

    private fun SQLiteConnection.languagesOf(id: Long): Pair<String, String> {
        prepare("SELECT learning_language, translation_language FROM dictionaries WHERE id = $id").use { stmt ->
            stmt.step()
            return stmt.getText(0) to stmt.getText(1)
        }
    }

    // === Case A — флаг → язык страны ===
    @Test
    fun caseA_dictionaryWithFlag_getsCountryLanguage() {
        helper.createDatabase(15).use { v15 ->
            v15.insertDictionary(id = 1, numericCode = 484, name = "MX")
        }
        val v16 = migrateFrom15()

        assertEquals("es-MX" to "ru", v16.languagesOf(1))
        v16.close()
    }

    // === Case B — без флага → английский ===
    @Test
    fun caseB_dictionaryWithoutFlag_getsEnglish() {
        helper.createDatabase(15).use { v15 ->
            v15.insertDictionary(id = 1, numericCode = null, name = "Bio")
        }
        val v16 = migrateFrom15()

        assertEquals("en" to "ru", v16.languagesOf(1))
        v16.close()
    }

    // === Case C — несколько словарей, данные живы ===
    @Test
    fun caseC_severalDictionaries_eachGetsOwnLanguages_dataAlive() {
        helper.createDatabase(15).use { v15 ->
            v15.insertDictionary(id = 1, numericCode = 484, name = "MX")
            v15.insertDictionary(id = 2, numericCode = 826, name = "GB")
            v15.insertDictionary(id = 3, numericCode = null, name = "Bio")
        }
        val v16 = migrateFrom15()

        assertEquals("es-MX" to "ru", v16.languagesOf(1))
        assertEquals("xx-826" to "ru", v16.languagesOf(2))
        assertEquals("en" to "ru", v16.languagesOf(3))
        v16.prepare("SELECT name FROM dictionaries WHERE id = 2").use { stmt ->
            stmt.step()
            assertEquals("GB", stmt.getText(0))
        }
        v16.close()
    }

    // === Case D — chained 11→16 ===
    @Test
    fun caseD_chained11to16_languagesFilled() {
        helper.createDatabase(11).use { v11 ->
            v11.execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES (1, 484, 'MX', 0)")
            v11.execSQL("INSERT INTO words (id, dictionary_id, value, add_date) VALUES (1, 1, 'gato', 0)")
        }
        val v16 = migrateFrom11()

        assertEquals("es-MX" to "ru", v16.languagesOf(1))
        v16.prepare("SELECT COUNT(*) FROM words WHERE dictionary_id = 1").use { stmt ->
            stmt.step()
            assertEquals(1L, stmt.getLong(0))
        }
        v16.close()
    }

    // === Case E — правила бросают → английский, миграция жива ===
    @Test
    fun caseE_rulesThrowOnCountry_migrationSurvives_englishFallback() {
        val throwing = object : DictionaryLanguageDefaults {
            override fun learningLanguageFor(numericCode: Int?): String =
                if (numericCode == null) "en" else throw NullPointerException("unknown country $numericCode")

            override fun translationLanguage(): String = "ru"
        }
        helper.createDatabase(15).use { v15 ->
            v15.insertDictionary(id = 1, numericCode = 999_999, name = "Old")
        }
        val v16 = helper.runMigrationsAndValidate(16, listOf(Migration_015_to_016(throwing)))

        assertEquals("en" to "ru", v16.languagesOf(1))
        v16.close()
    }

    companion object {
        private const val DB_NAME = "migration-test-16"
    }
}
