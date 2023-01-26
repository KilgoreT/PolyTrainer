package me.apomazkin.core_db_impl.room.migrations

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.apomazkin.core_db_impl.room.Schema
import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.dataSource.DataProvider
import me.apomazkin.core_db_impl.room.utils.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom05to06 : BaseMigration() {

    override fun getMigrationClass() = migration_5_6
    override fun getCurrentVersion() = CURRENT_VERSION

    @Test
    fun from05to06() {
        runMigrateDbTest(
            onCreate = { database ->
                DataProvider
                    .wordList
                    .asContentValue()
                    .toDatabase(
                        database = database,
                        table = Schema.Word.tableName
                    )
                DataProvider
                    .writeQuizList
                    .asContentValue()
                    .toDatabase(
                        database = database,
                        table = Schema.WriteQuiz.tableName
                    )
                DataProvider
                    .languageList
                    .asContentValue()
                    .toDatabase(
                        database = database,
                        table = Schema.Languages.tableName
                    )
            },
            onCreateCheck = { database ->
                database
                    .getWordsFromDatabase()
                    .checkCount(DataProvider.wordList)
                database
                    .getWriteQuizFromDatabase()
                    .checkCount(DataProvider.writeQuizList)
                database
                    .getLanguagesFromDatabase()
                    .checkCount(DataProvider.languageList)
            },
            onMigrationCheck = { database ->
                database
                    .getLanguagesFromDatabase()
                    .forEach { item ->
                        Log.d(
                            "###",
                            "MigrationFrom05to06: ${item.numericCode} ${item.code} ${item.addDate}"
                        )
                    }
                database
                    .getLanguagesFromDatabase()
                    .checkCount(DataProvider.languageList)
            }
        )
    }

    companion object {
        private const val CURRENT_VERSION = 5
    }
}