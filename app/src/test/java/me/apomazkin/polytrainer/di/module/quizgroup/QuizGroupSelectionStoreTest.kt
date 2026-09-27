package me.apomazkin.polytrainer.di.module.quizgroup

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.QuizGroupCountApiEntity
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.MIN_QUIZ_WORDS
import me.apomazkin.quiz.QuizTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * IS500 | Selection-store: валидация при чтении (Д3 — мусор, мёртвая,
 * усохшая группа → молча «Все»), запись/стирание ключа, изоляция
 * ключей по (тип × словарь), сырой поток.
 */
class QuizGroupSelectionStoreTest {

    private val quizApi = mockk<CoreDbApi.QuizApi>()
    private val prefsProvider = mockk<PrefsProvider>()
    private val logger = mockk<LexemeLogger>(relaxed = true)

    private val store = QuizGroupSelectionStore(
        quizApi = quizApi,
        prefsProvider = prefsProvider,
        logger = logger,
    )

    private val key = "quiz_group_chat_dict_1"

    private fun stubCounts(vararg counts: QuizGroupCountApiEntity, allCount: Int = 10) {
        every { quizApi.flowQuizGroupCounts(1L) } returns flowOf(counts.toList())
        every { quizApi.flowDictionaryQuizWordCount(1L) } returns flowOf(allCount)
    }

    private fun living(id: Long, count: Int = MIN_QUIZ_WORDS) =
        QuizGroupCountApiEntity(groupId = id, name = "g$id", wordCount = count)

    @Test
    fun `valid persisted group - returned`() = runTest {
        coEvery { prefsProvider.getStringByRawKey(key) } returns "5"
        stubCounts(living(5L))

        assertEquals(5L, store.getValidatedSelection(QuizTypes.CHAT, 1L))
    }

    @Test
    fun `no pref - All`() = runTest {
        coEvery { prefsProvider.getStringByRawKey(key) } returns null
        stubCounts(living(5L))

        assertNull(store.getValidatedSelection(QuizTypes.CHAT, 1L))
    }

    @Test
    fun `garbage pref - silent All`() = runTest {
        coEvery { prefsProvider.getStringByRawKey(key) } returns "not-a-number"
        stubCounts(living(5L))

        assertNull(store.getValidatedSelection(QuizTypes.CHAT, 1L))
    }

    @Test
    fun `dead group - silent All`() = runTest {
        coEvery { prefsProvider.getStringByRawKey(key) } returns "99"
        stubCounts(living(5L))

        assertNull(store.getValidatedSelection(QuizTypes.CHAT, 1L))
    }

    @Test
    fun `shrunken group - silent All`() = runTest {
        coEvery { prefsProvider.getStringByRawKey(key) } returns "5"
        stubCounts(living(5L, count = MIN_QUIZ_WORDS - 1))

        assertNull(store.getValidatedSelection(QuizTypes.CHAT, 1L))
    }

    @Test
    fun `set selection - group id written as string`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        store.setSelection(QuizTypes.CHAT, 1L, 5L)

        coVerify { prefsProvider.setStringByRawKey(key, "5") }
    }

    @Test
    fun `set All - key erased`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        store.setSelection(QuizTypes.CHAT, 1L, null)

        coVerify { prefsProvider.setStringByRawKey(key, null) }
    }

    @Test
    fun `keys isolated by quiz type and dictionary`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        store.setSelection(QuizTypes.CHAT, 1L, 5L)
        store.setSelection("flip_cards", 1L, 6L)
        store.setSelection(QuizTypes.CHAT, 2L, 7L)

        coVerify { prefsProvider.setStringByRawKey("quiz_group_chat_dict_1", "5") }
        coVerify { prefsProvider.setStringByRawKey("quiz_group_flip_cards_dict_1", "6") }
        coVerify { prefsProvider.setStringByRawKey("quiz_group_chat_dict_2", "7") }
    }

    @Test
    fun `raw selection flow - parses long, garbage becomes All`() = runTest {
        every { prefsProvider.getStringFlowByRawKey(key) } returns flowOf("5")
        assertEquals(5L, store.flowSelection(QuizTypes.CHAT, 1L).first())

        every { prefsProvider.getStringFlowByRawKey(key) } returns flowOf("garbage")
        assertNull(store.flowSelection(QuizTypes.CHAT, 1L).first())
    }
}
