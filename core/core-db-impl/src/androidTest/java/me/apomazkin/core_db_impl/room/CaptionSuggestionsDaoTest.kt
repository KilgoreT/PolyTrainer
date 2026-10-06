package me.apomazkin.core_db_impl.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.apomazkin.core_db_impl.CoreDbApiImpl
import me.apomazkin.core_db_impl.entity.ComponentTypeDb
import me.apomazkin.core_db_impl.entity.ComponentValueDb
import me.apomazkin.core_db_impl.entity.LexemeDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.mapper.toJson
import me.apomazkin.lexeme.CaptionedTextValues
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.Primitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * IS491: `ComponentValueDao.captionSuggestions` (UC3, UC10–UC12; Д4).
 *
 * Контракт: distinct живые caption-значения строго заданного typeId;
 * регистр значим (дедупа НЕТ); сортировка по частоте DESC, tie-break — алфавит;
 * soft-deleted и пустые caption исключены.
 */
@RunWith(AndroidJUnit4::class)
class CaptionSuggestionsDaoTest {

    private lateinit var db: Database
    private lateinit var logger: RecordingLogger

    private var dictId: Long = 0
    private var lexemeId: Long = 0

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder<Database>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        logger = RecordingLogger()
        runBlocking {
            val dictionaryApi = CoreDbApiImpl.DictionaryApiImpl(
                database = db,
                wordDao = db.wordDao(),
                componentTypeDao = db.componentTypeDao(),
                componentOptionDao = db.componentOptionDao(),
                logger = logger,
            )
            dictId = dictionaryApi.addDictionary("ES", null, "es", "ru")
            val now = Date(0L)
            val wordId = db.wordDao().addWordSuspend(
                WordDb(dictionaryId = dictId, value = "gato", addDate = now),
            )
            lexemeId = db.wordDao().addLexeme(LexemeDb(wordId = wordId, addDate = now))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun newCaptionedType(name: String): Long = runBlocking {
        db.componentTypeDao().insert(
            ComponentTypeDb(
                systemKey = null,
                dictionaryId = dictId,
                name = name,
                templateKey = ComponentTemplate.CAPTIONED_TEXT.key,
                position = 10,
                isMultiple = true,
                core = false,
                enabled = true,
                createdAt = Date(0L),
                updatedAt = Date(0L),
            )
        )
    }

    private fun insertValue(typeId: Long, caption: String?, removed: Boolean = false) = runBlocking {
        val json = CaptionedTextValues(
            text = Primitive.Text("quote"),
            caption = caption?.let { Primitive.Text(it) },
        ).toJson()
        db.componentValueDao().insert(
            ComponentValueDb(
                lexemeId = lexemeId,
                componentTypeId = typeId,
                value = json,
                createdAt = Date(0L),
                updatedAt = Date(0L),
                removedAt = if (removed) Date(1L) else null,
            )
        )
    }

    private fun insertRawValue(typeId: Long, rawJson: String) = runBlocking {
        db.componentValueDao().insert(
            ComponentValueDb(
                lexemeId = lexemeId,
                componentTypeId = typeId,
                value = rawJson,
                createdAt = Date(0L),
                updatedAt = Date(0L),
            )
        )
    }

    @Test
    fun frequencyOrder_thenAlphabetTieBreak() = runBlocking {
        val typeId = newCaptionedType("Топик")
        repeat(3) { insertValue(typeId, "Grammar") }
        repeat(2) { insertValue(typeId, "IELTS") }
        insertValue(typeId, "Idioms")
        // Idioms и ... — частота 1; tie-break — алфавит (IELTS(2) выше обоих).
        insertValue(typeId, "Aspect")

        assertEquals(
            listOf("Grammar", "IELTS", "Aspect", "Idioms"),
            db.componentValueDao().captionSuggestions(typeId),
        )
    }

    @Test
    fun caseSensitive_noDedup_uc11() = runBlocking {
        val typeId = newCaptionedType("Стиль")
        insertValue(typeId, "ielts")
        repeat(3) { insertValue(typeId, "IELTS") }
        insertValue(typeId, "разговорный")
        repeat(2) { insertValue(typeId, "Разговорный") }

        // 4 пункта: регистр значим; частота DESC, tie (1) — алфавит (латиница < кириллицы).
        assertEquals(
            listOf("IELTS", "Разговорный", "ielts", "разговорный"),
            db.componentValueDao().captionSuggestions(typeId),
        )
    }

    @Test
    fun scopeIsolation_strictlyByTypeId_uc10() = runBlocking {
        val quoteType = newCaptionedType("Цитата")
        val ruleType = newCaptionedType("Правило")
        insertValue(quoteType, "IELTS")
        insertValue(ruleType, "B2")

        assertEquals(listOf("IELTS"), db.componentValueDao().captionSuggestions(quoteType))
        assertEquals(listOf("B2"), db.componentValueDao().captionSuggestions(ruleType))
    }

    @Test
    fun softDeleted_excluded_uc8() = runBlocking {
        val typeId = newCaptionedType("Цитата")
        insertValue(typeId, "Alive")
        insertValue(typeId, "Solo", removed = true)

        assertEquals(listOf("Alive"), db.componentValueDao().captionSuggestions(typeId))
    }

    @Test
    fun nullAndEmptyCaptions_excluded_uc5() = runBlocking {
        val typeId = newCaptionedType("Цитата")
        insertValue(typeId, caption = null)
        // Пустая строка в envelope (руками — сериализатор пустую не пишет).
        insertRawValue(
            typeId,
            """{"fields":{"text":{"type":"text","value":"q"},"caption":{"type":"text","value":""}}}""",
        )
        insertValue(typeId, "Real")

        assertEquals(listOf("Real"), db.componentValueDao().captionSuggestions(typeId))
    }

    @Test
    fun emptyType_returnsEmptyList_uc2() = runBlocking {
        val typeId = newCaptionedType("Цитата")
        assertEquals(emptyList<String>(), db.componentValueDao().captionSuggestions(typeId))
    }
}
