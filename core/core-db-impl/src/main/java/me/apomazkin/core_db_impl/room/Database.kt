package me.apomazkin.core_db_impl.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.apomazkin.core_db_impl.converters.DateTimeConverter
import me.apomazkin.core_db_impl.entity.ComponentOptionDb
import me.apomazkin.core_db_impl.entity.ComponentTypeDb
import me.apomazkin.core_db_impl.entity.ComponentValueDb
import me.apomazkin.core_db_impl.entity.DictionaryDb
import me.apomazkin.core_db_impl.entity.HintDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.QuizConfigDb
import me.apomazkin.core_db_impl.entity.SampleDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.entity.WriteQuizDb
import me.apomazkin.core_db_impl.room.dao.ComponentOptionDao
import me.apomazkin.core_db_impl.room.dao.ComponentTypeDao
import me.apomazkin.core_db_impl.room.dao.ComponentValueDao
import me.apomazkin.core_db_impl.room.dao.QuizConfigDao

@Database(
    entities = [
        WordDb::class,
        LexemeDb::class,
        HintDb::class,
        SampleDb::class,
        WriteQuizDb::class,
        DictionaryDb::class,
        ComponentTypeDb::class,
        ComponentValueDb::class,
        ComponentOptionDb::class,
        QuizConfigDb::class,
    ],
    version = 12
)
@TypeConverters(DateTimeConverter::class)
abstract class Database : RoomDatabase() {
    abstract fun wordDao(): WordDao
    abstract fun componentTypeDao(): ComponentTypeDao
    abstract fun componentValueDao(): ComponentValueDao
    abstract fun componentOptionDao(): ComponentOptionDao
    abstract fun quizConfigDao(): QuizConfigDao
}
