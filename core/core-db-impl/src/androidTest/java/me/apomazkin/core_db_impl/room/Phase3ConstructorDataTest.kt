package me.apomazkin.core_db_impl.room

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import me.apomazkin.core_db_api.entity.CreateComponentOutcome
import me.apomazkin.core_db_api.entity.EditComponentOutcome
import me.apomazkin.core_db_api.entity.OptionCrudOutcome
import me.apomazkin.core_db_api.entity.SetEnabledComponentOutcome
import me.apomazkin.core_db_api.entity.SoftDeleteComponentOutcome
import me.apomazkin.core_db_impl.CoreDbApiImpl
import me.apomazkin.core_db_impl.entity.ComponentTypeDb
import me.apomazkin.core_db_impl.entity.ComponentValueDb
import me.apomazkin.core_db_impl.entity.WordDb
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.Scope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * IS486 фаза 3, W3: data-методы конструктора (spec §6, §7.5, §7.8, §8, §9.4, К1–К5).
 *
 * Фикстура (как в [CascadeExecutorTest]): словарь D1 (builtin seed: translation ядро,
 * part_of_speech CHOICE не-ядро) + кастомы Base(ядро) ← Child ← Grandchild, лексема
 * со значениями всех трёх.
 *
 * Покрытие:
 * - setComponentEnabled: Success / LastEnabledCore / Removed; без каскада значений.
 * - editComponentType: перепривязка со сбросом поддерева / CycleDetected /
 *   LastEnabledCore / MultiForbiddenForChoice.
 * - options CRUD: add / rename / delete (комбинированный каскад) / Removed.
 * - softDeleteComponentType: LastEnabledCore.
 */
@RunWith(AndroidJUnit4::class)
class Phase3ConstructorDataTest {

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

    private fun lexemeApi() = CoreDbApiImpl.LexemeApiImpl(
        database = db,
        wordDao = db.wordDao(),
        componentTypeDao = db.componentTypeDao(),
        componentValueDao = db.componentValueDao(),
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

    private suspend fun translationId(): Long =
        db.componentTypeDao().getBySystemKeyForDictionary("translation", dictId)!!.id

    // ============================================================
    // setComponentEnabled (spec §6, §7.8)
    // ============================================================

    // === P3.1 — disable не-ядра: Success, значения живы (каскада нет) ===
    @Test
    fun setEnabled_disable_noCascade() = runBlocking {
        fixture()

        val outcome = lexemeApi().setComponentEnabled(childId, enabled = false)

        assertTrue(outcome is SetEnabledComponentOutcome.Success)
        val child = db.componentTypeDao().getById(childId)!!
        assertFalse(child.enabled)
        // Мягкий рубильник: значения не тронуты.
        assertEquals(1, db.componentValueDao().countActiveByTypeId(childId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(grandchildId))
    }

    // === P3.2 — включение обратно: Success, enabled = true ===
    @Test
    fun setEnabled_reEnable() = runBlocking {
        fixture()
        lexemeApi().setComponentEnabled(childId, enabled = false)

        val outcome = lexemeApi().setComponentEnabled(childId, enabled = true)

        assertTrue(outcome is SetEnabledComponentOutcome.Success)
        assertTrue(db.componentTypeDao().getById(childId)!!.enabled)
    }

    // === P3.3 — нельзя выключить последнее включённое ядро словаря ===
    @Test
    fun setEnabled_lastEnabledCore_refused() = runBlocking {
        fixture()
        // Ядра словаря: translation (builtin) + Base. Выключаем translation — не последний.
        val first = lexemeApi().setComponentEnabled(translationId(), enabled = false)
        assertTrue(first is SetEnabledComponentOutcome.Success)

        // Base — последнее включённое ядро → отказ.
        val outcome = lexemeApi().setComponentEnabled(baseId, enabled = false)

        assertTrue(outcome is SetEnabledComponentOutcome.LastEnabledCore)
        assertTrue("Base остался включён", db.componentTypeDao().getById(baseId)!!.enabled)
    }

    // === P3.4 — soft-deleted тип: Removed ===
    @Test
    fun setEnabled_removedType() = runBlocking {
        fixture()
        lexemeApi().softDeleteComponentType(childId)

        val outcome = lexemeApi().setComponentEnabled(childId, enabled = false)

        assertTrue(outcome is SetEnabledComponentOutcome.Removed)
    }

    // ============================================================
    // editComponentType — перепривязка (spec §8, §9.4, §7.5, §7.8)
    // ============================================================

    // === P3.5 — умный сброс (решение 2026-07-21): rebind на выполненное условие БЕЗ потерь ===
    @Test
    fun rebind_satisfiedTarget_keepsValues() = runBlocking {
        fixture()

        // Child: Base → лексема (ядро). Цель-лексема всегда выполнена → значения живут.
        val outcome = lexemeApi().editComponentType(
            typeId = childId,
            name = "Child",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            core = true,
            dependsOnTypeId = null,
            dependsOnOptionId = null,
        )

        assertTrue(outcome is EditComponentOutcome.Success)
        val child = db.componentTypeDao().getById(childId)!!
        assertNull(child.dependsOnTypeId)
        assertTrue(child.core)
        // Умный сброс: новое условие выполнено — НИЧЕГО не сброшено.
        assertEquals(1, db.componentValueDao().countActiveByTypeId(childId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(grandchildId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(baseId))
    }

    // === P3.5b — умный сброс: rebind на НЕвыполненное условие гасит только те лексемы ===
    @Test
    fun rebind_unsatisfiedOption_resetsSubtree() = runBlocking {
        fixture()
        val posType = db.componentTypeDao().getBySystemKeyForDictionary("part_of_speech", dictId)!!
        val noun = db.componentOptionDao().getForType(posType.id).first { it.systemKey == "noun" }

        // Child: Base → опция noun; у лексемы noun НЕ выбран → условие не выполнено.
        val outcome = lexemeApi().editComponentType(
            typeId = childId,
            name = "Child",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            core = false,
            dependsOnTypeId = null,
            dependsOnOptionId = noun.id,
        )

        assertTrue(outcome is EditComponentOutcome.Success)
        // Child и его поддерево (Grandchild) сброшены; Base не тронут.
        assertEquals(0, db.componentValueDao().countActiveByTypeId(childId))
        assertEquals(0, db.componentValueDao().countActiveByTypeId(grandchildId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(baseId))
    }

    // === P3.5c — previewRebindImpact: dry-run умного сброса ===
    @Test
    fun previewRebindImpact_safeAndUnsafe() = runBlocking {
        fixture()
        val posType = db.componentTypeDao().getBySystemKeyForDictionary("part_of_speech", dictId)!!
        val noun = db.componentOptionDao().getForType(posType.id).first { it.systemKey == "noun" }

        // Безопасно: Child → лексема.
        val safe = lexemeApi().previewRebindImpact(childId, core = true, dependsOnTypeId = null, dependsOnOptionId = null)!!
        assertEquals(0, safe.valueCount)
        assertEquals(0, safe.descendantValueCount)

        // Небезопасно: Child → опция noun (не выбрана) — своё значение + потомок Grandchild.
        val unsafe = lexemeApi().previewRebindImpact(childId, core = false, dependsOnTypeId = null, dependsOnOptionId = noun.id)!!
        assertEquals(1, unsafe.valueCount)
        assertEquals(1, unsafe.descendantValueCount)
        // Preview — read-only: ничего не сброшено.
        assertEquals(1, db.componentValueDao().countActiveByTypeId(childId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(grandchildId))
    }

    // === P3.6 — перепривязка, создающая цикл: CycleDetected, ничего не изменено ===
    @Test
    fun rebind_cycleDetected() = runBlocking {
        fixture()

        // Base → Grandchild при цепочке Base ← Child ← Grandchild = цикл.
        val outcome = lexemeApi().editComponentType(
            typeId = baseId,
            name = "Base",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            core = false,
            dependsOnTypeId = grandchildId,
            dependsOnOptionId = null,
        )

        assertTrue(outcome is EditComponentOutcome.CycleDetected)
        val base = db.componentTypeDao().getById(baseId)!!
        assertNull("ссылка не записана", base.dependsOnTypeId)
        assertTrue("ядро не снято", base.core)
        assertEquals("значения не тронуты", 1, db.componentValueDao().countActiveByTypeId(baseId))
    }

    // === P3.7 — перепривязка последнего включённого ядра на не-лексему: отказ ===
    @Test
    fun rebind_lastEnabledCore_refused() = runBlocking {
        fixture()
        lexemeApi().setComponentEnabled(translationId(), enabled = false)
        val posTypeId = db.componentTypeDao().getBySystemKeyForDictionary("part_of_speech", dictId)!!.id

        // Base — последнее включённое ядро; увод на компонент = потеря ядра.
        val outcome = lexemeApi().editComponentType(
            typeId = baseId,
            name = "Base",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            core = false,
            dependsOnTypeId = posTypeId,
            dependsOnOptionId = null,
        )

        assertTrue(outcome is EditComponentOutcome.LastEnabledCore)
        val base = db.componentTypeDao().getById(baseId)!!
        assertNull(base.dependsOnTypeId)
        assertTrue(base.core)
    }

    // === P3.8 — CHOICE + isMultiple: MultiForbiddenForChoice ===
    @Test
    fun editChoice_multiForbidden() = runBlocking {
        fixture()
        val created = lexemeApi().createUserDefinedComponent(
            name = "Kind",
            template = ComponentTemplate.CHOICE,
            isMultiple = false,
            scope = Scope.PerDictionaries(listOf(dictId)),
            core = false,
            optionLabels = listOf("a", "b"),
        )
        val kindId = (created as CreateComponentOutcome.Success).types.single().id

        val outcome = lexemeApi().editComponentType(
            typeId = kindId,
            name = "Kind",
            template = ComponentTemplate.CHOICE,
            isMultiple = true,
        )

        assertTrue(outcome is EditComponentOutcome.MultiForbiddenForChoice)
        assertFalse(db.componentTypeDao().getById(kindId)!!.isMultiple)
    }

    // === P3.14 — перепривязка degraded свободна (spec §6: терять нечего, план пуст) ===
    @Test
    fun rebind_degraded_isFree() = runBlocking {
        fixture()
        // Base умирает → Child degraded, значения Child/Grandchild сброшены каскадом.
        lexemeApi().softDeleteComponentType(baseId)
        assertEquals(0, db.componentValueDao().countActiveByTypeId(childId))

        // Перепривязка degraded Child на лексему-ядро — без ошибок и без новых каскадов.
        val outcome = lexemeApi().editComponentType(
            typeId = childId,
            name = "Child",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            core = true,
            dependsOnTypeId = null,
            dependsOnOptionId = null,
        )

        assertTrue(outcome is EditComponentOutcome.Success)
        val child = db.componentTypeDao().getById(childId)!!
        assertNull(child.dependsOnTypeId)
        assertTrue(child.core)
        // Мёртвые значения не оживают, живые чужие не тронуты (их и не было).
        assertEquals(0, db.componentValueDao().countActiveByTypeId(childId))
        assertEquals(0, db.componentValueDao().countActiveByTypeId(grandchildId))
    }

    // ============================================================
    // Options CRUD (spec К1–К5) — на кастомном CHOICE (builtin защищён, §21.2)
    // ============================================================

    /** Кастомный CHOICE «Род» с опциями муж/жен, цель — лексема (не ядро). */
    private suspend fun createCustomChoice(): Pair<Long, List<Long>> {
        val created = lexemeApi().createUserDefinedComponent(
            name = "Род",
            template = ComponentTemplate.CHOICE,
            isMultiple = false,
            scope = Scope.PerDictionaries(listOf(dictId)),
            core = false,
            optionLabels = listOf("муж", "жен"),
        ) as CreateComponentOutcome.Success
        val typeId = created.types.single().id
        val optionIds = db.componentOptionDao().getForType(typeId).map { it.id }
        return typeId to optionIds
    }

    // === P3.9 — add: опция в конец списка ===
    @Test
    fun optionAdd_appendsWithPosition() = runBlocking {
        fixture()
        val (choiceId, _) = createCustomChoice()
        val before = db.componentOptionDao().getForType(choiceId)

        val outcome = lexemeApi().addComponentOption(choiceId, "средний")

        assertTrue(outcome is OptionCrudOutcome.Success)
        val added = (outcome as OptionCrudOutcome.Success).option
        assertEquals("средний", added.label)
        assertNull(added.systemKey)
        assertTrue("позиция после существующих", added.position > before.maxOf { it.position })
    }

    // === P3.10 — rename: label обновлён, id устойчив ===
    @Test
    fun optionRename_updatesLabel() = runBlocking {
        fixture()
        val (_, optionIds) = createCustomChoice()

        val outcome = lexemeApi().renameComponentOption(optionIds.first(), "мужской")

        assertTrue(outcome is OptionCrudOutcome.Success)
        assertEquals("мужской", db.componentOptionDao().getById(optionIds.first())!!.label)
    }

    // === P3.11 — delete: комбинированный каскад (выборы + поддеревья зависимых) ===
    @Test
    fun optionDelete_combinedCascade() = runBlocking {
        fixture()
        val now = Date(0L)
        val (choiceId, optionIds) = createCustomChoice()
        val muzh = optionIds.first()
        // Suffix зависит от опции «муж»; выбор «муж» у лексемы + значение Suffix.
        val suffixId = db.componentTypeDao().insert(
            typeDb("Suffix", dependsOnTypeId = null, now = now).copy(core = false, dependsOnOptionId = muzh)
        )
        val choiceValueId = db.componentValueDao().insert(
            ComponentValueDb(
                lexemeId = lexemeId, componentTypeId = choiceId,
                value = """{"fields":{}}""", optionId = muzh,
                createdAt = now, updatedAt = now,
            )
        )
        db.componentValueDao().insert(
            ComponentValueDb(
                lexemeId = lexemeId, componentTypeId = suffixId,
                value = """{"fields":{"value":{"type":"text","value":"o"}}}""",
                createdAt = now, updatedAt = now,
            )
        )

        val outcome = lexemeApi().deleteComponentOption(muzh)

        assertTrue(outcome is OptionCrudOutcome.Deleted)
        // Опция soft-deleted.
        assertNotNull(db.componentOptionDao().getById(muzh)!!.removedAt)
        // Выбор-значение сброшен (getById фильтрует removed → null); поддерево Suffix сброшено.
        assertNull(db.componentValueDao().getById(choiceValueId))
        assertEquals(0, db.componentValueDao().countActiveByTypeId(choiceId))
        assertEquals(0, db.componentValueDao().countActiveByTypeId(suffixId))
        // Suffix-тип жив (degraded вычисляемо), ссылка на опцию сохранена.
        val suffix = db.componentTypeDao().getById(suffixId)!!
        assertNull(suffix.removedAt)
        assertEquals(muzh, suffix.dependsOnOptionId)
        // Опорная ветка Base/Child не тронута.
        assertEquals(1, db.componentValueDao().countActiveByTypeId(baseId))
        assertEquals(1, db.componentValueDao().countActiveByTypeId(childId))
    }

    // === P3.12 — повторный delete опции: Removed ===
    @Test
    fun optionDelete_repeat_removed() = runBlocking {
        fixture()
        val (_, optionIds) = createCustomChoice()
        lexemeApi().deleteComponentOption(optionIds.first())

        val outcome = lexemeApi().deleteComponentOption(optionIds.first())

        assertTrue(outcome is OptionCrudOutcome.Removed)
    }

    // === P3.16 — решение §21.2: опции builtin нередактируемы (все три операции) ===
    @Test
    fun builtinOptions_areProtected() = runBlocking {
        fixture()
        val posTypeId = db.componentTypeDao().getBySystemKeyForDictionary("part_of_speech", dictId)!!.id
        val noun = db.componentOptionDao().getForType(posTypeId).first { it.systemKey == "noun" }

        assertTrue(lexemeApi().addComponentOption(posTypeId, "герундий") is OptionCrudOutcome.BuiltInProtected)
        assertTrue(lexemeApi().renameComponentOption(noun.id, "сущ") is OptionCrudOutcome.BuiltInProtected)
        assertTrue(lexemeApi().deleteComponentOption(noun.id) is OptionCrudOutcome.BuiltInProtected)

        // Ничего не изменилось: опция жива, label не переписан, состав прежний.
        val after = db.componentOptionDao().getById(noun.id)!!
        assertNull(after.removedAt)
        assertNull(after.label)
        assertEquals(6, db.componentOptionDao().getForType(posTypeId).size)
    }

    // ============================================================
    // softDeleteComponentType — защита ядра (spec §7.8)
    // ============================================================

    // === P3.13 — нельзя удалить последнее включённое ядро ===
    @Test
    fun softDelete_lastEnabledCore_refused() = runBlocking {
        fixture()
        lexemeApi().setComponentEnabled(translationId(), enabled = false)

        // Base — последнее включённое ядро словаря.
        val outcome = lexemeApi().softDeleteComponentType(baseId)

        assertTrue(outcome is SoftDeleteComponentOutcome.LastEnabledCore)
        assertNull("тип жив", db.componentTypeDao().getById(baseId)!!.removedAt)
        assertEquals("значения живы", 1, db.componentValueDao().countActiveByTypeId(baseId))
    }
}
