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
import me.apomazkin.quiz.PersistedQuizGroups
import me.apomazkin.quiz.QuizTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Selection-store набора групп: валидация при чтении (мусор, мёртвая,
 * усохшая группа выпадают поэлементно, остальные остаются), сортировка
 * групп, запись/стирание ключа, изоляция ключей по (тип × словарь),
 * сырой поток с пометкой мусора.
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

    private val key = "quiz_groups_chat_dict_1"

    private fun stubCounts(vararg counts: QuizGroupCountApiEntity, allCount: Int = 10) {
        every { quizApi.flowQuizGroupCounts(1L) } returns flowOf(counts.toList())
        every { quizApi.flowDictionaryQuizWordCount(1L) } returns flowOf(allCount)
    }

    private fun living(id: Long, count: Int = MIN_QUIZ_WORDS, name: String = "g$id") =
        QuizGroupCountApiEntity(groupId = id, name = name, wordCount = count)

    @Test
    fun `valid persisted groups - returned`() = runTest {
        coEvery { prefsProvider.getStringByRawKey(key) } returns "5,7"
        stubCounts(living(5L), living(7L))

        assertEquals(setOf(5L, 7L), store.getValidatedSelection(QuizTypes.CHAT, 1L))
    }

    @Test
    fun `no pref - All`() = runTest {
        coEvery { prefsProvider.getStringByRawKey(key) } returns null
        stubCounts(living(5L))

        assertTrue(store.getValidatedSelection(QuizTypes.CHAT, 1L).isEmpty())
    }

    @Test
    fun `garbage token - dropped, valid kept`() = runTest {
        coEvery { prefsProvider.getStringByRawKey(key) } returns "5,abc"
        stubCounts(living(5L))

        assertEquals(setOf(5L), store.getValidatedSelection(QuizTypes.CHAT, 1L))
    }

    @Test
    fun `dead group - dropped, live kept`() = runTest {
        coEvery { prefsProvider.getStringByRawKey(key) } returns "5,99"
        stubCounts(living(5L))

        assertEquals(setOf(5L), store.getValidatedSelection(QuizTypes.CHAT, 1L))
    }

    @Test
    fun `shrunken group - dropped, live kept`() = runTest {
        coEvery { prefsProvider.getStringByRawKey(key) } returns "5,7"
        stubCounts(living(5L, count = MIN_QUIZ_WORDS - 1), living(7L))

        assertEquals(setOf(7L), store.getValidatedSelection(QuizTypes.CHAT, 1L))
    }

    @Test
    fun `all dropped - All`() = runTest {
        coEvery { prefsProvider.getStringByRawKey(key) } returns "99"
        stubCounts(living(5L))

        assertTrue(store.getValidatedSelection(QuizTypes.CHAT, 1L).isEmpty())
    }

    @Test
    fun `legacy single-group key is not read`() = runTest {
        coEvery { prefsProvider.getStringByRawKey(key) } returns null
        stubCounts(living(5L))

        store.getValidatedSelection(QuizTypes.CHAT, 1L)

        coVerify(exactly = 0) { prefsProvider.getStringByRawKey("quiz_group_chat_dict_1") }
    }

    @Test
    fun `validated state - groups sorted by name`() = runTest {
        coEvery { prefsProvider.getStringByRawKey(key) } returns null
        stubCounts(living(1L, name = "Дом"), living(2L, name = "Быт"))

        val state = store.getValidatedState(QuizTypes.CHAT, 1L)

        assertEquals(listOf("Быт", "Дом"), state.options.groups.map { it.name })
    }

    @Test
    fun `set selection - ids sorted, comma separated`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        store.setSelection(QuizTypes.CHAT, 1L, setOf(7L, 5L))

        coVerify { prefsProvider.setStringByRawKey(key, "5,7") }
    }

    @Test
    fun `set All - key erased`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        store.setSelection(QuizTypes.CHAT, 1L, emptySet())

        coVerify { prefsProvider.setStringByRawKey(key, null) }
    }

    @Test
    fun `keys isolated by quiz type and dictionary`() = runTest {
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        store.setSelection(QuizTypes.CHAT, 1L, setOf(5L))
        store.setSelection("flip_cards", 1L, setOf(6L))
        store.setSelection(QuizTypes.CHAT, 2L, setOf(7L))

        coVerify { prefsProvider.setStringByRawKey("quiz_groups_chat_dict_1", "5") }
        coVerify { prefsProvider.setStringByRawKey("quiz_groups_flip_cards_dict_1", "6") }
        coVerify { prefsProvider.setStringByRawKey("quiz_groups_chat_dict_2", "7") }
    }

    @Test
    fun `raw selection flow - parses set, flags garbage`() = runTest {
        every { prefsProvider.getStringFlowByRawKey(key) } returns flowOf("5,7")
        assertEquals(PersistedQuizGroups(setOf(5L, 7L), hasGarbage = false), store.flowSelection(QuizTypes.CHAT, 1L).first())

        every { prefsProvider.getStringFlowByRawKey(key) } returns flowOf("5,garbage")
        assertEquals(PersistedQuizGroups(setOf(5L), hasGarbage = true), store.flowSelection(QuizTypes.CHAT, 1L).first())
    }
}
