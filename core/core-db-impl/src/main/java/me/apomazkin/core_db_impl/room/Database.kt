package me.apomazkin.core_db_impl.room

import androidx.room.Database
import androidx.room.RoomDatabase
import me.apomazkin.core_db_impl.entity.WordDb

@Database(entities = [WordDb::class], version = 1)
abstract class Database : RoomDatabase() {
    abstract fun definitionDao(): DefinitionDao
}