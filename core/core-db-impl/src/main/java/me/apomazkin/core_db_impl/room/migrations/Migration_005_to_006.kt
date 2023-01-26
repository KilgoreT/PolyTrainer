package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration_5_6 = object : Migration(5, 6) {

    val defaultId = 1
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE languages ADD COLUMN numericCode INTEGER NOT NULL default $defaultId")
    }
}