package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration_7_8 = object : Migration(7, 8) {

    override fun migrate(db: SupportSQLiteDatabase) {

        // words: rename column word to value
        db.execSQL(
            """
            CREATE TABLE words_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                langId INTEGER NOT NULL,
                value TEXT,
                addDate INTEGER,
                changeDate INTEGER,
                removeDate INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO words_new (id, langId, value, addDate, changeDate)
            SELECT id, langId, word, addDate, changeDate
            FROM words
            """.trimIndent()
        )
        db.execSQL("DROP TABLE words")
        db.execSQL("ALTER TABLE words_new RENAME TO words")

        // Rename table definitions to lexemes
        db.execSQL(
            """
            CREATE TABLE lexemes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                wordId INTEGER,
                translation TEXT,
                definition TEXT,
                wordClass TEXT,
                options INTEGER NOT NULL
            )
        """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO lexemes (id, wordId, definition, wordClass, options)
            SELECT id, wordId, definition, wordClass, options FROM definitions
        """.trimIndent()
        )
        db.execSQL("DROP TABLE definitions")

        // Rename table sample to samples
        db.execSQL(
            """
            CREATE TABLE samples (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                lexemeId INTEGER,
                value TEXT NOT NULL,
                source TEXT,
                addDate INTEGER NOT NULL,
                changeDate INTEGER,
                removeDate INTEGER
            )
        """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO samples (id, lexemeId, value, source, addDate, changeDate)
            SELECT id, definitionId, value, source, addDate, changeDate FROM sample
        """.trimIndent()
        )
        db.execSQL("DROP TABLE sample")

        // Rename table hint to hints
        db.execSQL(
            """
            CREATE TABLE hints (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                lexemeId INTEGER,
                value TEXT NOT NULL,
                addDate INTEGER NOT NULL,
                changeDate INTEGER,
                removeDate INTEGER
            )
        """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO hints (id, lexemeId, value, addDate, changeDate)
            SELECT id, definitionId, value, addDate, changeDate FROM hint
        """.trimIndent()
        )
        db.execSQL("DROP TABLE hint")
    }
}