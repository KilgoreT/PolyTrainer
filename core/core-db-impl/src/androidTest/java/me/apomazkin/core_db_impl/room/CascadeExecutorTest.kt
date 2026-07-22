package me.apomazkin.core_db_impl.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.apomazkin.core_db_api.entity.SoftDeleteComponentOutcome
import me.apomazkin.core_db_impl.CoreDbApiImpl
import me.apomazkin.core_db_impl.entity.ComponentTypeDb
import me.apomazkin.core_db_impl.entity.ComponentValueDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.core_db_impl.room.dao.ComponentValueDao
import me.apomazkin.lexeme.ComponentTemplate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * IS486 фаза 1, зона E2: каскад-исполнитель на softDelete-пути (phase1_plan.md § Зона E).
 *
 * Фикстура: словарь D1 (builtin seed через addDictionary) + кастомы:
 * Base (ядро-legacy) ← Child (зависит от Base) ← Grandchild (зависит от Child).
 * Лексема со значениями всех трёх.
 *
 * Транзакционность — DAO-декоратор с инжектированным сбоем (продуктовый код чист).
 */
@RunWith(AndroidJUnit4::class)
class CascadeExecutorTest {

    private lateinit var db: Database
    private lateinit var dictionaryApi: CoreDbApiImpl.DictionaryApiImpl
    private lateinit var logger: RecordingLogger

    private var dictId: Long = 0
    private var baseId: Long = 0
    private var childId: Long = 0
    private var grandchildId: Long = 0
    private var lexemeId: Long = 0

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
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun lexemeApi(valueDao: ComponentValueDao = db.componentValueDao()) =
        CoreDbApiImpl.LexemeApiImpl(
            database = db,
            wordDao = db.wordDao(),
            componentTypeDao = db.componentTypeDao(),
            componentValueDao = valueDao,
            componentOptionDao = db.componentOptionDao(),
            quizConfigDao = db.quizConfigDao(),
            logger = logger,
        )

    private fun fixture() = runBlocking {
        val now = Date(0L)
        dictId = dictionaryApi.addDictionary("ES", null)
        baseId = db.componentTypeDao().insert(typeDb("Base", dependsOnTypeId = null, now = now))
        childId = db.componentTypeDao().insert(typeDb("Child", dependsOnTypeId = baseId, now = now))
        grandchildId = db.componentTypeDao().insert(typeDb("Grandchild", dependsOnTypeId = childId, now = now))
        val wordId = db.wordDao().addWordSuspend(WordDb(dictionaryId = dictId, value = "gato", addDate = now))
        lexemeId = lexemeApi().addLexeme(wordId)
        listOf(baseId, childId, grandchildId).forEach { typeId ->
            db.componentValueDao().insert(
                ComponentValueDb(
                    lexemeId = lexemeId,
                    componentTypeId = typeId,
                    value = """{"fields":{"value":{"type":"text","value":"x"}}}""",
                    createdAt = now,
                    updatedAt = now,
                )
            )
        }
    }

    private fun typeDb(name: String, dependsOnTypeId: Long?, now: Date) = ComponentTypeDb(
        systemKey = null,
        dictionaryId = dictId,
        name = name,
        templateKey = ComponentTemplate.TEXT.key,
        position = 5,
        isMultiple = false,
        core = dependsOnTypeId == null,
        enabled = true,
        dependsOnTypeId = dependsOnTypeId,
        createdAt = now,
        updatedAt = now,
    )

    // === E2.1 — softDelete компонента: поддерево значений умирает, дети-компоненты живы ===
    @Test
    fun cascadeKillsSubtreeValues_childTypesSurviveAsDegraded() = runBlocking {
        fixture()

        val outcome = lexemeApi().softDeleteComponentType(baseId)
        assertTrue(outcome is SoftDeleteComponentOutcome.Success)

        // Значения: все три сброшены (Base + каскад Child/Grandchild).
        assertEquals(0, db.componentValueDao().countActiveByTypeId(baseId))
        assertEquals(0, db.componentValueDao().countActiveByTypeId(childId))
        assertEquals(0, db.componentValueDao().countActiveByTypeId(grandchildId))
        // Типы-дети НЕ удалены — живые строки, degraded вычисляется по ссылке.
        val child = db.componentTypeDao().getById(childId)
        assertNotNull(child)
        assertNull(child!!.removedAt)
        assertEquals(baseId, child.dependsOnTypeId)
        val grandchild = db.componentTypeDao().getById(grandchildId)
        assertNull(grandchild!!.removedAt)
    }

    // === E2.2 — транзакционность: сбой в середине → полный откат ===
    @Test
    fun failureMidCascade_rollsBackEverything() = runBlocking {
        fixture()
        val failingDao = object : ComponentValueDao by db.componentValueDao() {
            override suspend fun softDeleteByIds(ids: List<Long>, now: Date): Int {
                throw IllegalStateException("injected cascade failure")
            }
        }

        assertThrows(IllegalStateException::class.java) {
            runBlocking { lexemeApi(failingDao).softDeleteComponentType(baseId) }
        }

        // Полный откат: тип жив, все значения живы.
        assertNull(db.componentTypeDao().getById(baseId)!!.removedAt)
        assertEquals(1, db.componentValueDao().countActiveByTypeId(baseId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(childId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(grandchildId))
    }

    // === E2.3 — restore цели оживляет детей без записи (вычисляемый degraded) ===
    @Test
    fun restoreParent_revivesChildrenWithoutWrites(): Unit = runBlocking {
        fixture()
        lexemeApi().softDeleteComponentType(baseId)

        val childBefore = db.componentTypeDao().getById(childId)!!
        // Restore парента (data-уровень).
        val base = db.componentTypeDao().getById(baseId)!!
        db.componentTypeDao().update(base.copy(removedAt = null))

        // Ребёнок не менялся вообще (никаких записей) — и его ссылка снова указывает на живого.
        val childAfter = db.componentTypeDao().getById(childId)!!
        assertEquals(childBefore, childAfter)
        assertNull(db.componentTypeDao().getById(baseId)!!.removedAt)
    }

    // ============================================================
    // IS486 фаза 2, W1: value-триггеры каскада (spec §9.2).
    // ============================================================

    // === W1.1 — удаление значения опоры: потомки soft-deleted, лексема считает remaining без них ===
    @Test
    fun deleteValue_cascadesSubtree() = runBlocking {
        fixture()
        val api = lexemeApi()
        val baseValueId = db.componentValueDao().getActiveByTypeIds(listOf(baseId)).single().id

        val remaining = api.deleteComponentValue(baseValueId)

        // Каскад: Child/Grandchild сброшены soft; remaining = 0.
        assertEquals(0, remaining)
        assertEquals(0, db.componentValueDao().countActiveByTypeId(childId))
        assertEquals(0, db.componentValueDao().countActiveByTypeId(grandchildId))
    }

    // === W1.2 — мульти-цель: смерть не-последнего значения не каскадит ===
    @Test
    fun deleteValue_multiTargetNotLast_noCascade() = runBlocking {
        fixture()
        val now = Date(0L)
        // Второе значение Base (делаем его мульти для теста).
        db.componentTypeDao().update(db.componentTypeDao().getById(baseId)!!.copy(isMultiple = true))
        db.componentValueDao().insert(
            ComponentValueDb(
                lexemeId = lexemeId,
                componentTypeId = baseId,
                value = """{"fields":{"value":{"type":"text","value":"second"}}}""",
                createdAt = now,
                updatedAt = now,
            )
        )
        val firstValueId = db.componentValueDao().getActiveByTypeIds(listOf(baseId)).minBy { it.id }.id

        lexemeApi().deleteComponentValue(firstValueId)

        // Осталось второе значение Base → цель активна → Child/Grandchild живы.
        assertEquals(1, db.componentValueDao().countActiveByTypeId(baseId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(childId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(grandchildId))
    }

    // === W1.3 — CHOICE: смена опции сбрасывает только поддерево старой опции ===
    @Test
    fun updateChoiceValue_cascadesOldOptionSubtree() = runBlocking {
        fixture()
        val now = Date(0L)
        // Часть речи словаря + зависимый от опции noun компонент Gender со значением.
        val posType = db.componentTypeDao().getBySystemKeyForDictionary("part_of_speech", dictId)!!
        val options = db.componentOptionDao().getForType(posType.id)
        val noun = options.first { it.systemKey == "noun" }
        val verb = options.first { it.systemKey == "verb" }
        val genderId = db.componentTypeDao().insert(
            typeDb("Gender", dependsOnTypeId = null, now = now).copy(
                core = false,
                dependsOnOptionId = noun.id,
            )
        )
        // Выбор части речи = noun + значение Gender.
        val posValueId = db.componentValueDao().insert(
            ComponentValueDb(
                lexemeId = lexemeId, componentTypeId = posType.id,
                value = """{"fields":{}}""", optionId = noun.id,
                createdAt = now, updatedAt = now,
            )
        )
        db.componentValueDao().insert(
            ComponentValueDb(
                lexemeId = lexemeId, componentTypeId = genderId,
                value = """{"fields":{"value":{"type":"text","value":"m"}}}""",
                createdAt = now, updatedAt = now,
            )
        )

        // Смена noun → verb.
        val updated = lexemeApi().updateComponentValue(
            posValueId,
            me.apomazkin.lexeme.ChoiceValues(optionId = verb.id),
        )

        assertEquals(1, updated)
        // Значение части речи живо и указывает на verb.
        val posValue = db.componentValueDao().getById(posValueId)!!
        assertNull(posValue.removedAt)
        assertEquals(verb.id, posValue.optionId)
        // Gender (зависимый от noun) сброшен; опорные Base/Child/Grandchild не тронуты.
        assertEquals(0, db.componentValueDao().countActiveByTypeId(genderId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(baseId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(childId))
    }

    // === W1.4 — TEXT-смена значения не каскадит (план пуст) ===
    @Test
    fun updateTextValue_noCascade() = runBlocking {
        fixture()
        val baseValueId = db.componentValueDao().getActiveByTypeIds(listOf(baseId)).single().id

        lexemeApi().updateComponentValue(
            baseValueId,
            me.apomazkin.lexeme.TextValues(me.apomazkin.lexeme.Primitive.Text("новый текст")),
        )

        assertEquals(1, db.componentValueDao().countActiveByTypeId(baseId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(childId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(grandchildId))
    }

    // === E2.4 — существующий cleanup квиз-конфигов не сломан каскадом ===
    @Test
    fun quizConfigCleanupStillWorks() = runBlocking {
        fixture()
        // Default-конфиг уже создан addDictionary (F1) — дописываем в него Base-ref.
        val config = db.quizConfigDao().getAllConfigs().single()
        db.quizConfigDao().updateComponentRefs(
            config.id,
            """[{"type":"builtin","key":"translation"},{"type":"user","name":"Base"}]""",
        )

        lexemeApi().softDeleteComponentType(baseId)

        val refs = db.quizConfigDao().getAllConfigs().single().componentRefs
        assertTrue("Base ref must be cleaned", !refs.contains("Base"))
        assertTrue("translation ref must survive", refs.contains("translation"))
    }
}
