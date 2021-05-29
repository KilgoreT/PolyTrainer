package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration_2_3 = object : Migration(2, 3) {

    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL("ALTER TABLE words ADD COLUMN addDate INTEGER")
        database.execSQL("ALTER TABLE words ADD COLUMN changeDate INTEGER")
        val currentWordDate = System.currentTimeMillis() - 1_000_000
        database.execSQL("UPDATE words SET addDate = $currentWordDate")

        database.execSQL("ALTER TABLE writeQuiz ADD COLUMN addDate INTEGER")
        database.execSQL("ALTER TABLE writeQuiz ADD COLUMN lastSelectDate INTEGER")
        val currentQuizDate = System.currentTimeMillis() - 1_000_000
        database.execSQL("UPDATE writeQuiz SET addDate = $currentQuizDate")

    }
}