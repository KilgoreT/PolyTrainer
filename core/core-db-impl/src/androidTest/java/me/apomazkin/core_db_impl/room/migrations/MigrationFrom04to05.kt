package me.apomazkin.core_db_impl.room.migrations

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.apomazkin.core_db_impl.room.Schema
import me.apomazkin.core_db_impl.room.base.BaseMigration
import me.apomazkin.core_db_impl.room.dataSource.DataProvider
import me.apomazkin.core_db_impl.room.utils.*
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationFrom04to05 : BaseMigration() {

    override fun getMigrationClass() = migration_4_5
    override fun getCurrentVersion() = CURRENT_VERSION

    @Test
    fun from04to05() {
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
            },
            onCreateCheck = { database ->
                database
                    .getWordsFromDatabase()
                    .checkCount(DataProvider.wordList)
                database
                    .getWriteQuizFromDatabase()
                    .checkCount(DataProvider.writeQuizList)
            },
            onMigrationCheck = { database ->
                database
                    .getLanguagesFromDatabase()
                    .forEach { item ->
                        Log.d(
                            "###",
                            "MigrationFrom04to05 / 49 / from04to05: ${item.code} ${item.addDate}"
                        )
                    }
                database
                    .getLanguagesFromDatabase()
                    .checkCount(DataProvider.languageList)
                database
                    .getWordsFromDatabase()
                    .forEach { item ->
                        Assert.assertTrue(
                            "WordDb.langId must be 0, but it is ${item.langId}",
                            DataProvider.languageList.first().id?.let { item.langId == it } ?: false
                        )
                    }
                database
                    .getWriteQuizFromDatabase()
                    .forEach { item ->
                        Assert.assertTrue(
                            "WriteQuizDb.langId must be 0, but it is ${item.langId}",
                            DataProvider.languageList.first().id?.let { item.langId == it } ?: false
                        )
                    }
            }
        )
    }

    companion object {
        private const val CURRENT_VERSION = 4
    }
}