package me.apomazkin.polytrainer.di.module.quizchat

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.QuizConfigApiEntity
import me.apomazkin.core_db_api.entity.WordApiEntity
import me.apomazkin.core_db_api.entity.WriteQuizApiEntity
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
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
    private val logger = mockk<LexemeLogger>(relaxed = true)

    private val useCase = QuizChatUseCaseImpl(
        dictionaryApi = dictionaryApi,
        quizApi = quizApi,
        lexemeApi = lexemeApi,
        prefsProvider = prefsProvider,
        logger = logger,
    )

    private fun stubPrefs() {
        coEvery { prefsProvider.getBoolean(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN) } returns false
        coEvery { prefsProvider.getBoolean(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN) } returns false
    }

    private fun makeQuizEntity(id: Long, grade: Int, dictId: Long = 1L) = WriteQuizComplexEntity(
        quizData = WriteQuizApiEntity(
            id = id,
            dictionaryId = dictId,
            lexemeId = id,
            grade = grade,
            addDate = Date(),
        ),
        lexemeData = LexemeApiEntity(id = id, addDate = Date()),
        wordData = WordApiEntity(id = id, dictionaryId = dictId, value = "word_$id", addDate = Date()),
        sampleData = emptyList(),
    )

    @Test
    fun `normal - returns items within limit`() = runTest {
        stubPrefs()
        val ids = (1L..100L).toList()
        coEvery { quizApi.getWriteQuizIds(grade = any(), dictionaryId = 1L) } returns ids
        coEvery { quizApi.getWriteQuizByIds(any()) } answers {
            val requestedIds = firstArg<List<Long>>()
            requestedIds.map { makeQuizEntity(it, grade = 0) }
        }

        val result = useCase.getRandomWriteQuizList(limit = 10, maxGrade = 0, dictionaryId = 1L)

        assertTrue("Result should not exceed limit", result.size <= 10)
        assertTrue("Result should not be empty", result.isNotEmpty())
        coVerify { quizApi.getWriteQuizIds(grade = 0, dictionaryId = 1L) }
        coVerify { quizApi.getWriteQuizByIds(match { it.size <= 10 }) }
    }

    @Test
    fun `few items - returns all available`() = runTest {
        stubPrefs()
        val ids = listOf(1L, 2L, 3L)
        coEvery { quizApi.getWriteQuizIds(grade = any(), dictionaryId = 1L) } returns ids
        coEvery { quizApi.getWriteQuizByIds(any()) } answers {
            val requestedIds = firstArg<List<Long>>()
            requestedIds.map { makeQuizEntity(it, grade = 0) }
        }

        val result = useCase.getRandomWriteQuizList(limit = 10, maxGrade = 0, dictionaryId = 1L)

        assertTrue("Result should have at most 3 items", result.size <= 3)
        assertTrue("Result should not be empty", result.isNotEmpty())
    }

    @Test
    fun `empty - returns empty list without calling getByIds`() = runTest {
        stubPrefs()
        coEvery { quizApi.getWriteQuizIds(grade = any(), dictionaryId = 1L) } returns emptyList()

        val result = useCase.getRandomWriteQuizList(limit = 10, maxGrade = 0, dictionaryId = 1L)

        assertEquals("Result should be empty", 0, result.size)
        coVerify(exactly = 0) { quizApi.getWriteQuizByIds(any()) }
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

    // ===== IS481 quiz picker (AGG-12) =====

    private fun ctApi(id: Long, systemKey: BuiltInComponent?, name: String?, position: Int) =
        ComponentTypeApiEntity(
            id = id,
            systemKey = systemKey,
            dictionaryId = if (systemKey == null) 1L else null,
            name = name,
            template = ComponentTemplate.TEXT,
            position = position,
            createdAt = Date(),
            updatedAt = Date(),
        )

    @Test
    fun `getAvailableTypes proxies LexemeApi preserving order`() = runTest {
        coEvery { lexemeApi.getComponentTypes(1L) } returns listOf(
            ctApi(1L, BuiltInComponent.TRANSLATION, null, 0),
            ctApi(2L, null, "Definition", 1),
        )

        val result = useCase.getAvailableTypes(1L)

        assertEquals(2, result.size)
        assertEquals(BuiltInComponent.TRANSLATION, result[0].systemKey)
        assertEquals("Definition", result[1].name)
    }

    @Test
    fun `getAvailableTypes empty proxies to empty list`() = runTest {
        coEvery { lexemeApi.getComponentTypes(1L) } returns emptyList()

        assertTrue(useCase.getAvailableTypes(1L).isEmpty())
    }

    // IS486: CHOICE в квизах v1 не участвует (spec §9.6) — пикер не предлагает.
    @Test
    fun `getAvailableTypes filters choice template`() = runTest {
        coEvery { lexemeApi.getComponentTypes(1L) } returns listOf(
            ctApi(1L, BuiltInComponent.TRANSLATION, null, 0),
            ctApi(2L, BuiltInComponent.PART_OF_SPEECH, null, 1).copy(template = ComponentTemplate.CHOICE),
            ctApi(3L, null, "Definition", 2),
        )

        val result = useCase.getAvailableTypes(1L)

        assertEquals(2, result.size)
        assertTrue(result.none { it.template == ComponentTemplate.CHOICE })
    }

    @Test
    fun `getQuizPickerSelection decodes builtin translation`() = runTest {
        coEvery { prefsProvider.getStringByRawKey("quiz_picker_dict_1") } returns "builtin:translation"

        val result = useCase.getQuizPickerSelection(1L)

        assertEquals(ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION), result)
    }

    @Test
    fun `getQuizPickerSelection decodes user defined`() = runTest {
        coEvery { prefsProvider.getStringByRawKey("quiz_picker_dict_1") } returns "user:Definition"

        val result = useCase.getQuizPickerSelection(1L)

        assertEquals(ComponentTypeRef.UserDefined("Definition"), result)
    }

    @Test
    fun `getQuizPickerSelection decodes user defined name with colon (substringAfter first colon)`() = runTest {
        coEvery { prefsProvider.getStringByRawKey("quiz_picker_dict_1") } returns "user:My:Type"

        val result = useCase.getQuizPickerSelection(1L)

        assertEquals(ComponentTypeRef.UserDefined("My:Type"), result)
    }

    @Test
    fun `getQuizPickerSelection unknown builtin key returns null (future-proof)`() = runTest {
        coEvery { prefsProvider.getStringByRawKey("quiz_picker_dict_1") } returns "builtin:unknown_xyz"

        assertNull(useCase.getQuizPickerSelection(1L))
    }

    @Test
    fun `getQuizPickerSelection corrupted format returns null`() = runTest {
        coEvery { prefsProvider.getStringByRawKey("quiz_picker_dict_1") } returns "garbage"

        assertNull(useCase.getQuizPickerSelection(1L))
    }

    @Test
    fun `getQuizPickerSelection empty pref returns null`() = runTest {
        coEvery { prefsProvider.getStringByRawKey("quiz_picker_dict_1") } returns null

        assertNull(useCase.getQuizPickerSelection(1L))
    }

    @Test
    fun `getQuizPickerSelection empty builtin key returns null`() = runTest {
        coEvery { prefsProvider.getStringByRawKey("quiz_picker_dict_1") } returns "builtin:"

        assertNull(useCase.getQuizPickerSelection(1L))
    }

    @Test
    fun `getQuizPickerSelection prefix is case-sensitive`() = runTest {
        coEvery { prefsProvider.getStringByRawKey("quiz_picker_dict_1") } returns "USER:Definition"

        assertNull(useCase.getQuizPickerSelection(1L))
    }

    @Test
    fun `getQuizPickerSelection no colon returns null`() = runTest {
        coEvery { prefsProvider.getStringByRawKey("quiz_picker_dict_1") } returns "user"

        assertNull(useCase.getQuizPickerSelection(1L))
    }

    @Test
    fun `setQuizPickerSelection encodes builtin`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        useCase.setQuizPickerSelection(1L, ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION))

        coVerify {
            prefsProvider.setStringByRawKey("quiz_picker_dict_1", "builtin:translation")
        }
    }

    @Test
    fun `setQuizPickerSelection encodes user defined`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        useCase.setQuizPickerSelection(1L, ComponentTypeRef.UserDefined("Definition"))

        coVerify {
            prefsProvider.setStringByRawKey("quiz_picker_dict_1", "user:Definition")
        }
    }

    @Test
    fun `setQuizPickerSelection encodes empty user defined name`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        useCase.setQuizPickerSelection(1L, ComponentTypeRef.UserDefined(""))

        coVerify {
            prefsProvider.setStringByRawKey("quiz_picker_dict_1", "user:")
        }
    }

    @Test
    fun `getQuizPickerSelection round-trip empty user defined name`() = runTest {
        coEvery { prefsProvider.getStringByRawKey("quiz_picker_dict_1") } returns "user:"

        val result = useCase.getQuizPickerSelection(1L)

        assertEquals(ComponentTypeRef.UserDefined(""), result)
    }

    @Test
    fun `per-dictionary keys isolated by id`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        useCase.setQuizPickerSelection(7L, ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION))
        useCase.setQuizPickerSelection(42L, ComponentTypeRef.UserDefined("Definition"))

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

        useCase.setQuizPickerSelection(1L, ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION))
        useCase.setQuizPickerSelection(1L, ComponentTypeRef.UserDefined("Definition"))

        coVerify(exactly = 2) {
            prefsProvider.setStringByRawKey(eq("quiz_picker_dict_1"), any())
        }
    }
}
