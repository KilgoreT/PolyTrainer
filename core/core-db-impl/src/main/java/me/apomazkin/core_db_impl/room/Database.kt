package me.apomazkin.core_db_impl.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Definition::class], version = 1)
abstract class Database : RoomDatabase() {
    abstract fun definitionDao(): DefinitionDao
}