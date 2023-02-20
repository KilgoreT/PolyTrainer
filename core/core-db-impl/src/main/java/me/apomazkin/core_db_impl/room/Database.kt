package me.apomazkin.core_db_impl.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.apomazkin.core_db_impl.converters.DateTimeConverter
import me.apomazkin.core_db_impl.entity.*

@Database(
    entities = [
        WordDb::class,
        DefinitionDb::class,
        HintDb::class,
        SampleDb::class,
        WriteQuizDb::class,
        LanguageDb::class
    ],
    version = 7
)
@TypeConverters(DateTimeConverter::class)
abstract class Database : RoomDatabase() {
    abstract fun wordDao(): WordDao
}