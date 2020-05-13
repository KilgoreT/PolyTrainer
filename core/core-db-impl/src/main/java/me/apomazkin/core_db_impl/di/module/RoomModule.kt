package me.apomazkin.core_db_impl.di.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import me.apomazkin.core_db_impl.room.Database
import me.apomazkin.core_db_impl.room.DefinitionDao

@Module
class RoomModule {

    @Provides
    fun provideeee(context: Context): Database {
        val db = Room.databaseBuilder(context, Database::class.java, "name")
            .allowMainThreadQueries()
            .build()
        return db
    }

    @Provides
    fun provideDao(db: Database): DefinitionDao {
        return db.definitionDao()
    }
}