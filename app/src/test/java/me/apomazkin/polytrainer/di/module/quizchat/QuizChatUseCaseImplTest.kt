package me.apomazkin.polytrainer.di.module.quizchat

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.ComponentOptionApiEntity
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.QuizConfigApiEntity
import me.apomazkin.core_db_api.entity.QuizGroupCountApiEntity
import me.apomazkin.core_db_api.entity.WordApiEntity
import me.apomazkin.core_db_api.entity.WriteQuizApiEntity
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.polytrainer.di.module.quizgroup.QuizGroupSelectionStore
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.QuizTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class QuizChatUseCaseImplTest {

    private val dictionaryApi = mockk<CoreDbApi.DictionaryApi>()
    private val quizApi = mockk<CoreDbApi.QuizApi>()
    private val lexemeApi = mockk<CoreDbApi.LexemeApi>()
    private val prefsProvider = mockk<PrefsProvider>()
    private val quizGroupSelectionStore = mockk<QuizGroupSelectionStore>()
    private val logger = mockk<LexemeLogger>(relaxed = true)

    private val useCase = QuizChatUseCaseImpl(
        dictionaryApi = dictionaryApi,
        quizApi = quizApi,
        lexemeApi = lexemeApi,
        prefsProvider = prefsProvider,
        quizGroupSelectionStore = quizGroupSelectionStore,
        logger = logger,
    )

    /** Включённые ядра раунда — для порции обязательны, содержание в этих тестах не важно. */
    private val cores = listOf(10L)

    private val translation = ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)
    private val definition = ComponentTypeRef.UserDefined("Definition")

    private fun stubPrefs(groupId: Long? = null) {
        coEvery { prefsProvider.getBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN) } returns false
        coEvery { prefsProvider.getBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN) } returns false
        coEvery {
            quizGroupSelectionStore.getValidatedSelection(any(), any())
        } returns groupId
    }

    /**
     * Квиз-запись с `lexemeId == id`; `wordId` отдельно — IS508 моделирует
     * «две лексемы одного слова» (пины Д1/Д4).
     */
    private fun makeQuizEntity(
        id: Long,
        grade: Int,
        dictId: Long = 1L,
        wordId: Long = id,
    ) = WriteQuizComplexEntity(
        quizData = WriteQuizApiEntity(
            id = id,
            dictionaryId = dictId,
            lexemeId = id,
            grade = grade,
            addDate = Date(),
        ),
        lexemeData = LexemeApiEntity(id = id, addDate = Date()),
        wordData = WordApiEntity(id = wordId, dictionaryId = dictId, value = "word_$wordId", addDate = Date()),
    )

    private fun stubBucket(grade: Int, ids: List<Long>) {
        coEvery {
            quizApi.getWriteQuizIds(grade = grade, dictionaryId = 1L, coreTypeIds = cores)
        } returns ids
    }

    private fun stubGetByIds(gradeOf: (Long) -> Int = { 0 }, wordOf: (Long) -> Long = { it }) {
        coEvery { quizApi.getWriteQuizByIds(any()) } answers {
            firstArg<List<Long>>().map { makeQuizEntity(it, grade = gradeOf(it), wordId = wordOf(it)) }
        }
    }

    private suspend fun portion(limit: Int, maxGrade: Int) = useCase.getRandomWriteQuizList(
        limit = limit,
        maxGrade = maxGrade,
        dictionaryId = 1L,
        coreTypeIds = cores,
    )

    private fun List<me.apomazkin.quiz.chat.entity.WriteQuiz>.lexemeIds(): List<Long> =
        map { it.lexeme.lexemeId.id }

    // ===== IS508 grade distribution pin (on pre-fix code) =====

    @Test
    fun `grade distribution - halves remaining per grade, fills from leftovers`() = runTest {
        stubPrefs()
        stubBucket(grade = 0, ids = (1L..10L).toList())
        stubBucket(grade = 1, ids = (11L..20L).toList())
        stubBucket(grade = 2, ids = (21L..30L).toList())
        stubGetByIds(gradeOf = { ((it - 1) / 10).toInt() })

        val result = portion(limit = 10, maxGrade = 2)

        assertEquals(10, result.size)
        // корзина 0 → limit/2 = 5, корзина 1 → remaining/2 = 2, корзина 2 → 1;
        // добор до 10 — из остатков любых грейдов (перемешан).
        assertTrue(result.count { it.grade == 0 } >= 5)
        assertTrue(result.count { it.grade == 1 } >= 2)
        assertTrue(result.count { it.grade == 2 } >= 1)
        assertEquals(result.size, result.lexemeIds().toSet().size)
    }

    @Test
    fun `grade distribution - single small bucket, no fill from nowhere`() = runTest {
        stubPrefs()
        stubBucket(grade = 0, ids = listOf(1L, 2L))
        stubBucket(grade = 1, ids = emptyList())
        stubBucket(grade = 2, ids = emptyList())
        stubGetByIds()

        val result = portion(limit = 10, maxGrade = 2)

        assertEquals(setOf(1L, 2L), result.lexemeIds().toSet())
    }

    @Test
    fun `normal - returns items within limit`() = runTest {
        stubPrefs()
        val ids = (1L..100L).toList()
        coEvery {
            quizApi.getWriteQuizIds(grade = any(), dictionaryId = 1L, coreTypeIds = cores)
        } returns ids
        coEvery { quizApi.getWriteQuizByIds(any()) } answers {
            val requestedIds = firstArg<List<Long>>()
            requestedIds.map { makeQuizEntity(it, grade = 0) }
        }

        val result = portion(limit = 10, maxGrade = 0)

        assertTrue("Result should not exceed limit", result.size <= 10)
        assertTrue("Result should not be empty", result.isNotEmpty())
        coVerify { quizApi.getWriteQuizIds(grade = 0, dictionaryId = 1L, coreTypeIds = cores) }
        coVerify { quizApi.getWriteQuizByIds(match { it.size <= 10 }) }
    }

    @Test
    fun `few items - returns all available`() = runTest {
        stubPrefs()
        val ids = listOf(1L, 2L, 3L)
        coEvery {
            quizApi.getWriteQuizIds(grade = any(), dictionaryId = 1L, coreTypeIds = cores)
        } returns ids
        coEvery { quizApi.getWriteQuizByIds(any()) } answers {
            val requestedIds = firstArg<List<Long>>()
            requestedIds.map { makeQuizEntity(it, grade = 0) }
        }

        val result = portion(limit = 10, maxGrade = 0)

        assertTrue("Result should have at most 3 items", result.size <= 3)
        assertTrue("Result should not be empty", result.isNotEmpty())
    }

    @Test
    fun `empty - returns empty list without calling getByIds`() = runTest {
        stubPrefs()
        coEvery {
            quizApi.getWriteQuizIds(grade = any(), dictionaryId = 1L, coreTypeIds = cores)
        } returns emptyList()

        val result = portion(limit = 10, maxGrade = 0)

        assertEquals("Result should be empty", 0, result.size)
        coVerify(exactly = 0) { quizApi.getWriteQuizByIds(any()) }
    }

    // ===== IS508 no duplicates =====

    private fun stubAddons(earliest: List<Long>? = null, errors: List<Long>? = null, wordOf: (Long) -> Long = { it }) {
        coEvery { prefsProvider.getBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN) } returns (earliest != null)
        coEvery { prefsProvider.getBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN) } returns (errors != null)
        earliest?.let { ids ->
            coEvery { quizApi.getEarliestWriteQuizList(any(), 1L, null, cores) } returns
                ids.map { makeQuizEntity(it, grade = 0, wordId = wordOf(it)) }
        }
        errors?.let { ids ->
            coEvery { quizApi.getFrequentMistakesWriteQuizList(any(), 1L, null, cores) } returns
                ids.map { makeQuizEntity(it, grade = 0, wordId = wordOf(it)) }
        }
    }

    @Test
    fun `add-ons - lexeme never taken twice, next candidates taken`() = runTest {
        // 1 отсекается корзиной, 5 — «давними»: у добавок остаются 4,5 и 6.
        stubPrefs()
        stubBucket(grade = 0, ids = listOf(1L, 2L, 3L))
        stubGetByIds()
        stubAddons(earliest = listOf(1L, 4L, 5L), errors = listOf(1L, 5L, 6L))

        val result = portion(limit = 10, maxGrade = 0)

        val lexemeIds = result.lexemeIds()
        assertEquals(lexemeIds.size, lexemeIds.toSet().size)
        assertEquals(setOf(1L, 2L, 3L, 4L, 5L, 6L), lexemeIds.toSet())
    }

    @Test
    fun `add-on cap - at most ADDON_SIZE taken from candidates`() = runTest {
        stubPrefs()
        stubBucket(grade = 0, ids = listOf(1L, 2L, 3L))
        stubGetByIds()
        stubAddons(earliest = listOf(4L, 5L, 6L, 7L))

        val result = portion(limit = 10, maxGrade = 0)

        val added = result.lexemeIds().toSet() - setOf(1L, 2L, 3L)
        assertEquals(5, result.size)
        assertEquals(2, added.size)
        assertTrue(added.all { it in setOf(4L, 5L, 6L, 7L) })
    }

    @Test
    fun `only mistakes on - filtered against bucket`() = runTest {
        stubPrefs()
        stubBucket(grade = 0, ids = listOf(1L, 2L))
        stubGetByIds()
        stubAddons(errors = listOf(1L, 3L, 4L))

        val result = portion(limit = 10, maxGrade = 0)

        assertEquals(setOf(1L, 2L, 3L, 4L), result.lexemeIds().toSet())
    }

    @Test
    fun `add-on fully overlapping bucket - contributes nothing (Д3)`() = runTest {
        stubPrefs()
        stubBucket(grade = 0, ids = listOf(1L, 2L))
        stubGetByIds()
        stubAddons(earliest = listOf(1L, 2L))

        val result = portion(limit = 10, maxGrade = 0)

        assertEquals(setOf(1L, 2L), result.lexemeIds().toSet())
        assertEquals(2, result.size)
    }

    @Test
    fun `two lexemes of one word - both taken when no other candidates (Д1)`() = runTest {
        stubPrefs()
        stubBucket(grade = 0, ids = listOf(1L, 2L))
        stubGetByIds(wordOf = { 1L })

        val result = portion(limit = 10, maxGrade = 0)

        assertEquals(setOf(1L, 2L), result.lexemeIds().toSet())
    }

    @Test
    fun `word preference - second lexeme of same word skipped while other words available (Д4)`() = runTest {
        // Корзина 0: лексемы 1 и 2 — слово 1; корзина 1: лексемы 3 и 4 — слова 3 и 4.
        // Из корзины берётся не больше limit id, поэтому обе корзины не длиннее limit —
        // иначе часть кандидатов отсекается ещё до выбора слов (нестабильный тест).
        // limit=3: по одной из каждой корзины, добор одного — слово 1 уже в порции,
        // значит добирается оставшееся новое слово: 3 и 4 всегда, из 1/2 — ровно одна.
        stubPrefs()
        stubBucket(grade = 0, ids = listOf(1L, 2L))
        stubBucket(grade = 1, ids = listOf(3L, 4L))
        stubGetByIds(gradeOf = { if (it <= 2L) 0 else 1 }, wordOf = { if (it <= 2L) 1L else it })

        val result = portion(limit = 3, maxGrade = 1)

        val ids = result.lexemeIds().toSet()
        assertEquals(3, ids.size)
        assertTrue(3L in ids)
        assertTrue(4L in ids)
        assertTrue((1L in ids) != (2L in ids))
    }

    @Test
    fun `word preference in add-on - new word first, same word by second pass (Д4)`() = runTest {
        // порция: лексема 1 слова 1; «давние»: 4 (слово 1) и 5 (слово 5);
        // ADDON_SIZE=2 — 5 первым проходом, 4 вторым: обе входят.
        stubPrefs()
        stubBucket(grade = 0, ids = listOf(1L))
        stubGetByIds()
        stubAddons(earliest = listOf(4L, 5L), wordOf = { if (it == 4L) 1L else it })

        val result = portion(limit = 10, maxGrade = 0)

        assertEquals(setOf(1L, 4L, 5L), result.lexemeIds().toSet())
        assertEquals(2, result.map { it.word.id }.toSet().size)
    }

    // ===== IS500 quiz group filter =====

    @Test
    fun `group filter - validated selection passed to all three quiz queries`() = runTest {
        stubPrefs(groupId = 5L)
        coEvery { prefsProvider.getBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN) } returns true
        coEvery { prefsProvider.getBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN) } returns true
        coEvery {
            quizApi.getWriteQuizIds(grade = any(), dictionaryId = 1L, groupId = 5L, coreTypeIds = cores)
        } returns listOf(1L)
        coEvery { quizApi.getWriteQuizByIds(any()) } answers {
            firstArg<List<Long>>().map { makeQuizEntity(it, grade = 0) }
        }
        coEvery {
            quizApi.getEarliestWriteQuizList(any(), 1L, 5L, cores)
        } returns emptyList()
        coEvery {
            quizApi.getFrequentMistakesWriteQuizList(any(), 1L, 5L, cores)
        } returns emptyList()

        portion(limit = 10, maxGrade = 0)

        coVerify { quizApi.getWriteQuizIds(grade = 0, dictionaryId = 1L, groupId = 5L, coreTypeIds = cores) }
        coVerify { quizApi.getEarliestWriteQuizList(any(), 1L, 5L, cores) }
        coVerify { quizApi.getFrequentMistakesWriteQuizList(any(), 1L, 5L, cores) }
    }

    @Test
    fun `getSelectedQuizGroupName - resolves name of validated group`() = runTest {
        coEvery {
            quizGroupSelectionStore.getValidatedSelection(QuizTypes.CHAT, 1L)
        } returns 5L
        coEvery { quizApi.flowQuizGroupCounts(1L) } returns flowOf(
            listOf(QuizGroupCountApiEntity(groupId = 5L, name = "Быт", wordCount = 4)),
        )

        assertEquals("Быт", useCase.getSelectedQuizGroupName(1L))
    }

    @Test
    fun `getSelectedQuizGroupName - null selection means All`() = runTest {
        coEvery {
            quizGroupSelectionStore.getValidatedSelection(QuizTypes.CHAT, 1L)
        } returns null

        assertNull(useCase.getSelectedQuizGroupName(1L))
    }

    // ===== core filter =====

    @Test
    fun `core filter - core type ids passed to all three quiz queries`() = runTest {
        stubPrefs()
        val twoCores = listOf(10L, 11L)
        coEvery { prefsProvider.getBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN) } returns true
        coEvery { prefsProvider.getBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN) } returns true
        coEvery {
            quizApi.getWriteQuizIds(grade = any(), dictionaryId = 1L, coreTypeIds = twoCores)
        } returns listOf(1L)
        coEvery { quizApi.getWriteQuizByIds(any()) } answers {
            firstArg<List<Long>>().map { makeQuizEntity(it, grade = 0) }
        }
        coEvery { quizApi.getEarliestWriteQuizList(any(), 1L, null, twoCores) } returns emptyList()
        coEvery { quizApi.getFrequentMistakesWriteQuizList(any(), 1L, null, twoCores) } returns emptyList()

        useCase.getRandomWriteQuizList(limit = 10, maxGrade = 0, dictionaryId = 1L, coreTypeIds = twoCores)

        coVerify { quizApi.getWriteQuizIds(grade = 0, dictionaryId = 1L, coreTypeIds = twoCores) }
        coVerify { quizApi.getEarliestWriteQuizList(any(), 1L, null, twoCores) }
        coVerify { quizApi.getFrequentMistakesWriteQuizList(any(), 1L, null, twoCores) }
    }

    @Test
    fun `core filter - empty core list returns empty without queries`() = runTest {
        val result = useCase.getRandomWriteQuizList(
            limit = 10,
            maxGrade = 2,
            dictionaryId = 1L,
            coreTypeIds = emptyList(),
        )

        assertTrue(result.isEmpty())
        coVerify(exactly = 0) { quizApi.getWriteQuizIds(any(), any(), any(), any()) }
        coVerify(exactly = 0) { quizGroupSelectionStore.getValidatedSelection(any(), any()) }
    }

    // ===== IS481 getQuizConfig =====

    @Test
    fun `getQuizConfig happy path maps ApiEntity to domain`() = runTest {
        val apiCfg = QuizConfigApiEntity(
            id = 1L,
            dictionaryId = 42L,
            quizMode = "write",
            componentRefs = listOf(
                ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION),
                ComponentTypeRef.UserDefined("Definition"),
            ),
        )
        coEvery { lexemeApi.getQuizConfig(42L, "write") } returns apiCfg

        val result = useCase.getQuizConfig(42L, "write")

        assertNotNull(result)
        assertEquals(42L, result!!.dictionaryId)
        assertEquals("write", result.quizMode)
        assertEquals(2, result.componentRefs.size)
        assertTrue(result.componentRefs[0] is ComponentTypeRef.BuiltIn)
        assertTrue(result.componentRefs[1] is ComponentTypeRef.UserDefined)
    }

    @Test
    fun `getQuizConfig null when row missing`() = runTest {
        coEvery { lexemeApi.getQuizConfig(42L, "write") } returns null

        val result = useCase.getQuizConfig(42L, "write")

        assertNull(result)
    }

    @Test
    fun `getQuizConfig exception returns null and logs`() = runTest {
        coEvery { lexemeApi.getQuizConfig(any(), any()) } throws IllegalStateException("boom")

        val result = useCase.getQuizConfig(42L, "write")

        assertNull(result)
    }

    // ===== quiz picker: кандидаты (ядра) =====

    private fun ctApi(
        id: Long,
        systemKey: BuiltInComponent?,
        name: String?,
        position: Int,
        core: Boolean = true,
        enabled: Boolean = true,
        removedAt: Date? = null,
    ) = ComponentTypeApiEntity(
        id = id,
        systemKey = systemKey,
        dictionaryId = if (systemKey == null) 1L else null,
        name = name,
        template = ComponentTemplate.TEXT,
        position = position,
        core = core,
        enabled = enabled,
        createdAt = Date(),
        updatedAt = Date(),
        removedAt = removedAt,
    )

    @Test
    fun `getQuizCoreTypes keeps live enabled cores in position order`() = runTest {
        coEvery { lexemeApi.getComponentTypes(1L) } returns listOf(
            ctApi(2L, null, "Definition", 1),
            ctApi(1L, BuiltInComponent.TRANSLATION, null, 0),
        )

        val result = useCase.getQuizCoreTypes(1L)

        assertEquals(2, result.size)
        assertEquals(BuiltInComponent.TRANSLATION, result[0].systemKey)
        assertEquals("Definition", result[1].name)
    }

    @Test
    fun `getQuizCoreTypes empty proxies to empty list`() = runTest {
        coEvery { lexemeApi.getComponentTypes(1L) } returns emptyList()

        assertTrue(useCase.getQuizCoreTypes(1L).isEmpty())
    }

    @Test
    fun `getQuizCoreTypes filters non-core types`() = runTest {
        coEvery { lexemeApi.getComponentTypes(1L) } returns listOf(
            ctApi(1L, BuiltInComponent.TRANSLATION, null, 0),
            ctApi(2L, null, "Комментарий", 1, core = false),
        )

        val result = useCase.getQuizCoreTypes(1L)

        assertEquals(listOf(BuiltInComponent.TRANSLATION), result.map { it.systemKey })
    }

    @Test
    fun `getQuizCoreTypes filters disabled and removed cores`() = runTest {
        coEvery { lexemeApi.getComponentTypes(1L) } returns listOf(
            ctApi(1L, BuiltInComponent.TRANSLATION, null, 0),
            ctApi(2L, null, "Выключено", 1, enabled = false),
            ctApi(3L, null, "Удалено", 2, removedAt = Date()),
        )

        val result = useCase.getQuizCoreTypes(1L)

        assertEquals(1, result.size)
        assertEquals(BuiltInComponent.TRANSLATION, result.single().systemKey)
    }

    // Чип и подпись показать можно, а спросить — только TEXT: CHOICE и
    // captioned (встроенный «Пример», кастомы) в кандидаты не попадают.
    @Test
    fun `getQuizCoreTypes whitelists only text template`() = runTest {
        coEvery { lexemeApi.getComponentTypes(1L) } returns listOf(
            ctApi(1L, BuiltInComponent.TRANSLATION, null, 0),
            ctApi(2L, BuiltInComponent.PART_OF_SPEECH, null, 1).copy(template = ComponentTemplate.CHOICE),
            ctApi(3L, BuiltInComponent.EXAMPLE, null, 2).copy(template = ComponentTemplate.CAPTIONED_TEXT),
            ctApi(4L, null, "Цитата", 3).copy(template = ComponentTemplate.CAPTIONED_TEXT),
            ctApi(5L, null, "Definition", 4),
        )

        val result = useCase.getQuizCoreTypes(1L)

        assertEquals(2, result.size)
        assertTrue(result.all { it.template == ComponentTemplate.TEXT })
    }

    // ===== quiz picker: набор в prefs =====

    private fun stubPicker(raw: String?) {
        coEvery { prefsProvider.getStringByRawKey("quiz_picker_dict_1") } returns raw
    }

    @Test
    fun `getQuizPickerSelection decodes legacy single builtin value`() = runTest {
        stubPicker("builtin:translation")

        assertEquals(setOf(translation), useCase.getQuizPickerSelection(1L))
    }

    @Test
    fun `getQuizPickerSelection decodes legacy single user defined value`() = runTest {
        stubPicker("user:Definition")

        assertEquals(setOf(definition), useCase.getQuizPickerSelection(1L))
    }

    @Test
    fun `getQuizPickerSelection decodes set joined by unit separator`() = runTest {
        stubPicker("builtin:translation\u001Fuser:Definition")

        assertEquals(setOf(translation, definition), useCase.getQuizPickerSelection(1L))
    }

    @Test
    fun `getQuizPickerSelection drops broken tokens keeps valid`() = runTest {
        stubPicker("builtin:unknown_xyz\u001Fgarbage\u001Fuser:Definition\u001FUSER:x")

        assertEquals(setOf(definition), useCase.getQuizPickerSelection(1L))
    }

    @Test
    fun `getQuizPickerSelection decodes user defined name with colon (substringAfter first colon)`() = runTest {
        stubPicker("user:My:Type")

        assertEquals(setOf(ComponentTypeRef.UserDefined("My:Type")), useCase.getQuizPickerSelection(1L))
    }

    @Test
    fun `getQuizPickerSelection unknown builtin key returns empty (future-proof)`() = runTest {
        stubPicker("builtin:unknown_xyz")

        assertTrue(useCase.getQuizPickerSelection(1L).isEmpty())
    }

    @Test
    fun `getQuizPickerSelection corrupted format returns empty`() = runTest {
        stubPicker("garbage")

        assertTrue(useCase.getQuizPickerSelection(1L).isEmpty())
    }

    @Test
    fun `getQuizPickerSelection missing pref returns empty`() = runTest {
        stubPicker(null)

        assertTrue(useCase.getQuizPickerSelection(1L).isEmpty())
    }

    @Test
    fun `getQuizPickerSelection empty string returns empty`() = runTest {
        stubPicker("")

        assertTrue(useCase.getQuizPickerSelection(1L).isEmpty())
    }

    @Test
    fun `getQuizPickerSelection empty builtin key returns empty`() = runTest {
        stubPicker("builtin:")

        assertTrue(useCase.getQuizPickerSelection(1L).isEmpty())
    }

    @Test
    fun `getQuizPickerSelection prefix is case-sensitive`() = runTest {
        stubPicker("USER:Definition")

        assertTrue(useCase.getQuizPickerSelection(1L).isEmpty())
    }

    @Test
    fun `getQuizPickerSelection no colon returns empty`() = runTest {
        stubPicker("user")

        assertTrue(useCase.getQuizPickerSelection(1L).isEmpty())
    }

    @Test
    fun `getQuizPickerSelection round-trip empty user defined name`() = runTest {
        stubPicker("user:")

        assertEquals(setOf(ComponentTypeRef.UserDefined("")), useCase.getQuizPickerSelection(1L))
    }

    @Test
    fun `setQuizPickerSelection encodes single builtin`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        useCase.setQuizPickerSelection(1L, setOf(translation))

        coVerify {
            prefsProvider.setStringByRawKey("quiz_picker_dict_1", "builtin:translation")
        }
    }

    @Test
    fun `setQuizPickerSelection encodes set joined by unit separator`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        useCase.setQuizPickerSelection(1L, linkedSetOf(translation, definition))

        coVerify {
            prefsProvider.setStringByRawKey("quiz_picker_dict_1", "builtin:translation\u001Fuser:Definition")
        }
    }

    @Test
    fun `setQuizPickerSelection encodes empty user defined name`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        useCase.setQuizPickerSelection(1L, setOf(ComponentTypeRef.UserDefined("")))

        coVerify {
            prefsProvider.setStringByRawKey("quiz_picker_dict_1", "user:")
        }
    }

    @Test
    fun `per-dictionary keys isolated by id`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        useCase.setQuizPickerSelection(7L, setOf(translation))
        useCase.setQuizPickerSelection(42L, setOf(definition))

        coVerify {
            prefsProvider.setStringByRawKey("quiz_picker_dict_7", "builtin:translation")
        }
        coVerify {
            prefsProvider.setStringByRawKey("quiz_picker_dict_42", "user:Definition")
        }
    }

    @Test
    fun `overwrite on same dict key invokes set twice with same key`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        useCase.setQuizPickerSelection(1L, setOf(translation))
        useCase.setQuizPickerSelection(1L, setOf(definition))

        coVerify(exactly = 2) {
            prefsProvider.setStringByRawKey(eq("quiz_picker_dict_1"), any())
        }
    }

    // ===== часть речи для чипа =====

    @Test
    fun `getPartOfSpeechOptions loads options of builtin part of speech type`() = runTest {
        coEvery { lexemeApi.getComponentTypes(1L) } returns listOf(
            ctApi(1L, BuiltInComponent.TRANSLATION, null, 0),
            ctApi(2L, BuiltInComponent.PART_OF_SPEECH, null, 1, core = false)
                .copy(template = ComponentTemplate.CHOICE),
        )
        coEvery { lexemeApi.getComponentOptions(2L) } returns listOf(
            ComponentOptionApiEntity(id = 100L, componentTypeId = 2L, systemKey = "noun", label = null, position = 0),
            ComponentOptionApiEntity(id = 101L, componentTypeId = 2L, systemKey = null, label = "междометие", position = 1),
        )

        val result = useCase.getPartOfSpeechOptions(1L)

        assertEquals(listOf(100L, 101L), result.map { it.id })
        assertEquals("noun", result[0].systemKey)
        assertEquals("междометие", result[1].label)
    }

    @Test
    fun `getPartOfSpeechOptions without type returns empty`() = runTest {
        coEvery { lexemeApi.getComponentTypes(1L) } returns listOf(
            ctApi(1L, BuiltInComponent.TRANSLATION, null, 0),
        )

        assertTrue(useCase.getPartOfSpeechOptions(1L).isEmpty())
        coVerify(exactly = 0) { lexemeApi.getComponentOptions(any()) }
    }
}
