package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * IS508 migration M13 → M14: unique-индекс `write_quiz.lexeme_id`.
 *
 * Инвариант «одна лексема — одна квиз-строка» до v14 держался только
 * построением (квиз-строка создаётся атомарно с лексемой в
 * `WordDao.addLexemeWithQuiz` / `addLexemeWithComponents`); на нём стоит
 * сборка порции квиза без повторов. С v14 его гарантирует схема.
 *
 * Шаги:
 *  1. схлопнуть дубли, если есть — остаётся старейшая строка (MIN(id));
 *     на легальных базах строк-дублей нет, шаг no-op. Нужен, чтобы
 *     CREATE UNIQUE INDEX не упал на повреждённой/восстановленной базе;
 *  2. пересоздать индекс под тем же именем `index_write_quiz_lexeme_id`
 *     (имя — как в экспорте Room, иначе валидация схемы не пройдёт):
 *     Room не умеет «изменить индекс» — только DROP + CREATE.
 *
 * `IF EXISTS` / `IF NOT EXISTS` — идемпотентность (прецедент 12→13),
 * Room оборачивает migrate() в транзакцию.
 */
object Migration_013_to_014 : Migration(13, 14) {

    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            DELETE FROM write_quiz
            WHERE id NOT IN (SELECT MIN(id) FROM write_quiz GROUP BY lexeme_id)
            """.trimIndent(),
        )
        connection.execSQL("DROP INDEX IF EXISTS `index_write_quiz_lexeme_id`")
        connection.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_write_quiz_lexeme_id` ON `write_quiz` (`lexeme_id`)",
        )
    }
}
