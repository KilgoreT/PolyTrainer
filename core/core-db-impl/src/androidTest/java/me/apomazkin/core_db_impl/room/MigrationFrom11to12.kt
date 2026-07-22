package me.apomazkin.core_db_impl.room

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.apomazkin.core_db_impl.room.migrations.Migration_011_to_012
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * IS481 collapsed migration test M11 → M12 (single step, финальная схема v12).
 *
 * Проверяет ВЕСЬ объединённый переход на реальных v11-данных (lexemes.translation /
 * lexemes.definition — единственные источники на v11):
 *  - создание component_types/component_values/quiz_configs в финальной форме;
 *  - translation/definition → component_values сразу в финальном JSON-envelope
 *    `{"fields":{"value":{"type":"text","value":"..."}}}`;
 *  - `is_multiple` + `created_at`/`updated_at`/`removed_at` (без `remove_date`);
 *  - отсутствие UNIQUE `(dictionary_id, name)` и `(lexeme_id, component_type_id)`;
 *  - quiz_configs (default [translation] + Definition), FK cascade, спецсимволы.
 *
 * Image/long_text/произвольные user-types на v11 НЕ существуют → их rewrite на
 * upgrade-пути недостижим и здесь не тестируется (по дизайну схлопывания).
 */
@RunWith(AndroidJUnit4::class)
class MigrationFrom11to12 {

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

    private fun migrate(): SQLiteConnection =
        helper.runMigrationsAndValidate(12, listOf(Migration_011_to_012))

    // === Case A — Translation-only: финальный envelope + is_multiple + timestamps ===
    @Test
    fun caseA_translationOnly_finalShape() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.insertLexeme(id = 1, wordId = 1, translation = "кошка", definition = null)
        }
        val v12 = migrate()

        assertEquals(1, v12.countWhere("component_types", "system_key='translation'"))
        assertEquals(1, v12.countWhere("component_values", "lexeme_id=1"))
        // Финальный M13-envelope, НЕ старый {"v":1,...}.
        assertEquals(
            """{"fields":{"value":{"type":"text","value":"кошка"}}}""",
            v12.scalarText("SELECT value FROM component_values WHERE lexeme_id=1"),
        )
        // is_multiple + timestamps на built-in translation.
        assertEquals(0L, v12.scalarLong("SELECT is_multiple FROM component_types WHERE system_key='translation'"))
        assertTrue(v12.scalarLong("SELECT created_at FROM component_types WHERE system_key='translation'") > 0)
        assertTrue(v12.scalarLong("SELECT updated_at FROM component_types WHERE system_key='translation'") > 0)
        assertTrue(v12.scalarLong("SELECT created_at FROM component_values WHERE lexeme_id=1") > 0)
        // translation/definition колонки удалены.
        assertEquals(
            0,
            v12.countWhere("pragma_table_info('lexemes')", "name IN ('translation','definition')"),
        )
        v12.close()
    }

    // === Case B — translation + definition в одном словаре ===
    @Test
    fun caseB_translationAndDefinitionSameDict() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.insertWord(id = 2, dictionaryId = 1, value = "dog")
            v11.insertLexeme(id = 1, wordId = 1, translation = "кошка", definition = "домашнее животное")
            v11.insertLexeme(id = 2, wordId = 2, translation = "собака", definition = null)
        }
        val v12 = migrate()

        assertEquals(
            1,
            v12.countWhere(
                "component_types",
                "dictionary_id=1 AND name='Definition' AND system_key IS NULL AND template_key='text'",
            ),
        )
        assertEquals(3, v12.countWhere("component_values", "1=1"))
        assertEquals(2, v12.countWhere("component_values", "lexeme_id=1"))
        assertEquals(1, v12.countWhere("component_values", "lexeme_id=2"))
        // Definition envelope тоже финальный.
        assertEquals(
            "домашнее животное",
            v12.scalarText(
                "SELECT json_extract(value, '${'$'}.fields.value.value') FROM component_values " +
                    "WHERE lexeme_id=1 AND component_type_id=" +
                    "(SELECT id FROM component_types WHERE name='Definition' AND dictionary_id=1)",
            ),
        )
        v12.close()
    }

    // === Case C — Definition-only lexeme ===
    @Test
    fun caseC_definitionOnly() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.insertLexeme(id = 1, wordId = 1, translation = null, definition = "что-то")
        }
        val v12 = migrate()

        assertEquals(
            1,
            v12.countWhere("component_types", "dictionary_id=1 AND name='Definition' AND system_key IS NULL"),
        )
        assertEquals(1, v12.countWhere("component_values", "lexeme_id=1"))
        val refs = v12.scalarText(
            "SELECT component_refs FROM quiz_configs WHERE dictionary_id=1 AND quiz_mode='write'",
        )
        assertTrue("missing translation ref", refs.contains("\"key\":\"translation\""))
        assertTrue("missing Definition ref", refs.contains("\"name\":\"Definition\""))
        v12.close()
    }

    // === Case D — Empty dictionary: default quiz config (F1) ===
    @Test
    fun caseD_emptyDictionary() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
        }
        val v12 = migrate()

        assertEquals(1, v12.countWhere("quiz_configs", "dictionary_id=1"))
        assertEquals(
            """[{"type":"builtin","key":"translation"}]""",
            v12.scalarText("SELECT component_refs FROM quiz_configs WHERE dictionary_id=1 AND quiz_mode='write'"),
        )
        assertEquals(0, v12.countWhere("component_values", "1=1"))
        assertEquals(0, v12.countWhere("component_types", "dictionary_id=1 AND name='Definition'"))
        v12.close()
    }

    // === Case E — Dictionary с definition → quiz_config содержит оба ref (порядок) ===
    @Test
    fun caseE_dictWithDefinition_quizConfigHasBoth() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.insertLexeme(id = 1, wordId = 1, translation = "кошка", definition = "domestic animal")
        }
        val v12 = migrate()

        assertEquals(
            2L,
            v12.scalarLong(
                "SELECT json_array_length(component_refs) FROM quiz_configs WHERE dictionary_id=1 AND quiz_mode='write'",
            ),
        )
        assertEquals(
            "translation",
            v12.scalarText(
                "SELECT json_extract(component_refs, '${'$'}[0].key') FROM quiz_configs WHERE dictionary_id=1 AND quiz_mode='write'",
            ),
        )
        assertEquals(
            "Definition",
            v12.scalarText(
                "SELECT json_extract(component_refs, '${'$'}[1].name') FROM quiz_configs WHERE dictionary_id=1 AND quiz_mode='write'",
            ),
        )
        v12.close()
    }

    // === Case F — FK cascade chain dictionary delete ===
    @Test
    fun caseF_cascadeChain_dictionaryDelete() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.insertLexeme(id = 1, wordId = 1, translation = "кошка", definition = "pet")
        }
        val v12 = migrate()
        v12.execSQL("PRAGMA foreign_keys=ON")
        v12.execSQL("DELETE FROM dictionaries WHERE id=1")

        assertEquals(0, v12.countWhere("words", "dictionary_id=1"))
        assertEquals(0, v12.countWhere("lexemes", "1=1"))
        assertEquals(0, v12.countWhere("component_values", "1=1"))
        assertEquals(0, v12.countWhere("component_types", "dictionary_id=1"))
        // IS486: builtin пословарные — перевод словаря умирает вместе со словарём.
        assertEquals(0, v12.countWhere("component_types", "system_key='translation'"))
        assertEquals(0, v12.countWhere("component_options", "1=1"))
        assertEquals(0, v12.countWhere("quiz_configs", "dictionary_id=1"))
        v12.close()
    }

    // === Case G — cascade component_types → component_values (финальные колонки) ===
    @Test
    fun caseG_cascadeComponentTypeToValues() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.insertLexeme(id = 1, wordId = 1, translation = "кошка", definition = null)
        }
        val v12 = migrate()
        v12.execSQL("PRAGMA foreign_keys=ON")
        val now = System.currentTimeMillis()

        v12.execSQL(
            """
            INSERT INTO component_types (system_key, dictionary_id, name, template_key, position, is_multiple, created_at, updated_at)
            VALUES (NULL, 1, 'TestType', 'text', 5, 0, $now, $now)
            """.trimIndent(),
        )
        val testTypeId = v12.scalarLong("SELECT id FROM component_types WHERE name='TestType' AND dictionary_id=1")
        v12.execSQL(
            "INSERT INTO component_values (lexeme_id, component_type_id, value, created_at, updated_at) " +
                "VALUES (1, $testTypeId, '{\"fields\":{\"value\":{\"type\":\"text\",\"value\":\"x\"}}}', $now, $now)",
        )
        assertEquals(1, v12.countWhere("component_values", "component_type_id=$testTypeId"))

        v12.execSQL("DELETE FROM component_types WHERE id=$testTypeId")
        assertEquals(0, v12.countWhere("component_values", "component_type_id=$testTypeId"))
        assertEquals(1, v12.countWhere("component_values", "lexeme_id=1"))
        v12.close()
    }

    // === Case H — json_insert append: порядок [translation, Definition] ===
    @Test
    fun caseH_jsonInsertAppendOrder() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.insertLexeme(id = 1, wordId = 1, translation = "кошка", definition = "pet")
        }
        val v12 = migrate()
        assertEquals(
            "translation",
            v12.scalarText(
                "SELECT json_extract(component_refs, '${'$'}[0].key') FROM quiz_configs WHERE dictionary_id=1 AND quiz_mode='write'",
            ),
        )
        assertEquals(
            "Definition",
            v12.scalarText(
                "SELECT json_extract(component_refs, '${'$'}[1].name') FROM quiz_configs WHERE dictionary_id=1 AND quiz_mode='write'",
            ),
        )
        v12.close()
    }

    // === Case J — Orphan lexeme (translation IS NULL AND definition IS NULL) ===
    @Test
    fun caseJ_orphanLexeme() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.insertLexeme(id = 1, wordId = 1, translation = null, definition = null)
        }
        val v12 = migrate()
        assertEquals(0, v12.countWhere("component_values", "lexeme_id=1"))
        assertEquals(1, v12.countWhere("lexemes", "id=1"))
        v12.close()
    }

    // === Case K — Special chars в translation/definition (финальный path) ===
    @Test
    fun caseK_specialChars() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.execSQL(
                "INSERT INTO lexemes (id, word_id, translation, definition, options, add_date) " +
                    "VALUES (1, 1, 'she said \"hello\"', 'строка с" + "\n" + "переносом 😀', 0, 0)",
            )
        }
        val v12 = migrate()
        assertEquals(
            "she said \"hello\"",
            v12.scalarText(
                "SELECT json_extract(value, '${'$'}.fields.value.value') FROM component_values " +
                    "WHERE lexeme_id=1 AND component_type_id=(SELECT id FROM component_types WHERE system_key='translation')",
            ),
        )
        assertEquals(
            "строка с\nпереносом 😀",
            v12.scalarText(
                "SELECT json_extract(value, '${'$'}.fields.value.value') FROM component_values " +
                    "WHERE lexeme_id=1 AND component_type_id=(SELECT id FROM component_types WHERE name='Definition' AND dictionary_id=1)",
            ),
        )
        v12.close()
    }

    // === Case L — UNIQUE (dictionary_id, quiz_mode) на quiz_configs ===
    @Test
    fun caseL_uniqueQuizMode() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
        }
        val v12 = migrate()
        var thrown = false
        try {
            v12.execSQL("INSERT INTO quiz_configs (dictionary_id, quiz_mode, component_refs) VALUES (1, 'write', '[]')")
        } catch (_: Exception) {
            thrown = true
        }
        assertTrue("UNIQUE constraint must throw", thrown)
        v12.close()
    }

    // === Case M — timestamps backfill (нет 0) + removed_at вместо remove_date ===
    @Test
    fun caseM_timestampsAndRemovedAtColumn() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.insertLexeme(id = 1, wordId = 1, translation = "кошка", definition = "pet")
        }
        val v12 = migrate()

        assertEquals(0, v12.countWhere("component_types", "created_at = 0 OR updated_at = 0"))
        assertEquals(0, v12.countWhere("component_values", "created_at = 0 OR updated_at = 0"))
        // remove_date колонки нет, removed_at есть.
        assertEquals(0, v12.countWhere("pragma_table_info('component_types')", "name='remove_date'"))
        assertEquals(1, v12.countWhere("pragma_table_info('component_types')", "name='removed_at'"))
        assertEquals(1, v12.countWhere("pragma_table_info('component_types')", "name='is_multiple'"))
        v12.close()
    }

    // === Case N — DROP UNIQUE (lexeme_id, component_type_id): multi values разрешены ===
    @Test
    fun caseN_multipleValuesPerLexemeType() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.insertLexeme(id = 1, wordId = 1, translation = "кошка", definition = null)
        }
        val v12 = migrate()
        val now = System.currentTimeMillis()
        val translationTypeId = v12.scalarLong("SELECT id FROM component_types WHERE system_key='translation'")
        // Второй value на ту же (lexeme_id=1, component_type_id=translation) — больше не падает.
        v12.execSQL(
            "INSERT INTO component_values (lexeme_id, component_type_id, value, created_at, updated_at) " +
                "VALUES (1, $translationTypeId, '{\"fields\":{\"value\":{\"type\":\"text\",\"value\":\"second\"}}}', $now, $now)",
        )
        assertEquals(2, v12.countWhere("component_values", "lexeme_id=1 AND component_type_id=$translationTypeId"))
        v12.close()
    }

    // === Case O — DROP UNIQUE (dictionary_id, name): дубль типа разрешён ===
    @Test
    fun caseO_duplicateTypeNamePerDict() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.insertLexeme(id = 1, wordId = 1, translation = null, definition = "def")
        }
        val v12 = migrate()
        val now = System.currentTimeMillis()
        // Второй 'Definition' в том же словаре — больше не падает (UNIQUE снят).
        v12.execSQL(
            """
            INSERT INTO component_types (system_key, dictionary_id, name, template_key, position, is_multiple, created_at, updated_at)
            VALUES (NULL, 1, 'Definition', 'text', 0, 0, $now, $now)
            """.trimIndent(),
        )
        assertEquals(2, v12.countWhere("component_types", "dictionary_id=1 AND name='Definition'"))
        v12.close()
    }

    // === Case P — index_component_values_lexeme_id есть, старые composite UNIQUE сняты ===
    @Test
    fun caseP_indexes() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
        }
        val v12 = migrate()
        assertEquals(
            "index",
            v12.scalarText("SELECT type FROM sqlite_master WHERE name='index_component_values_lexeme_id'"),
        )
        assertEquals(0, v12.countWhere("sqlite_master", "name='index_component_values_lexeme_id_component_type_id'"))
        assertEquals(0, v12.countWhere("sqlite_master", "name='index_component_types_dictionary_id_name'"))
        v12.close()
    }

    // ============================================================
    // IS486 (collapsed в тот же переход): иерархия компонентов,
    // пословарные builtin, опции части речи. phase1_plan.md § Зона B.
    // ============================================================

    // === Case Q — v12-схема: новые колонки, индексы, не-UNIQUE system_key ===
    @Test
    fun caseQ_is486Schema() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
        }
        val v12 = migrate()
        assertEquals(1, v12.countWhere("sqlite_master", "type='table' AND name='component_options'"))
        listOf("core", "enabled", "depends_on_type_id", "depends_on_option_id").forEach { col ->
            assertEquals("missing column $col", 1, v12.countWhere("pragma_table_info('component_types')", "name='$col'"))
        }
        assertEquals(1, v12.countWhere("pragma_table_info('component_values')", "name='option_id'"))
        val indexSql = v12.scalarText("SELECT sql FROM sqlite_master WHERE name='index_component_types_system_key'")
        assertTrue("system_key index must not be UNIQUE", !indexSql.uppercase().contains("UNIQUE"))
        listOf(
            "index_component_types_depends_on_type_id",
            "index_component_types_depends_on_option_id",
            "index_component_values_option_id",
            "index_component_options_component_type_id",
        ).forEach { idx ->
            assertEquals("missing index $idx", 1, v12.countWhere("sqlite_master", "type='index' AND name='$idx'"))
        }
        v12.close()
    }

    // === Case R — пословарные builtin: перевод-ядро + часть речи с опциями на каждый словарь ===
    @Test
    fun caseR_perDictionaryBuiltIns() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "ES")
            v11.insertDictionary(id = 2, name = "EN")
        }
        val v12 = migrate()
        // Перевод: по строке на словарь, ядро, включён, цель-лексема; глобальной нет.
        assertEquals(2, v12.countWhere("component_types", "system_key='translation'"))
        assertEquals(0, v12.countWhere("component_types", "system_key='translation' AND dictionary_id IS NULL"))
        assertEquals(
            2,
            v12.countWhere(
                "component_types",
                "system_key='translation' AND core=1 AND enabled=1 " +
                    "AND depends_on_type_id IS NULL AND depends_on_option_id IS NULL",
            ),
        )
        // Часть речи: CHOICE, не-ядро, по строке на словарь.
        assertEquals(
            2,
            v12.countWhere(
                "component_types",
                "system_key='part_of_speech' AND template_key='choice' AND core=0 AND enabled=1",
            ),
        )
        // 6 опций-ключей на каждый словарь, label NULL, позиции стабильны.
        listOf(1L, 2L).forEach { dictId ->
            val posTypeId = v12.scalarLong(
                "SELECT id FROM component_types WHERE system_key='part_of_speech' AND dictionary_id=$dictId",
            )
            assertEquals(6, v12.countWhere("component_options", "component_type_id=$posTypeId"))
            assertEquals(
                6,
                v12.countWhere(
                    "component_options",
                    "component_type_id=$posTypeId AND label IS NULL AND system_key IN " +
                        "('noun','verb','adjective','adverb','preposition','phrase')",
                ),
            )
            assertEquals(
                "noun",
                v12.scalarText("SELECT system_key FROM component_options WHERE component_type_id=$posTypeId AND position=0"),
            )
        }
        v12.close()
    }

    // === Case S — перевязка значений перевода на строку своего словаря ===
    @Test
    fun caseS_translationValuesPerDictionary() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "ES")
            v11.insertDictionary(id = 2, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "gato")
            v11.insertWord(id = 2, dictionaryId = 2, value = "cat")
            v11.insertLexeme(id = 1, wordId = 1, translation = "кот", definition = null)
            v11.insertLexeme(id = 2, wordId = 2, translation = "кошка", definition = null)
        }
        val v12 = migrate()
        val d1TranslationId = v12.scalarLong("SELECT id FROM component_types WHERE system_key='translation' AND dictionary_id=1")
        val d2TranslationId = v12.scalarLong("SELECT id FROM component_types WHERE system_key='translation' AND dictionary_id=2")
        assertEquals(d1TranslationId, v12.scalarLong("SELECT component_type_id FROM component_values WHERE lexeme_id=1"))
        assertEquals(d2TranslationId, v12.scalarLong("SELECT component_type_id FROM component_values WHERE lexeme_id=2"))
        v12.close()
    }

    // === Case T — backfill: Definition-ядро при лексеме с definition без translation ===
    @Test
    fun caseT_definitionCoreWhenUsedAlone() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            // Лексема с definition БЕЗ translation → Definition фактически используется сам.
            v11.insertLexeme(id = 1, wordId = 1, translation = null, definition = "def")
        }
        val v12 = migrate()
        assertEquals(
            1,
            v12.countWhere(
                "component_types",
                "name='Definition' AND dictionary_id=1 AND core=1 " +
                    "AND depends_on_type_id IS NULL AND depends_on_option_id IS NULL AND enabled=1",
            ),
        )
        v12.close()
    }

    // === Case U — backfill: Definition-зависимость от перевода, когда все def-лексемы с переводом ===
    @Test
    fun caseU_definitionDependsOnTranslationWhenAlwaysPaired() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
            v11.insertWord(id = 1, dictionaryId = 1, value = "cat")
            v11.insertLexeme(id = 1, wordId = 1, translation = "кошка", definition = "pet")
        }
        val v12 = migrate()
        val translationId = v12.scalarLong("SELECT id FROM component_types WHERE system_key='translation' AND dictionary_id=1")
        assertEquals(
            1,
            v12.countWhere(
                "component_types",
                "name='Definition' AND dictionary_id=1 AND core=0 AND depends_on_type_id=$translationId",
            ),
        )
        v12.close()
    }

    // === Case V — вырожденная БД: 0 словарей — builtin сеять некуда ===
    @Test
    fun caseV_zeroDictionaries() {
        helper.createDatabase(11).use { }
        val v12 = migrate()
        assertEquals(0, v12.countWhere("component_types", "1=1"))
        assertEquals(0, v12.countWhere("component_options", "1=1"))
        v12.close()
    }

    // === Case W — словарь без лексем: builtin сеются, значений нет ===
    @Test
    fun caseW_emptyDictionarySeeded() {
        helper.createDatabase(11).use { v11 ->
            v11.insertDictionary(id = 1, name = "EN")
        }
        val v12 = migrate()
        assertEquals(1, v12.countWhere("component_types", "system_key='translation' AND dictionary_id=1 AND core=1"))
        assertEquals(1, v12.countWhere("component_types", "system_key='part_of_speech' AND dictionary_id=1"))
        assertEquals(6, v12.countWhere("component_options", "1=1"))
        assertEquals(0, v12.countWhere("component_values", "1=1"))
        v12.close()
    }

    companion object {
        private const val DB_NAME = "migration-test"
    }
}

// === Helpers ===

private fun SQLiteConnection.insertDictionary(id: Long, name: String) {
    execSQL("INSERT INTO dictionaries (id, numericCode, name, addDate) VALUES ($id, NULL, '$name', 0)")
}

private fun SQLiteConnection.insertWord(id: Long, dictionaryId: Long, value: String) {
    execSQL("INSERT INTO words (id, dictionary_id, value, add_date) VALUES ($id, $dictionaryId, '$value', 0)")
}

private fun SQLiteConnection.insertLexeme(id: Long, wordId: Long, translation: String?, definition: String?) {
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
