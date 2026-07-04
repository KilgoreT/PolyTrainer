package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * IS481 single migration M11 → M12 (collapsed).
 *
 * Объединяет бывшие M11→M12 + M12→M13 в один переход (v12/v13 нигде не релизились,
 * см. `docs/features/IS481_migration_collapse/brief.md`). Таблицы создаются СРАЗУ в
 * финальной форме (`is_multiple` + `created_at`/`updated_at`/`removed_at`, без UNIQUE
 * `(dictionary_id, name)` и без UNIQUE `(lexeme_id, component_type_id)`), а данные
 * translation/definition пишутся СРАЗУ в финальном JSON-envelope
 * `{"fields":{"value":{"type":"text","value":"..."}}}` — без промежуточного `{"v":1,...}`.
 *
 * Колонка cardinality называется `is_multiple` (Kotlin-поле `ComponentType.isMultiple`).
 *
 * Critical порядок:
 *   1. CREATE TABLE component_types (FK ready) + indexes.
 *   2. CREATE TABLE component_values + indexes.
 *   3. CREATE TABLE quiz_configs + indexes.
 *   4. seedBuiltIns: built-in translation (is_multiple=0, timestamps=now).
 *   5. Create per-dictionary user-defined "Definition" types (для словарей с definition data).
 *   6. INSERT translation data в component_values (M13 envelope).
 *   7. INSERT definition data в component_values (M13 envelope).
 *   8. INSERT default quiz_configs для ВСЕХ словарей (F1 invariant).
 *   9. UPDATE quiz_configs добавить UserDefined("Definition") для словарей с definition.
 *      ВАЖНО: step 9 ДО step 10 — SELECT из lexemes.definition колонки.
 *  10. ALTER TABLE lexemes DROP COLUMN translation; DROP COLUMN definition.
 *
 * Room оборачивает `migrate()` в транзакцию автоматически.
 */
object Migration_011_to_012 : Migration(11, 12) {

    override fun migrate(connection: SQLiteConnection) {
        migrateImpl(connection, failAfterStep = null)
    }

    /**
     * Internal impl с опциональной test-hook для idempotency теста.
     * Если `failAfterStep != null` — после шага бросается [MigrationTestFailureException];
     * Room ловит и откатывает транзакцию (user_version остаётся 11).
     */
    internal fun migrateImpl(connection: SQLiteConnection, failAfterStep: Int? = null) {
        val now = System.currentTimeMillis()

        createComponentTypesTable(connection)
        maybeFail(1, failAfterStep)

        createComponentValuesTable(connection)
        maybeFail(2, failAfterStep)

        createQuizConfigsTable(connection)
        maybeFail(3, failAfterStep)

        seedBuiltIns(connection, now)
        maybeFail(4, failAfterStep)

        createUserDefinedDefinitionTypes(connection, now)
        maybeFail(5, failAfterStep)

        migrateTranslationData(connection, now)
        maybeFail(6, failAfterStep)

        migrateDefinitionData(connection, now)
        maybeFail(7, failAfterStep)

        insertDefaultQuizConfigsForAllDictionaries(connection)
        maybeFail(8, failAfterStep)

        addDefinitionToQuizConfigsForDictionariesWithDefinitionData(connection)
        maybeFail(9, failAfterStep)

        connection.execSQL("ALTER TABLE lexemes DROP COLUMN translation")
        connection.execSQL("ALTER TABLE lexemes DROP COLUMN definition")
        maybeFail(10, failAfterStep)
    }

    private fun maybeFail(currentStep: Int, failAfterStep: Int?) {
        if (failAfterStep != null && currentStep == failAfterStep) {
            throw MigrationTestFailureException(currentStep)
        }
    }

    // === Schema creation (финальная форма — должна совпадать с 12.json) ===

    private fun createComponentTypesTable(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `component_types` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `system_key` TEXT,
                `dictionary_id` INTEGER,
                `name` TEXT,
                `template_key` TEXT NOT NULL,
                `position` INTEGER NOT NULL,
                `is_multiple` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `removed_at` INTEGER,
                FOREIGN KEY(`dictionary_id`) REFERENCES `dictionaries`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_component_types_dictionary_id` ON `component_types` (`dictionary_id`)"
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_component_types_system_key` ON `component_types` (`system_key`)"
        )
    }

    private fun createComponentValuesTable(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `component_values` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `lexeme_id` INTEGER NOT NULL,
                `component_type_id` INTEGER NOT NULL,
                `value` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `removed_at` INTEGER,
                FOREIGN KEY(`lexeme_id`) REFERENCES `lexemes`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`component_type_id`) REFERENCES `component_types`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_component_values_component_type_id` ON `component_values` (`component_type_id`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_component_values_lexeme_id` ON `component_values` (`lexeme_id`)"
        )
    }

    private fun createQuizConfigsTable(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `quiz_configs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `dictionary_id` INTEGER NOT NULL,
                `quiz_mode` TEXT NOT NULL,
                `component_refs` TEXT NOT NULL,
                FOREIGN KEY(`dictionary_id`) REFERENCES `dictionaries`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_quiz_configs_dictionary_id` ON `quiz_configs` (`dictionary_id`)"
        )
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_quiz_configs_dictionary_id_quiz_mode` ON `quiz_configs` (`dictionary_id`, `quiz_mode`)"
        )
    }

    // === Seed / data migration ===

    private fun seedBuiltIns(connection: SQLiteConnection, now: Long) {
        connection.execSQL(
            """
            INSERT OR IGNORE INTO component_types
                (system_key, dictionary_id, name, template_key, position, is_multiple, created_at, updated_at, removed_at)
            VALUES ('translation', NULL, NULL, 'text', 0, 0, $now, $now, NULL)
            """.trimIndent()
        )
    }

    private fun createUserDefinedDefinitionTypes(connection: SQLiteConnection, now: Long) {
        // Для каждого словаря, у которого хотя бы одна лексема с definition,
        // создаём user-defined "Definition" тип (is_multiple=0).
        connection.execSQL(
            """
            INSERT INTO component_types
                (system_key, dictionary_id, name, template_key, position, is_multiple, created_at, updated_at, removed_at)
            SELECT DISTINCT NULL, w.dictionary_id, 'Definition', 'text', 10, 0, $now, $now, NULL
            FROM words w
            JOIN lexemes l ON l.word_id = w.id
            WHERE l.definition IS NOT NULL
            """.trimIndent()
        )
    }

    private fun migrateTranslationData(connection: SQLiteConnection, now: Long) {
        // M13 envelope сразу: {"fields":{"value":{"type":"text","value":"<translation>"}}}.
        connection.execSQL(
            """
            INSERT INTO component_values (lexeme_id, component_type_id, value, created_at, updated_at, removed_at)
            SELECT
                l.id,
                (SELECT id FROM component_types WHERE system_key = 'translation'),
                json_object('fields', json_object('value', json_object('type', 'text', 'value', l.translation))),
                $now, $now, NULL
            FROM lexemes l
            WHERE l.translation IS NOT NULL
            """.trimIndent()
        )
    }

    private fun migrateDefinitionData(connection: SQLiteConnection, now: Long) {
        connection.execSQL(
            """
            INSERT INTO component_values (lexeme_id, component_type_id, value, created_at, updated_at, removed_at)
            SELECT
                l.id,
                (SELECT ct.id FROM component_types ct
                 WHERE ct.dictionary_id = w.dictionary_id
                   AND ct.name = 'Definition'
                   AND ct.system_key IS NULL),
                json_object('fields', json_object('value', json_object('type', 'text', 'value', l.definition))),
                $now, $now, NULL
            FROM lexemes l
            JOIN words w ON l.word_id = w.id
            WHERE l.definition IS NOT NULL
            """.trimIndent()
        )
    }

    private fun insertDefaultQuizConfigsForAllDictionaries(connection: SQLiteConnection) {
        // F1 invariant — даже пустой словарь получает default config [BuiltIn(TRANSLATION)].
        connection.execSQL(
            """
            INSERT INTO quiz_configs (dictionary_id, quiz_mode, component_refs)
            SELECT id, 'write', json_array(
                json_object('type', 'builtin', 'key', 'translation')
            )
            FROM dictionaries
            """.trimIndent()
        )
    }

    private fun addDefinitionToQuizConfigsForDictionariesWithDefinitionData(connection: SQLiteConnection) {
        // SELECT'ит из lexemes.definition — обязан выполниться ДО ALTER TABLE DROP COLUMN definition.
        connection.execSQL(
            """
            UPDATE quiz_configs
            SET component_refs = json_insert(
                component_refs,
                '${'$'}[#]',
                json_object('type', 'user', 'name', 'Definition')
            )
            WHERE quiz_mode = 'write'
              AND dictionary_id IN (
                SELECT DISTINCT w.dictionary_id
                FROM words w JOIN lexemes l ON l.word_id = w.id
                WHERE l.definition IS NOT NULL
              )
            """.trimIndent()
        )
    }
}

/**
 * Test-only exception для idempotency теста — инжектируется через
 * `Migration_011_to_012.migrateImpl(connection, failAfterStep = N)`.
 * Production `migrate(connection)` всегда передаёт `null`.
 */
internal class MigrationTestFailureException(val step: Int) :
    RuntimeException("Injected failure after migration step $step")
