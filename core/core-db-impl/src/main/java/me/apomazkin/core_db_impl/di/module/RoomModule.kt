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
import me.apomazkin.core_db_impl.room.migrations.migration_8_9

/**
 * Migration plan:
 * 1. Create migration object. For instance, [migration_1_2].
 * 2. Add migration object to Room.databaseBuilder.
 * 3. Increment version in [Database] class.
 * 4. Edit entity classes in [Database] class.
 * 5. Create migration test. For instance, [me.apomazkin.core_db_impl.room.migrations.MigrationFrom08to09].
 * 6. Add migration test to [me.apomazkin.core_db_impl.room.AllMigrationTest].
 */
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
                migration_8_9,
            )
            .build()
    }

    @Provides
    fun provideWordDao(db: Database): WordDao {
        return db.wordDao()
    }

}