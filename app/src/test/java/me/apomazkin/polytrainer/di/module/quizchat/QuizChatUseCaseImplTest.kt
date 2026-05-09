package me.apomazkin.polytrainer.di.module.quizchat

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.WordApiEntity
import me.apomazkin.core_db_api.entity.WriteQuizApiEntity
import me.apomazkin.core_db_api.entity.WriteQuizComplexEntity
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class QuizChatUseCaseImplTest {

    private val dictionaryApi = mockk<CoreDbApi.DictionaryApi>()
    private val quizApi = mockk<CoreDbApi.QuizApi>()
    private val prefsProvider = mockk<PrefsProvider>()

    private val useCase = QuizChatUseCaseImpl(
        dictionaryApi = dictionaryApi,
        quizApi = quizApi,
        prefsProvider = prefsProvider,
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
        lexemeData = LexemeApiEntity(id = id, wordId = id, addDate = Date()),
        wordData = WordApiEntity(id = id, dictionaryId = dictId, value = "word_$id", addDate = Date()),
        sampleData = emptyList(),
    )

    // Нормальный кейс: 100 ID, limit=10 → получаем ≤10
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

    // Мало данных: 3 ID, limit=10 → получаем 3
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

    // Пустой список: 0 ID → не вызывает getWriteQuizByIds, возвращает пустой
    @Test
    fun `empty - returns empty list without calling getByIds`() = runTest {
        stubPrefs()
        coEvery { quizApi.getWriteQuizIds(grade = any(), dictionaryId = 1L) } returns emptyList()

        val result = useCase.getRandomWriteQuizList(limit = 10, maxGrade = 0, dictionaryId = 1L)

        assertEquals("Result should be empty", 0, result.size)
        coVerify(exactly = 0) { quizApi.getWriteQuizByIds(any()) }
    }
}
