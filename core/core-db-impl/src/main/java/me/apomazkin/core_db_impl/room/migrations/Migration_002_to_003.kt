package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration_2_3 = object : Migration(2, 3) {

    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL("ALTER TABLE words ADD COLUMN addDate INTEGER")
        db.execSQL("ALTER TABLE words ADD COLUMN changeDate INTEGER")
        val currentWordDate = System.currentTimeMillis() - 1_000_000
        db.execSQL("UPDATE words SET addDate = $currentWordDate")

        db.execSQL("ALTER TABLE writeQuiz ADD COLUMN addDate INTEGER")
        db.execSQL("ALTER TABLE writeQuiz ADD COLUMN lastSelectDate INTEGER")
        val currentQuizDate = System.currentTimeMillis() - 1_000_000
        db.execSQL("UPDATE writeQuiz SET addDate = $currentQuizDate")

    }
}