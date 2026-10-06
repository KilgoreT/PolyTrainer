package me.apomazkin.core_db_impl.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.apomazkin.core_db_impl.CoreDbApiImpl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * IS525: языки словаря через DictionaryApi.
 *
 * Кейсы:
 *  - A создание с языками → читаются обратно;
 *  - B обновление меняет оба языка;
 *  - C обновление названия без смены языков их не трогает (языки
 *    передаются явно те же).
 */
@RunWith(AndroidJUnit4::class)
class DictionaryLanguagesApiTest {

    private lateinit var db: Database
    private lateinit var dictionaryApi: CoreDbApiImpl.DictionaryApiImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder<Database>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        dictionaryApi = CoreDbApiImpl.DictionaryApiImpl(
            database = db,
            wordDao = db.wordDao(),
            componentTypeDao = db.componentTypeDao(),
            componentOptionDao = db.componentOptionDao(),
            logger = RecordingLogger(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun caseA_addWithLanguages_readBack() = runBlocking {
        val id = dictionaryApi.addDictionary("MX", 484, "es-MX", "ru")

        val entity = dictionaryApi.getDictionaryById(id)!!
        assertEquals("es-MX", entity.learningLanguage)
        assertEquals("ru", entity.translationLanguage)
    }

    @Test
    fun caseB_updateChangesBothLanguages() = runBlocking {
        val id = dictionaryApi.addDictionary("MX", 484, "es-MX", "ru")

        dictionaryApi.updateDictionary(id, "MX", 484, "es", "en")

        val entity = dictionaryApi.getDictionaryById(id)!!
        assertEquals("es", entity.learningLanguage)
        assertEquals("en", entity.translationLanguage)
    }

    @Test
    fun caseC_renameKeepsLanguagesPassedExplicitly() = runBlocking {
        val id = dictionaryApi.addDictionary("MX", 484, "es-MX", "ru")

        dictionaryApi.updateDictionary(id, "Mexico", 484, "es-MX", "ru")

        val entity = dictionaryApi.getDictionaryById(id)!!
        assertEquals("Mexico", entity.name)
        assertEquals("es-MX", entity.learningLanguage)
        assertEquals("ru", entity.translationLanguage)
    }
}
