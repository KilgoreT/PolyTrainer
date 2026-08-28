package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * IS493 migration M12 → M13: группы (подсловари).
 *
 * Только DDL, data-backfill нет: обе таблицы создаются сразу в финальной
 * форме и ПУСТЫМИ — этапы Э3–Э7 фичи БД не трогают (концепт-правка
 * 2026-08-06: между этапами релизов не будет, миграция одна).
 *
 *  - dictionary_groups: дерево групп словаря (self-FK parent_group_id
 *    CASCADE — прецедент depends_on_type_id), soft-delete через removed_at;
 *    `kind` (Э4, 2026-08-23) — задел под будущую фичу папок: тип узла
 *    явный (в этой фиче всегда 'GROUP'), чтобы потом не угадывать тип
 *    пустых узлов по живым данным;
 *  - word_groups: membership, составной PK (word_id, group_id) — первая
 *    не-autoincrement PK-форма в проекте; INDEX(group_id) обязателен
 *    (PK покрывает только префикс word_id).
 *
 * DDL выверен против экспорта 13.json (порядок колонок PK, NOT NULL,
 * точное множество индексов). `IF NOT EXISTS` — defensive, как в
 * Migration_011_to_012; миграция идемпотентна по построению, Room
 * оборачивает migrate() в транзакцию.
 */
object Migration_012_to_013 : Migration(12, 13) {

    override fun migrate(connection: SQLiteConnection) {
        createDictionaryGroupsTable(connection)
        createWordGroupsTable(connection)
    }

    // === Schema creation (финальная форма — должна совпадать с 13.json) ===

    private fun createDictionaryGroupsTable(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `dictionary_groups` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `dictionary_id` INTEGER NOT NULL,
                `parent_group_id` INTEGER,
                `name` TEXT NOT NULL,
                `kind` TEXT NOT NULL DEFAULT 'GROUP',
                `created_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                `removed_at` INTEGER,
                FOREIGN KEY(`dictionary_id`) REFERENCES `dictionaries`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`parent_group_id`) REFERENCES `dictionary_groups`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_dictionary_groups_dictionary_id` ON `dictionary_groups` (`dictionary_id`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_dictionary_groups_parent_group_id` ON `dictionary_groups` (`parent_group_id`)"
        )
    }

    private fun createWordGroupsTable(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `word_groups` (
                `word_id` INTEGER NOT NULL,
                `group_id` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL,
                PRIMARY KEY(`word_id`, `group_id`),
                FOREIGN KEY(`word_id`) REFERENCES `words`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE,
                FOREIGN KEY(`group_id`) REFERENCES `dictionary_groups`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        // Ровно один индекс: PK покрывает префикс word_id, «симметричный»
        // индекс по word_id Room не экспортирует — не создавать.
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_word_groups_group_id` ON `word_groups` (`group_id`)"
        )
    }
}
