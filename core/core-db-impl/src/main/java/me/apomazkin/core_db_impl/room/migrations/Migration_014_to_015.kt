package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * IS515 migration M14 → M15: расширенный состав builtin «Часть речи».
 *
 * Схема не меняется — только данные. Для каждого типа
 * `system_key = 'part_of_speech'`:
 *  1. засеять 6 новых опций (местоимение, числительное, междометие,
 *     фразовый глагол, коллокация, идиома), если такого ключа у типа ещё
 *     нет — идемпотентно (`NOT EXISTS`);
 *  2. переставить позиции 12 builtin-опций в целевой порядок: сначала
 *     части речи, затем многословные единицы, «Фраза» последней.
 *
 * Ключи и позиции — литералами, НЕ из enum `PartOfSpeechOption`: миграция
 * фиксирует данные v15 и не должна меняться вместе с будущим enum
 * (прецедент 11→12). Своих опций у builtin-типа не бывает (add/rename/
 * delete → `BuiltInProtected`), сдвигать нечего. Значения лексем ссылаются
 * на опции по id — перестановка позиций их не трогает.
 */
object Migration_014_to_015 : Migration(14, 15) {

    /** Целевой порядок builtin-опций (ключ → позиция). */
    private val TARGET_POSITIONS: List<Pair<String, Int>> = listOf(
        "noun" to 0,
        "verb" to 1,
        "adjective" to 2,
        "adverb" to 3,
        "pronoun" to 4,
        "numeral" to 5,
        "preposition" to 6,
        "interjection" to 7,
        "phrasal_verb" to 8,
        "collocation" to 9,
        "idiom" to 10,
        "phrase" to 11,
    )

    /** Опции, которых не было до v15. */
    private val NEW_KEYS: Set<String> = setOf(
        "pronoun",
        "numeral",
        "interjection",
        "phrasal_verb",
        "collocation",
        "idiom",
    )

    override fun migrate(connection: SQLiteConnection) {
        val now = System.currentTimeMillis()
        TARGET_POSITIONS
            .filter { (key, _) -> key in NEW_KEYS }
            .forEach { (key, position) ->
                connection.execSQL(
                    """
                    INSERT INTO component_options
                        (component_type_id, system_key, label, position, created_at, updated_at, removed_at)
                    SELECT ct.id, '$key', NULL, $position, $now, $now, NULL
                    FROM component_types ct
                    WHERE ct.system_key = 'part_of_speech'
                      AND NOT EXISTS (
                          SELECT 1 FROM component_options co
                          WHERE co.component_type_id = ct.id AND co.system_key = '$key')
                    """.trimIndent(),
                )
            }
        val positionCase = TARGET_POSITIONS.joinToString(separator = " ") { (key, position) ->
            "WHEN '$key' THEN $position"
        }
        val keysList = TARGET_POSITIONS.joinToString(separator = ", ") { (key, _) -> "'$key'" }
        connection.execSQL(
            """
            UPDATE component_options
            SET position = CASE system_key $positionCase END,
                updated_at = $now
            WHERE system_key IN ($keysList)
              AND component_type_id IN (
                  SELECT id FROM component_types WHERE system_key = 'part_of_speech')
            """.trimIndent(),
        )
    }
}
