package me.apomazkin.core_db_impl.room.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val migration_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val currentTimeMillis = System.currentTimeMillis()
        db.execSQL("ALTER TABLE lexemes ADD COLUMN addDate INTEGER NOT NULL DEFAULT $currentTimeMillis")
        db.execSQL("ALTER TABLE lexemes ADD COLUMN changeDate INTEGER")
        db.execSQL("ALTER TABLE lexemes ADD COLUMN removeDate INTEGER")
    }
}