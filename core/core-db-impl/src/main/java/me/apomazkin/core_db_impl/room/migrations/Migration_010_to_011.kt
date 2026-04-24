package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration_10_11 = object : Migration(10, 11) {

    override fun migrate(db: SupportSQLiteDatabase) {

        // === languages → dictionaries (drop "code" column) ===
        db.execSQL(
            """
            CREATE TABLE dictionaries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                numericCode INTEGER,
                name TEXT NOT NULL,
                addDate INTEGER NOT NULL,
                changeDate INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO dictionaries (id, numericCode, name, addDate, changeDate)
            SELECT id, numericCode, COALESCE(name, ''), addDate, changeDate
            FROM languages
            """.trimIndent()
        )
        db.execSQL("DROP TABLE languages")

        // === words: lang_id → dictionary_id ===
        db.execSQL(
            """
            CREATE TABLE words_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dictionary_id INTEGER NOT NULL,
                value TEXT NOT NULL,
                add_date INTEGER NOT NULL,
                change_date INTEGER,
                FOREIGN KEY (dictionary_id) REFERENCES dictionaries(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO words_new (id, dictionary_id, value, add_date, change_date)
            SELECT id, lang_id, value, add_date, change_date
            FROM words
            """.trimIndent()
        )
        db.execSQL("DROP TABLE words")
        db.execSQL("ALTER TABLE words_new RENAME TO words")
        db.execSQL("CREATE INDEX index_words_dictionary_id ON words(dictionary_id)")

        // === write_quiz: lang_id → dictionary_id ===
        db.execSQL(
            """
            CREATE TABLE write_quiz_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dictionary_id INTEGER NOT NULL,
                lexeme_id INTEGER NOT NULL,
                grade INTEGER NOT NULL,
                score INTEGER NOT NULL,
                error_count INTEGER NOT NULL,
                add_date INTEGER NOT NULL,
                last_select_date INTEGER,
                FOREIGN KEY (lexeme_id) REFERENCES lexemes(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO write_quiz_new (
                id, dictionary_id, lexeme_id, grade,
                score, error_count, add_date, last_select_date
            )
            SELECT
                id, lang_id, lexeme_id, grade,
                score, error_count, add_date, last_select_date
            FROM write_quiz
            """.trimIndent()
        )
        db.execSQL("DROP TABLE write_quiz")
        db.execSQL("ALTER TABLE write_quiz_new RENAME TO write_quiz")
        db.execSQL("CREATE INDEX index_write_quiz_lexeme_id ON write_quiz(lexeme_id)")
    }
}
