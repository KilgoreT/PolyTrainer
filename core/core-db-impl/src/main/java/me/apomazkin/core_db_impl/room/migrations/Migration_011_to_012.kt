package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * IS481+IS486 collapsed migration M11 → M12 (single step).
 *
 * Последний деплой-тег (0.1.5) — на v11; промежуточная IS481-схема нигде не
 * релизилась (жила только на dev-девайсе — осознанная переустановка), поэтому
 * IS486-иерархия схлопнута прямо в M11→M12 (решение 2026-07-19; дом-прецедент
 * схлопывания — IS481). Таблицы создаются СРАЗУ в финальной v12-форме:
 *  - component_types: + core / enabled / depends_on_type_id / depends_on_option_id,
 *    system_key индекс НЕ unique (builtin пословарные);
 *  - component_values: + option_id;
 *  - component_options: таблица опций CHOICE (builtin-опции — ключами, label NULL).
 *
 * Builtin — пословарные (spec §4): на каждый словарь сеются свой «Перевод»
 * (TEXT, ядро) и своя «Часть речи» (CHOICE, не-ядро) с 6 опциями-ключами
 * (noun, verb, adjective, adverb, preposition, phrase) — display резолвится
 * из ресурсов. Глобальных builtin-строк не существует.
 *
 * Backfill иерархии (spec §11): Definition-тип — ядро, если в словаре есть
 * лексема с definition без translation (фактически используется сам);
 * иначе — зависимость от перевода своего словаря.
 *
 * Critical порядок шагов (failAfterStep — test-hook идемпотентности):
 *   1. CREATE component_types (финальная форма) + 4 индекса.
 *   2. CREATE component_values (+option_id) + 3 индекса.
 *   3. CREATE component_options + индекс.
 *   4. CREATE quiz_configs + индексы.
 *   5. Seed пословарных «Перевод» (ядра).
 *   6. Seed пословарных «Часть речи» + 6 опций-ключей на словарь.
 *   7. CREATE per-dictionary user-defined "Definition" types (core=0).
 *   8. Backfill: Definition → core=1 где есть лексема с definition без translation.
 *   9. Backfill: остальные Definition → depends_on_type_id = перевод своего словаря.
 *  10. INSERT translation data в component_values (пословарный тип, JOIN words).
 *  11. INSERT definition data в component_values.
 *  12. INSERT default quiz_configs для всех словарей (F1 invariant).
 *  13. UPDATE quiz_configs: + UserDefined("Definition") для словарей с definition.
 *      ВАЖНО: до шага 14 — SELECT из lexemes.definition.
 *  14. ALTER TABLE lexemes DROP COLUMN translation / definition.
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

        createComponentOptionsTable(connection)
        maybeFail(3, failAfterStep)

        createQuizConfigsTable(connection)
        maybeFail(4, failAfterStep)

        seedTranslationPerDictionary(connection, now)
        maybeFail(5, failAfterStep)

        seedPartOfSpeechPerDictionary(connection, now)
        maybeFail(6, failAfterStep)

        createUserDefinedDefinitionTypes(connection, now)
        maybeFail(7, failAfterStep)

        backfillDefinitionCore(connection)
        maybeFail(8, failAfterStep)

        backfillDefinitionDependency(connection)
        maybeFail(9, failAfterStep)

        migrateTranslationData(connection, now)
        maybeFail(10, failAfterStep)

        migrateDefinitionData(connection, now)
        maybeFail(11, failAfterStep)

        insertDefaultQuizConfigsForAllDictionaries(connection)
        maybeFail(12, failAfterStep)

        addDefinitionToQuizConfigsForDictionariesWithDefinitionData(connection)
        maybeFail(13, failAfterStep)

        connection.execSQL("ALTER TABLE lexemes DROP COLUMN translation")
        connection.execSQL("ALTER TABLE lexemes DROP COLUMN definition")
        maybeFail(14, failAfterStep)
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
                `core` INTEGER NOT NULL DEFAULT 0,
                `enabled` INTEGER NOT NULL DEFAULT 1,
                `depends_on_type_id` INTEGER,
                `depends_on_option_id` INTEGER,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `removed_at` INTEGER,
                FOREIGN KEY(`dictionary_id`) REFERENCES `dictionaries`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`depends_on_type_id`) REFERENCES `component_types`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`depends_on_option_id`) REFERENCES `component_options`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_component_types_dictionary_id` ON `component_types` (`dictionary_id`)"
        )
        // IS486: НЕ unique — builtin пословарные, уникальность «(ключ, словарь)» в UseCase.
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_component_types_system_key` ON `component_types` (`system_key`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_component_types_depends_on_type_id` ON `component_types` (`depends_on_type_id`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_component_types_depends_on_option_id` ON `component_types` (`depends_on_option_id`)"
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
                `option_id` INTEGER,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `removed_at` INTEGER,
                FOREIGN KEY(`lexeme_id`) REFERENCES `lexemes`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`component_type_id`) REFERENCES `component_types`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`option_id`) REFERENCES `component_options`(`id`)
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
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_component_values_option_id` ON `component_values` (`option_id`)"
        )
    }

    private fun createComponentOptionsTable(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `component_options` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `component_type_id` INTEGER NOT NULL,
                `system_key` TEXT,
                `label` TEXT,
                `position` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `removed_at` INTEGER,
                FOREIGN KEY(`component_type_id`) REFERENCES `component_types`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_component_options_component_type_id` ON `component_options` (`component_type_id`)"
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

    // === Seed пословарных builtin (spec §4/§11) ===

    private fun seedTranslationPerDictionary(connection: SQLiteConnection, now: Long) {
        connection.execSQL(
            """
            INSERT INTO component_types
                (system_key, dictionary_id, name, template_key, position, is_multiple,
                 core, enabled, depends_on_type_id, depends_on_option_id, created_at, updated_at, removed_at)
            SELECT 'translation', d.id, NULL, 'text', 0, 0,
                   1, 1, NULL, NULL, $now, $now, NULL
            FROM dictionaries d
            """.trimIndent()
        )
    }

    private fun seedPartOfSpeechPerDictionary(connection: SQLiteConnection, now: Long) {
        connection.execSQL(
            """
            INSERT INTO component_types
                (system_key, dictionary_id, name, template_key, position, is_multiple,
                 core, enabled, depends_on_type_id, depends_on_option_id, created_at, updated_at, removed_at)
            SELECT 'part_of_speech', d.id, NULL, 'choice', 1, 0,
                   0, 1, NULL, NULL, $now, $now, NULL
            FROM dictionaries d
            """.trimIndent()
        )
        // Опции — ключами (display из ресурсов), label NULL, позиции 0..5.
        listOf(
            "noun" to 0,
            "verb" to 1,
            "adjective" to 2,
            "adverb" to 3,
            "preposition" to 4,
            "phrase" to 5,
        ).forEach { (key, position) ->
            connection.execSQL(
                """
                INSERT INTO component_options
                    (component_type_id, system_key, label, position, created_at, updated_at, removed_at)
                SELECT ct.id, '$key', NULL, $position, $now, $now, NULL
                FROM component_types ct
                WHERE ct.system_key = 'part_of_speech'
                """.trimIndent()
            )
        }
    }

    // === Definition-типы + backfill иерархии ===

    private fun createUserDefinedDefinitionTypes(connection: SQLiteConnection, now: Long) {
        connection.execSQL(
            """
            INSERT INTO component_types
                (system_key, dictionary_id, name, template_key, position, is_multiple,
                 core, enabled, depends_on_type_id, depends_on_option_id, created_at, updated_at, removed_at)
            SELECT DISTINCT NULL, w.dictionary_id, 'Definition', 'text', 10, 0,
                   0, 1, NULL, NULL, $now, $now, NULL
            FROM words w
            JOIN lexemes l ON l.word_id = w.id
            WHERE l.definition IS NOT NULL
            """.trimIndent()
        )
    }

    private fun backfillDefinitionCore(connection: SQLiteConnection) {
        // Ядро — если в словаре есть лексема с definition без translation
        // (компонент фактически используется сам, spec §11).
        connection.execSQL(
            """
            UPDATE component_types
            SET core = 1
            WHERE system_key IS NULL
              AND name = 'Definition'
              AND dictionary_id IN (
                SELECT DISTINCT w.dictionary_id
                FROM words w
                JOIN lexemes l ON l.word_id = w.id
                WHERE l.definition IS NOT NULL AND l.translation IS NULL
              )
            """.trimIndent()
        )
    }

    private fun backfillDefinitionDependency(connection: SQLiteConnection) {
        // Остальные Definition — зависимость от перевода своего словаря.
        connection.execSQL(
            """
            UPDATE component_types
            SET depends_on_type_id = (
                SELECT t.id FROM component_types t
                WHERE t.system_key = 'translation'
                  AND t.dictionary_id = component_types.dictionary_id
            )
            WHERE system_key IS NULL
              AND name = 'Definition'
              AND core = 0
            """.trimIndent()
        )
    }

    // === Data migration (v11-колонки → component_values) ===

    private fun migrateTranslationData(connection: SQLiteConnection, now: Long) {
        // Пословарный тип перевода: JOIN words даёт словарь лексемы.
        connection.execSQL(
            """
            INSERT INTO component_values (lexeme_id, component_type_id, value, option_id, created_at, updated_at, removed_at)
            SELECT
                l.id,
                (SELECT ct.id FROM component_types ct
                 WHERE ct.system_key = 'translation' AND ct.dictionary_id = w.dictionary_id),
                json_object('fields', json_object('value', json_object('type', 'text', 'value', l.translation))),
                NULL, $now, $now, NULL
            FROM lexemes l
            JOIN words w ON l.word_id = w.id
            WHERE l.translation IS NOT NULL
            """.trimIndent()
        )
    }

    private fun migrateDefinitionData(connection: SQLiteConnection, now: Long) {
        connection.execSQL(
            """
            INSERT INTO component_values (lexeme_id, component_type_id, value, option_id, created_at, updated_at, removed_at)
            SELECT
                l.id,
                (SELECT ct.id FROM component_types ct
                 WHERE ct.dictionary_id = w.dictionary_id
                   AND ct.name = 'Definition'
                   AND ct.system_key IS NULL),
                json_object('fields', json_object('value', json_object('type', 'text', 'value', l.definition))),
                NULL, $now, $now, NULL
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
        // SELECT'ит из lexemes.definition — обязан выполниться ДО DROP COLUMN definition.
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
