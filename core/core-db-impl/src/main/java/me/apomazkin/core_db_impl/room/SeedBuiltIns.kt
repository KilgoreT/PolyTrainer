package me.apomazkin.core_db_impl.room

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Seed built-in component types для fresh install path (schema v12).
 *
 * Идемпотентна через `INSERT OR IGNORE` (UNIQUE на `system_key`).
 * Built-in `translation` — `is_multiple = 0` (false),
 * `created_at = updated_at = System.currentTimeMillis()`, `removed_at = NULL`.
 *
 * **Используется только в `RoomDatabase.Callback.onCreate` (fresh install path).**
 * Upgrade-path с v11 — через единую `Migration_011_to_012` (она сама сеет
 * built-in translation сразу в финальной форме).
 */
internal fun seedBuiltIns(connection: SQLiteConnection) {
    val now = System.currentTimeMillis()
    connection.execSQL(
        """
        INSERT OR IGNORE INTO component_types
            (system_key, dictionary_id, name, template_key, position,
             is_multiple, created_at, updated_at, removed_at)
        VALUES ('translation', NULL, NULL, 'text', 0,
                0, $now, $now, NULL)
        """.trimIndent()
    )
}
