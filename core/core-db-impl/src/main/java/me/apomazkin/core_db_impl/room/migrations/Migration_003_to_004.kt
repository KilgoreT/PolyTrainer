package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration_3_4 = object : Migration(3, 4) {

    override fun migrate(database: SupportSQLiteDatabase) {

        database.execSQL(
            "CREATE TABLE `hint`"
                    + " ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "`definitionId` INTEGER NOT NULL,"
                    + "`value` TEXT NOT NULL,"
                    + "`addDate` INTEGER NOT NULL,"
                    + "`changeDate` INTEGER "
                    + ");"
        )

        database.execSQL(
            "CREATE TABLE `sample`"
                    + " ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "`definitionId` INTEGER NOT NULL,"
                    + "`value` TEXT NOT NULL,"
                    + "`source` TEXT,"
                    + "`addDate` INTEGER NOT NULL,"
                    + "`changeDate` INTEGER "
                    + ");"
        )

    }
}