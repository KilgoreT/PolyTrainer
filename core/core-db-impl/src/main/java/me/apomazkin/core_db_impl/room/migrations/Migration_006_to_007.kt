package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration_6_7 = object : Migration(6, 7) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_languages_numericCode ON languages (numericCode)")
    }
}