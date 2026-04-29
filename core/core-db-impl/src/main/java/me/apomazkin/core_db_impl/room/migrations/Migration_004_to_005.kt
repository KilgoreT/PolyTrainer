package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration_4_5 = object : Migration(4, 5) {

    override fun migrate(db: SupportSQLiteDatabase) {

        val defaultId = 0
        val code = "en"
        val name = "English"
        val currentTime = System.currentTimeMillis()

        db.execSQL(
            "CREATE TABLE `languages`"
                    + " ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "`code` TEXT NOT NULL,"
                    + "`name` TEXT,"
                    + "`addDate` INTEGER NOT NULL,"
                    + "`changeDate` INTEGER "
                    + ");"
        )
        db.execSQL(
            "INSERT INTO languages (id, code, name, addDate) VALUES ($defaultId, '$code', '$name', $currentTime)"
        )
        db.execSQL("ALTER TABLE words ADD COLUMN langId INTEGER NOT NULL default $defaultId")
        db.execSQL("ALTER TABLE writeQuiz ADD COLUMN langId INTEGER NOT NULL default $defaultId")

    }
}