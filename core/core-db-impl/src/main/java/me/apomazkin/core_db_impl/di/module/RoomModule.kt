package me.apomazkin.core_db_impl.di.module

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import me.apomazkin.core_db_impl.LogTags
import me.apomazkin.core_db_impl.room.Database
import me.apomazkin.core_db_impl.room.WordDao
import me.apomazkin.core_db_impl.room.dao.ComponentOptionDao
import me.apomazkin.core_db_impl.room.dao.ComponentTypeDao
import me.apomazkin.core_db_impl.room.dao.ComponentValueDao
import me.apomazkin.core_db_impl.room.dao.QuizConfigDao
import me.apomazkin.core_db_impl.room.migrations.Migration_011_to_012
import me.apomazkin.logger.LexemeLogger
import javax.inject.Singleton

/**
 * Текущая схема — v12 (IS481+IS486 collapsed). Одна миграция:
 * - M11→M12 (`Migration_011_to_012.kt`) — component_types / component_values /
 *   component_options / quiz_configs сразу в финальной форме (иерархия
 *   компонентов IS486: core / enabled / depends-ссылки; пословарные builtin
 *   перевод + часть речи с опциями-ключами) + migrate translation/definition
 *   из v11-колонок. Последний деплой-тег (0.1.5) — на v11; промежуточная
 *   IS481-схема v12 не релизилась → IS486 схлопнут в тот же переход
 *   (`docs/features/IS481_migration_collapse/brief.md` — прецедент).
 *
 * **Fallback на destructive migration**: если когда-то встретится install с БД
 * `user_version < 11` (pre-0.1.0 internal сборка) и без зарегистрированной миграции —
 * Room вместо crash дропает БД и пересоздаёт из current schema. Это **уничтожает данные
 * этого пользователя**, поэтому мы логируем событие через `LexemeLogger` с уровнем ERROR.
 * `CrashlyticsSink` (см. `app/.../logger/CrashlyticsSink.kt`) автоматически зарепортит
 * non-fatal в Firebase Crashlytics.
 *
 * **Fresh install path**: Room создаёт таблицы из `@Entity` annotations, миграция
 * не вызывается. IS486: builtin — пословарные и рождаются вместе со словарём
 * (seed в транзакции `addDictionary`, data-слой) — при создании БД сеять нечего,
 * `Callback.onCreate`-seed удалён.
 */
@Module
class RoomModule {

    // TODO: 12.03.2021 поправить имя базы, при изменении имени информация теряется.
    @Singleton
    @Provides
    fun provideDatabase(context: Context, logger: LexemeLogger): Database {
        return Room.databaseBuilder<Database>(
            context = context,
            name = DATABASE_NAME,
        )
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(Migration_011_to_012)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onDestructiveMigration(connection: SQLiteConnection) {
                    logger.e(
                        tag = LogTags.DB,
                        message = "Destructive migration: detected DB with user_version < 11 without registered migration path. " +
                                "All tables dropped and recreated from current schema (v13). User data lost. " +
                                "Likely cause — install from pre-0.1.0 internal build."
                    )
                    // После destructive recreate Room вызывает onCreate — seed
                    // отработает там. Дополнительный seed здесь не нужен.
                }
            })
            .build()
    }

    @Provides
    fun provideWordDao(db: Database): WordDao {
        return db.wordDao()
    }

    @Provides
    fun provideComponentTypeDao(db: Database): ComponentTypeDao {
        return db.componentTypeDao()
    }

    @Provides
    fun provideComponentValueDao(db: Database): ComponentValueDao {
        return db.componentValueDao()
    }

    @Provides
    fun provideComponentOptionDao(db: Database): ComponentOptionDao {
        return db.componentOptionDao()
    }

    @Provides
    fun provideQuizConfigDao(db: Database): QuizConfigDao {
        return db.quizConfigDao()
    }

    companion object {
        private const val DATABASE_NAME = "name"
    }

}
