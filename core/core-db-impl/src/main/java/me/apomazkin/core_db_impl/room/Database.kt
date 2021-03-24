package me.apomazkin.core_db_impl.room

import androidx.room.Database
import androidx.room.RoomDatabase
import me.apomazkin.core_db_impl.entity.DefinitionDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb

@Database(entities = [WordDb::class, DefinitionDb::class, WriteQuizDb::class], version = 2)
abstract class Database : RoomDatabase() {
    abstract fun wordDao(): WordDao
}