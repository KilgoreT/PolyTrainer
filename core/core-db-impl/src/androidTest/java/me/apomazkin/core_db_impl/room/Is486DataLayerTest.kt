package me.apomazkin.core_db_impl.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.apomazkin.core_db_impl.CoreDbApiImpl
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.PartOfSpeechOption
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.Scope
import me.apomazkin.lexeme.TextValues
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * IS486 фаза 1, зона C: data-слой поверх новой схемы (phase1_plan.md § Зона C).
 *
 * Harness (C0): in-memory Room + bundled driver (зеркало RoomModule) + ручная
 * сборка Api-impl (конструкторы принимают DAO/Database/logger — Dagger не нужен).
 *
 * Fresh-install путь: Room создаёт v12-схему из @Entity; builtin сеются
 * пословарно в транзакции addDictionary (spec §9.5).
 */
@RunWith(AndroidJUnit4::class)
class Is486DataLayerTest {

    private lateinit var db: Database
    private lateinit var dictionaryApi: CoreDbApiImpl.DictionaryApiImpl
    private lateinit var lexemeApi: CoreDbApiImpl.LexemeApiImpl
    private lateinit var logger: RecordingLogger

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder<Database>(context)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        logger = RecordingLogger()
        dictionaryApi = CoreDbApiImpl.DictionaryApiImpl(
            database = db,
            wordDao = db.wordDao(),
            componentTypeDao = db.componentTypeDao(),
            componentOptionDao = db.componentOptionDao(),
        )
        lexemeApi = CoreDbApiImpl.LexemeApiImpl(
            database = db,
            wordDao = db.wordDao(),
            componentTypeDao = db.componentTypeDao(),
            componentValueDao = db.componentValueDao(),
            componentOptionDao = db.componentOptionDao(),
            quizConfigDao = db.quizConfigDao(),
            logger = logger,
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun newWord(dictId: Long, value: String): Long =
        db.wordDao().addWordSuspend(WordDb(dictionaryId = dictId, value = value, addDate = Date(0L)))

    // === C5 — seed builtin в транзакции addDictionary ===
    @Test
    fun c5_addDictionarySeedsPerDictBuiltIns() = runBlocking {
        val d1 = dictionaryApi.addDictionary("ES", null)
        val d2 = dictionaryApi.addDictionary("EN", null)

        listOf(d1, d2).forEach { dictId ->
            val translation = db.componentTypeDao().getBySystemKeyForDictionary("translation", dictId)
            assertNotNull("translation must be seeded for dict $dictId", translation)
            assertTrue("translation is core", translation!!.core)
            assertTrue("translation enabled", translation.enabled)
            assertNull(translation.dependsOnTypeId)
            assertNull(translation.dependsOnOptionId)

            val pos = db.componentTypeDao().getBySystemKeyForDictionary("part_of_speech", dictId)
            assertNotNull("part_of_speech must be seeded for dict $dictId", pos)
            assertEquals(ComponentTemplate.CHOICE.key, pos!!.templateKey)
            assertEquals(false, pos.core)

            val options = db.componentOptionDao().getForType(pos.id)
            assertEquals(6, options.size)
            assertEquals(
                PartOfSpeechOption.entries.map { it.key },
                options.map { it.systemKey },
            )
            assertTrue("builtin options have no label", options.all { it.label == null })
        }
    }

    // === C1 — пословарный lookup builtin ===
    @Test
    fun c1_getBySystemKeyScopedToDictionary() = runBlocking {
        val d1 = dictionaryApi.addDictionary("ES", null)
        val d2 = dictionaryApi.addDictionary("EN", null)

        val t1 = db.componentTypeDao().getBySystemKeyForDictionary("translation", d1)
        val t2 = db.componentTypeDao().getBySystemKeyForDictionary("translation", d2)
        assertNotNull(t1)
        assertNotNull(t2)
        assertTrue("different rows per dictionary", t1!!.id != t2!!.id)
        assertEquals(d1, t1.dictionaryId)
        assertEquals(d2, t2.dictionaryId)
    }

    // === C2 — создание лексемы с переводом цепляется к типу своего словаря ===
    @Test
    fun c2_addLexemeWithBuiltInBindsToOwnDictionary() = runBlocking {
        val d1 = dictionaryApi.addDictionary("ES", null)
        val d2 = dictionaryApi.addDictionary("EN", null)
        val w1 = newWord(d1, "gato")
        val w2 = newWord(d2, "cat")

        val l1 = lexemeApi.addLexemeWithBuiltInComponent(
            wordId = w1, dictionaryId = d1,
            systemKey = BuiltInComponent.TRANSLATION,
            data = TextValues(Primitive.Text("кот")),
        )
        val l2 = lexemeApi.addLexemeWithBuiltInComponent(
            wordId = w2, dictionaryId = d2,
            systemKey = BuiltInComponent.TRANSLATION,
            data = TextValues(Primitive.Text("кошка")),
        )

        val t1 = db.componentTypeDao().getBySystemKeyForDictionary("translation", d1)!!
        val t2 = db.componentTypeDao().getBySystemKeyForDictionary("translation", d2)!!
        val lex1 = lexemeApi.getLexemeById(l1)!!
        val lex2 = lexemeApi.getLexemeById(l2)!!
        assertEquals(t1.id, lex1.components.single().type.id)
        assertEquals(t2.id, lex2.components.single().type.id)
    }

    // === C3 — restore-эквивалент: addLexemeWithComponents с BuiltIn-ref ===
    @Test
    fun c3_addLexemeWithComponentsResolvesBuiltInPerDictionary() = runBlocking {
        val d1 = dictionaryApi.addDictionary("ES", null)
        val d2 = dictionaryApi.addDictionary("EN", null)
        val w1 = newWord(d1, "gato")
        val w2 = newWord(d2, "cat")

        val l1 = lexemeApi.addLexemeWithComponents(
            wordId = w1, dictionaryId = d1,
            components = listOf(
                ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION) to TextValues(Primitive.Text("кот")),
            ),
        )
        val l2 = lexemeApi.addLexemeWithComponents(
            wordId = w2, dictionaryId = d2,
            components = listOf(
                ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION) to TextValues(Primitive.Text("кошка")),
            ),
        )

        assertNotNull(l1)
        assertNotNull(l2)
        val t1 = db.componentTypeDao().getBySystemKeyForDictionary("translation", d1)!!
        val t2 = db.componentTypeDao().getBySystemKeyForDictionary("translation", d2)!!
        assertEquals(t1.id, lexemeApi.getLexemeById(l1!!)!!.components.single().type.id)
        assertEquals(t2.id, lexemeApi.getLexemeById(l2!!)!!.components.single().type.id)
    }

    // === C6 — legacy-create дефолты: цель-лексема + core=true ===
    @Test
    fun c6_legacyCreateDefaultsToLexemeCore() = runBlocking {
        val d1 = dictionaryApi.addDictionary("ES", null)

        lexemeApi.createUserDefinedComponent(
            name = "Definition",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            scope = Scope.PerDictionaries(listOf(d1)),
        )

        val created = db.componentTypeDao().findActiveUserDefinedByName(d1, "Definition")
        assertNotNull(created)
        assertTrue("legacy-created custom is core", created!!.core)
        assertTrue(created.enabled)
        assertNull(created.dependsOnTypeId)
        assertNull(created.dependsOnOptionId)
    }

    // === C7 — удаление словаря: CASCADE уносит типы и опции, второй словарь цел ===
    @Test
    fun c7_dictionaryDeleteCascadesTypesAndOptions() = runBlocking {
        val d1 = dictionaryApi.addDictionary("ES", null)
        val d2 = dictionaryApi.addDictionary("EN", null)

        dictionaryApi.deleteDictionary(d1)

        assertNull(db.componentTypeDao().getBySystemKeyForDictionary("translation", d1))
        assertNull(db.componentTypeDao().getBySystemKeyForDictionary("part_of_speech", d1))
        val posD2 = db.componentTypeDao().getBySystemKeyForDictionary("part_of_speech", d2)
        assertNotNull(posD2)
        assertEquals(6, db.componentOptionDao().getForType(posD2!!.id).size)
    }

    // === C5b — идемпотентность seed: повторов не бывает даже при гипотетическом дубле вызова ===
    @Test
    fun c5b_seedIdempotentPerDictionary() = runBlocking {
        val d1 = dictionaryApi.addDictionary("ES", null)
        // Повторный addDictionary создаёт ДРУГОЙ словарь — а у d1 builtin не дублируются.
        dictionaryApi.addDictionary("ES", null)

        val pos = db.componentTypeDao().getBySystemKeyForDictionary("part_of_speech", d1)!!
        assertEquals(6, db.componentOptionDao().getForType(pos.id).size)
        val allForD1 = db.componentTypeDao().getTypesForDictionary(d1)
        assertEquals(
            "ровно две builtin-строки у словаря",
            2,
            allForD1.count { it.systemKey != null },
        )
    }
}

internal class RecordingLogger : LexemeLogger {
    val errors: MutableList<String> = mutableListOf()
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (level == LogLevel.ERROR) errors += message
    }
}
