package me.apomazkin.core_db_impl.room

import androidx.room.Database
import androidx.room.RoomDatabase
import me.apomazkin.core_db_impl.entity.DefinitionDb
import me.apomazkin.core_db_impl.entity.WordDb

@Database(entities = [WordDb::class, DefinitionDb::class], version = 1)
abstract class Database : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun definitionDao(): DefinitionDao
}