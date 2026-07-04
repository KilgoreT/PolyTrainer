@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package me.apomazkin.components_manager.mate

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import me.apomazkin.lexeme.ComponentUsage
import me.apomazkin.lexeme.UserDefinedTypesSnapshot
import me.apomazkin.logger.LexemeLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests для [AllUserDefinedTypesFlowHandler].
 *
 * - subscribe → initial emit → Msg.TypesLoaded
 * - re-emit → Msg.TypesLoaded второй раз
 * - flow throws → Msg.TypesLoadFailed + logged
 * - unsubscribe cancels job
 */
class AllUserDefinedTypesFlowHandlerTest {

    private val useCase = mockk<ComponentsManagerUseCase>()
    private val logger = mockk<LexemeLogger>(relaxed = true)

    private fun emptySnapshot() = UserDefinedTypesSnapshot(
        types = emptyList(),
        usage = ComponentUsage(emptyMap(), emptyMap(), emptyMap()),
    )

    @Test
    fun `subscribe emits initial Msg TypesLoaded`() = runTest {
        val flow = MutableSharedFlow<UserDefinedTypesSnapshot>(replay = 1)
        val snapshot = emptySnapshot()
        flow.tryEmit(snapshot)
        every { useCase.flowAllUserDefinedTypes() } returns flow

        val handler = AllUserDefinedTypesFlowHandler(useCase, logger)
        val emissions = mutableListOf<Msg>()

        handler.subscribe(this) { emissions += it }
        advanceUntilIdle()
        handler.unsubscribe()

        assertEquals(1, emissions.size)
        assertTrue(emissions.first() is Msg.TypesLoaded)
        assertEquals(snapshot, (emissions.first() as Msg.TypesLoaded).snapshot)
    }

    @Test
    fun `subscribe re-emits on each snapshot`() = runTest {
        val flow = MutableSharedFlow<UserDefinedTypesSnapshot>(replay = 1)
        flow.tryEmit(emptySnapshot())
        every { useCase.flowAllUserDefinedTypes() } returns flow

        val handler = AllUserDefinedTypesFlowHandler(useCase, logger)
        val emissions = mutableListOf<Msg>()

        handler.subscribe(this) { emissions += it }
        advanceUntilIdle()
        flow.tryEmit(emptySnapshot())
        advanceUntilIdle()
        handler.unsubscribe()

        assertEquals(2, emissions.size)
        assertTrue(emissions.all { it is Msg.TypesLoaded })
    }

    @Test
    fun `flow throws - emits Msg TypesLoadFailed and logs`() = runTest {
        val boom = RuntimeException("boom")
        every { useCase.flowAllUserDefinedTypes() } returns flow { throw boom }

        val handler = AllUserDefinedTypesFlowHandler(useCase, logger)
        val emissions = mutableListOf<Msg>()

        handler.subscribe(this) { emissions += it }
        advanceUntilIdle()
        handler.unsubscribe()

        assertEquals(1, emissions.size)
        val msg = emissions.first()
        assertTrue(msg is Msg.TypesLoadFailed)
        assertEquals(boom, (msg as Msg.TypesLoadFailed).cause)
        coVerify { logger.e(any(), any()) }
    }

    @Test
    fun `unsubscribe cancels job - no further emissions`() = runTest {
        val flow = MutableSharedFlow<UserDefinedTypesSnapshot>(replay = 1)
        flow.tryEmit(emptySnapshot())
        every { useCase.flowAllUserDefinedTypes() } returns flow

        val handler = AllUserDefinedTypesFlowHandler(useCase, logger)
        val emissions = mutableListOf<Msg>()

        handler.subscribe(this) { emissions += it }
        advanceUntilIdle()
        handler.unsubscribe()
        flow.tryEmit(emptySnapshot())
        advanceUntilIdle()

        // only the initial emission - unsubscribe stopped further emits
        assertEquals(1, emissions.size)
    }
}
