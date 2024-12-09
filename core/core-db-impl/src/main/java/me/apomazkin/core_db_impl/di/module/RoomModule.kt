package me.apomazkin.core_db_impl.di.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import me.apomazkin.core_db_impl.room.Database
import me.apomazkin.core_db_impl.room.WordDao
import me.apomazkin.core_db_impl.room.migrations.migration_1_2
import me.apomazkin.core_db_impl.room.migrations.migration_2_3
import me.apomazkin.core_db_impl.room.migrations.migration_3_4
import me.apomazkin.core_db_impl.room.migrations.migration_4_5
import me.apomazkin.core_db_impl.room.migrations.migration_5_6
import me.apomazkin.core_db_impl.room.migrations.migration_6_7
import me.apomazkin.core_db_impl.room.migrations.migration_7_8

@Module
class RoomModule {

    // TODO: 12.03.2021 поправить имя базы, при изменении имени информация теряется.
    @Provides
    fun provideDatabase(context: Context): Database {
        return Room.databaseBuilder(context, Database::class.java, "name")
            .addMigrations(
                migration_1_2,
                migration_2_3,
                migration_3_4,
                migration_4_5,
                migration_5_6,
                migration_6_7,
                migration_7_8,
            )
            .build()
    }

    @Provides
    fun provideWordDao(db: Database): WordDao {
        return db.wordDao()
    }

}