package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE `writeQuiz`"
                    + " ("
                    + "`id` INTEGER NOT NULL,"
                    + "`definitionId` INTEGER NOT NULL,"
                    + "`grade` INTEGER NOT NULL,"
                    + "`score` INTEGER NOT NULL,"
                    + "PRIMARY KEY(`id`)"
                    + ")"
        )

        database.execSQL(
            "INSERT INTO writeQuiz (id, definitionId, grade, score) SELECT id, id as definitionId, 0 as grade, 0 as score FROM definitions"
        )
    }
}