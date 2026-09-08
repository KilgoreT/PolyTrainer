@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package me.apomazkin.per_dictionary_components.mate

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Тесты [PerDictionaryComponentsSubHandler] — исполнителя семейства
 * подписок [PerDictionaryComponentsSub]. `flow(sub)` — чистая фабрика
 * Flow: тестируем сбором потока в runTest, без раннера mate.
 *
 * - Components: каждый snapshot → [Msg.ItemsLoaded]; ошибка потока →
 *   [Msg.ItemsLoadFailed] + лог (поток завершается, retry — новой
 *   подпиской через generation);
 * - dictionaryId из подписки пробрасывается в use case.
 */
class PerDictionaryComponentsSubHandlerTest {

    private val useCase = mockk<PerDictionaryComponentsUseCase>()
    private val logger = mockk<LexemeLogger>(relaxed = true)
    private val DICT_ID = 7L

    private val handler get() = PerDictionaryComponentsSubHandler(useCase, logger)

    private fun componentsSub(generation: Int = 0) =
        PerDictionaryComponentsSub.Components(dictionaryId = DICT_ID, generation = generation)

    private fun emptySnapshot() = PerDictionarySnapshot(
        dictionaryId = DICT_ID,
        dictionaryName = "Spanish",
        types = emptyList(),
        valueCountByType = emptyMap(),
    )

    @Test
    fun `Components emits ItemsLoaded for initial snapshot`() = runTest {
        val source = MutableSharedFlow<PerDictionarySnapshot>(replay = 1)
        val snapshot = emptySnapshot()
        source.tryEmit(snapshot)
        every { useCase.flowComponentsForDictionary(DICT_ID) } returns source

        val emissions = mutableListOf<Msg>()
        val job = launch { handler.flow(componentsSub()).collect { emissions += it } }
        advanceUntilIdle()
        job.cancel()

        assertEquals(1, emissions.size)
        assertEquals(snapshot, (emissions.first() as Msg.ItemsLoaded).snapshot)
    }

    @Test
    fun `Components re-emits on each snapshot`() = runTest {
        val source = MutableSharedFlow<PerDictionarySnapshot>(replay = 1)
        source.tryEmit(emptySnapshot())
        every { useCase.flowComponentsForDictionary(DICT_ID) } returns source

        val emissions = mutableListOf<Msg>()
        val job = launch { handler.flow(componentsSub()).collect { emissions += it } }
        advanceUntilIdle()
        source.tryEmit(emptySnapshot())
        advanceUntilIdle()
        job.cancel()

        assertEquals(2, emissions.size)
        assertTrue(emissions.all { it is Msg.ItemsLoaded })
    }

    @Test
    fun `Components flow throws - emits ItemsLoadFailed and logs`() = runTest {
        val boom = RuntimeException("boom")
        every { useCase.flowComponentsForDictionary(DICT_ID) } returns flow { throw boom }

        // catch завершает поток fail-сообщением — toList не виснет.
        val emissions = handler.flow(componentsSub()).toList()

        assertEquals(1, emissions.size)
        assertEquals(boom, (emissions.first() as Msg.ItemsLoadFailed).cause)
        coVerify { logger.e(any(), any()) }
    }

    @Test
    fun `Components passes subscription dictionaryId to useCase`() = runTest {
        val source = MutableSharedFlow<PerDictionarySnapshot>(replay = 0)
        every { useCase.flowComponentsForDictionary(42L) } returns source

        val job = launch {
            handler.flow(
                PerDictionaryComponentsSub.Components(dictionaryId = 42L, generation = 0),
            ).collect {}
        }
        advanceUntilIdle()
        job.cancel()

        verify(exactly = 1) { useCase.flowComponentsForDictionary(42L) }
    }
}
