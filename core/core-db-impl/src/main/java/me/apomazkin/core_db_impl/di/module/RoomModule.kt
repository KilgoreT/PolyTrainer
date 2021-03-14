package me.apomazkin.core_db_impl.di.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import me.apomazkin.core_db_impl.room.Database
import me.apomazkin.core_db_impl.room.WordDao

@Module
class RoomModule {

    // TODO: 12.03.2021 поправить имя базы, при изменении имени информация теряется.
    @Provides
    fun provideeee(context: Context): Database {
        return Room.databaseBuilder(context, Database::class.java, "name")
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    fun provideWordDao(db: Database): WordDao {
        return db.wordDao()
    }

}