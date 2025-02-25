package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration_9_10 = object : Migration(9, 10) {
    
    override fun migrate(db: SupportSQLiteDatabase) {
        
        db.execSQL(
            """
            CREATE TABLE words_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                lang_id INTEGER NOT NULL,
                value TEXT NOT NULL,
                add_date INTEGER NOT NULL,
                change_date INTEGER,
                FOREIGN KEY (lang_id) REFERENCES languages(id) ON DELETE CASCADE
            )
        """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO words_new (
                id, lang_id, value, add_date, change_date
            )
            SELECT
                id,
                langId,
                COALESCE(value, ''),
                COALESCE(addDate, strftime('%s', 'now') * 1000),
                changeDate
            FROM words
        """.trimIndent()
        )
        db.execSQL("DROP TABLE words")
        db.execSQL("ALTER TABLE words_new RENAME TO words")
        db.execSQL("CREATE INDEX index_words_lang_id ON words(lang_id)")
        
        db.execSQL(
            """
            CREATE TABLE lexemes_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                word_id INTEGER NOT NULL,
                translation TEXT,
                definition TEXT,
                word_class TEXT,
                options INTEGER NOT NULL DEFAULT 0,
                add_date INTEGER NOT NULL,
                change_date INTEGER,
                FOREIGN KEY (word_id) REFERENCES words(id) ON DELETE CASCADE
            )
        """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO lexemes_new (
                id, word_id, translation, definition,
                word_class, options, add_date, change_date
            )
            SELECT
                id, wordId, translation, definition,
                wordClass, options, addDate, changeDate
            FROM lexemes
        """.trimIndent()
        )
        db.execSQL("DROP TABLE lexemes")
        db.execSQL("ALTER TABLE lexemes_new RENAME TO lexemes")
        db.execSQL("CREATE INDEX index_lexemes_word_id ON lexemes(word_id)")
        
        db.execSQL(
            """
            CREATE TABLE write_quiz (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                lang_id INTEGER NOT NULL,
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
            INSERT INTO write_quiz (
                 id, lang_id, lexeme_id, grade,
                 score, error_count, add_date, last_select_date
            )
            SELECT
                 id, langId, definitionId, grade,
                 score, 0 as error_count,
                 COALESCE(addDate, strftime('%s', 'now') * 1000),
                 lastSelectDate
            FROM writeQuiz
            """.trimIndent()
        )
        db.execSQL("DROP TABLE writeQuiz")
        db.execSQL("CREATE INDEX index_write_quiz_lexeme_id ON write_quiz(lexeme_id)")
    }
}