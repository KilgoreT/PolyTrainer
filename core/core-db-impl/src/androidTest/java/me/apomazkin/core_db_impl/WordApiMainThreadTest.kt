package me.apomazkin.core_db_impl

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.apomazkin.core_db_impl.entity.DictionaryDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.room.Database
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * IS521: [CoreDbApiImpl.WordApiImpl] безопасен из главного потока.
 *
 * Карточка слова выполняет эффекты в главном потоке; блокирующий
 * DAO-метод там падает с `Cannot access database on the main thread`
 * (переименование слова → снекбар «Не удалось сохранить»). Методы
 * `…Suspend` обязаны быть настоящими suspend — Room сам уходит в свой пул.
 */
@RunWith(AndroidJUnit4::class)
class WordApiMainThreadTest {

    private lateinit var db: Database
    private lateinit var api: CoreDbApiImpl.WordApiImpl
    private var dictId: Long = 0

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder<Database>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        api = CoreDbApiImpl.WordApiImpl(db.wordDao())
        dictId = runBlocking { db.wordDao().addDictionary(DictionaryDb(name = "EN", addDate = Date(0))) }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun updateWord_fromMainThread_saves() {
        val wordId = runBlocking {
            db.wordDao().addWordSuspend(WordDb(dictionaryId = dictId, value = "old", addDate = Date(0)))
        }

        val result = onMainThread { api.updateWordSuspend(wordId, "new") }

        assertNull(result.exceptionOrNull())
        assertTrue(result.getOrThrow())
        assertEquals("new", runBlocking { db.wordDao().getWordSuspend(wordId).wordDb.value })
    }

    @Test
    fun addWord_fromMainThread_saves() {
        val result = onMainThread { api.addWordSuspend("fresh", dictId.toInt()) }

        assertNull(result.exceptionOrNull())
        val wordId = result.getOrThrow()
        assertEquals("fresh", runBlocking { db.wordDao().getWordSuspend(wordId).wordDb.value })
    }

    /** Выполняет [block] в главном потоке, как раннер эффектов карточки. */
    private fun <T> onMainThread(block: suspend () -> T): Result<T> {
        var result: Result<T>? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            result = runCatching { runBlocking { block() } }
        }
        return checkNotNull(result)
    }
}
