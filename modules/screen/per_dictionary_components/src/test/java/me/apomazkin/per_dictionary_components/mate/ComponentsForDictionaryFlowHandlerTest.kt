@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package me.apomazkin.per_dictionary_components.mate

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.apomazkin.lexeme.PerDictionarySnapshot
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.per_dictionary_components.deps.PerDictionaryComponentsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests для [ComponentsForDictionaryFlowHandler].
 *
 * - subscribe → initial emit → Msg.ItemsLoaded
 * - re-emit → Msg.ItemsLoaded второй раз
 * - flow throws → Msg.ItemsLoadFailed + logged
 * - subscribe передаёт правильный dictionaryId в useCase (assisted-inject verify)
 */
class ComponentsForDictionaryFlowHandlerTest {

    private val useCase = mockk<PerDictionaryComponentsUseCase>()
    private val logger = mockk<LexemeLogger>(relaxed = true)
    private val DICT_ID = 7L

    private fun emptySnapshot() = PerDictionarySnapshot(
        dictionaryId = DICT_ID,
        dictionaryName = "Spanish",
        types = emptyList(),
        valueCountByType = emptyMap(),
    )

    @Test
    fun `subscribe emits initial Msg ItemsLoaded`() = runTest {
        val flow = MutableSharedFlow<PerDictionarySnapshot>(replay = 1)
        val snapshot = emptySnapshot()
        flow.tryEmit(snapshot)
        every { useCase.flowComponentsForDictionary(DICT_ID) } returns flow

        val handler = ComponentsForDictionaryFlowHandler(DICT_ID, useCase, logger)
        val emissions = mutableListOf<Msg>()

        handler.subscribe(this) { emissions += it }
        advanceUntilIdle()
        handler.unsubscribe()

        assertEquals(1, emissions.size)
        assertTrue(emissions.first() is Msg.ItemsLoaded)
        assertEquals(snapshot, (emissions.first() as Msg.ItemsLoaded).snapshot)
    }

    @Test
    fun `subscribe re-emits on each snapshot`() = runTest {
        val flow = MutableSharedFlow<PerDictionarySnapshot>(replay = 1)
        flow.tryEmit(emptySnapshot())
        every { useCase.flowComponentsForDictionary(DICT_ID) } returns flow

        val handler = ComponentsForDictionaryFlowHandler(DICT_ID, useCase, logger)
        val emissions = mutableListOf<Msg>()

        handler.subscribe(this) { emissions += it }
        advanceUntilIdle()
        flow.tryEmit(emptySnapshot())
        advanceUntilIdle()
        handler.unsubscribe()

        assertEquals(2, emissions.size)
        assertTrue(emissions.all { it is Msg.ItemsLoaded })
    }

    @Test
    fun `flow throws - emits Msg ItemsLoadFailed and logs`() = runTest {
        val boom = RuntimeException("boom")
        every { useCase.flowComponentsForDictionary(DICT_ID) } returns flow { throw boom }

        val handler = ComponentsForDictionaryFlowHandler(DICT_ID, useCase, logger)
        val emissions = mutableListOf<Msg>()

        handler.subscribe(this) { emissions += it }
        advanceUntilIdle()
        handler.unsubscribe()

        assertEquals(1, emissions.size)
        val msg = emissions.first()
        assertTrue(msg is Msg.ItemsLoadFailed)
        assertEquals(boom, (msg as Msg.ItemsLoadFailed).cause)
        coVerify { logger.e(any(), any()) }
    }

    @Test
    fun `subscribe passes correct dictionaryId to useCase`() = runTest {
        val flow = MutableSharedFlow<PerDictionarySnapshot>(replay = 0)
        every { useCase.flowComponentsForDictionary(42L) } returns flow

        val handler = ComponentsForDictionaryFlowHandler(42L, useCase, logger)

        handler.subscribe(this) {}
        advanceUntilIdle()
        handler.unsubscribe()

        verify(exactly = 1) { useCase.flowComponentsForDictionary(42L) }
    }
}
