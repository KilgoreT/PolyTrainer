package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import me.apomazkin.core_db_api.entity.DictionaryLanguageDefaults

/**
 * IS525 migration M15 → M16: языки словаря.
 *
 * Шаги:
 *  1. две обязательные колонки `learning_language`, `translation_language`
 *     (TEXT NOT NULL DEFAULT 'en' — иначе ALTER TABLE в SQLite невозможен);
 *  2. изучаемый язык: по каждому коду страны, что есть в таблице, —
 *     [DictionaryLanguageDefaults.learningLanguageFor]; словари без флага —
 *     `learningLanguageFor(null)` (английский);
 *  3. язык перевода всем — [DictionaryLanguageDefaults.translationLanguage].
 *
 * В отличие от прежних миграций — класс, не object: правила языков
 * приходят из app через конструктор (см. RoomModule).
 */
class Migration_015_to_016(
    private val languageDefaults: DictionaryLanguageDefaults,
) : Migration(15, 16) {

    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `dictionaries` ADD COLUMN `learning_language` TEXT NOT NULL DEFAULT 'en'",
        )
        connection.execSQL(
            "ALTER TABLE `dictionaries` ADD COLUMN `translation_language` TEXT NOT NULL DEFAULT 'en'",
        )
        connection.fillLearningLanguages()
        connection.fillTranslationLanguage()
    }

    private fun SQLiteConnection.fillLearningLanguages() {
        val numericCodes = mutableListOf<Long>()
        prepare("SELECT DISTINCT numericCode FROM dictionaries WHERE numericCode IS NOT NULL").use { stmt ->
            while (stmt.step()) numericCodes += stmt.getLong(0)
        }
        numericCodes.forEach { code ->
            // Правила приходят из app; их ошибка не должна ронять открытие
            // базы (исключение в migrate = крэш на каждом старте): любое
            // исключение = «страна без языка» → английский.
            val language = runCatching { languageDefaults.learningLanguageFor(code.toInt()) }
                .getOrElse { languageDefaults.learningLanguageFor(null) }
            prepare("UPDATE dictionaries SET learning_language = ? WHERE numericCode = ?").use { stmt ->
                stmt.bindText(1, language)
                stmt.bindLong(2, code)
                stmt.step()
            }
        }
        prepare("UPDATE dictionaries SET learning_language = ? WHERE numericCode IS NULL").use { stmt ->
            stmt.bindText(1, languageDefaults.learningLanguageFor(null))
            stmt.step()
        }
    }

    private fun SQLiteConnection.fillTranslationLanguage() {
        prepare("UPDATE dictionaries SET translation_language = ?").use { stmt ->
            stmt.bindText(1, languageDefaults.translationLanguage())
            stmt.step()
        }
    }
}
