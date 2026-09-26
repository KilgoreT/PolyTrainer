@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package me.apomazkin.components_manager.mate

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.lexeme.ComponentUsage
import me.apomazkin.lexeme.UserDefinedTypesSnapshot
import me.apomazkin.logger.LexemeLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Тесты [ComponentsManagerSubHandler] — исполнителя семейства подписок
 * [ComponentsManagerSub]. `flow(sub)` — чистая фабрика Flow: тестируем
 * сбором потока в runTest, без раннера mate.
 *
 * - AllTypes: каждый snapshot → [Msg.TypesLoaded]; ошибка потока →
 *   [Msg.TypesLoadFailed] + лог (поток завершается, retry — новой
 *   подпиской через generation).
 * - Dictionaries: список словарей → [Msg.DictionariesLoaded]; ошибка
 *   деградирует до пустого списка (retry не предусмотрен).
 */
class ComponentsManagerSubHandlerTest {
    private val useCase = mockk<ComponentsManagerUseCase>()
    private val logger = mockk<LexemeLogger>(relaxed = true)

    private val handler get() = ComponentsManagerSubHandler(useCase, logger)

    private fun emptySnapshot() =
        UserDefinedTypesSnapshot(
            types = emptyList(),
            usage = ComponentUsage(emptyMap(), emptyMap(), emptyMap()),
        )

    // ===== ComponentsManagerSub.AllTypes =====

    @Test
    fun `AllTypes emits TypesLoaded for initial snapshot`() =
        runTest {
            val source = MutableSharedFlow<UserDefinedTypesSnapshot>(replay = 1)
            val snapshot = emptySnapshot()
            source.tryEmit(snapshot)
            every { useCase.flowAllUserDefinedTypes() } returns source

            val emissions = mutableListOf<Msg>()
            val job = launch {
                handler.flow(ComponentsManagerSub.AllTypes(generation = 0)).collect { emissions += it }
            }
            advanceUntilIdle()
            job.cancel()

            assertEquals(1, emissions.size)
            assertEquals(snapshot, (emissions.first() as Msg.TypesLoaded).snapshot)
        }

    @Test
    fun `AllTypes re-emits on each snapshot`() =
        runTest {
            val source = MutableSharedFlow<UserDefinedTypesSnapshot>(replay = 1)
            source.tryEmit(emptySnapshot())
            every { useCase.flowAllUserDefinedTypes() } returns source

            val emissions = mutableListOf<Msg>()
            val job = launch {
                handler.flow(ComponentsManagerSub.AllTypes(generation = 0)).collect { emissions += it }
            }
            advanceUntilIdle()
            source.tryEmit(emptySnapshot())
            advanceUntilIdle()
            job.cancel()

            assertEquals(2, emissions.size)
            assertTrue(emissions.all { it is Msg.TypesLoaded })
        }

    @Test
    fun `AllTypes flow throws - emits TypesLoadFailed and logs`() =
        runTest {
            val boom = RuntimeException("boom")
            every { useCase.flowAllUserDefinedTypes() } returns flow { throw boom }

            // catch завершает поток fail-сообщением — toList не виснет.
            val emissions = handler.flow(ComponentsManagerSub.AllTypes(generation = 0)).toList()

            assertEquals(1, emissions.size)
            assertEquals(boom, (emissions.first() as Msg.TypesLoadFailed).cause)
            coVerify { logger.e(any(), any()) }
        }

    // ===== ComponentsManagerSub.Dictionaries =====

    @Test
    fun `Dictionaries emits DictionariesLoaded with list`() =
        runTest {
            val dict = DictionaryApiEntity(
                id = 1L,
                numericCode = null,
                name = "D1",
                addDate = Date(0L),
            )
            every { useCase.flowDictionaries() } returns flowOf(listOf(dict))

            val emissions = handler.flow(ComponentsManagerSub.Dictionaries).toList()

            assertEquals(listOf<Msg>(Msg.DictionariesLoaded(listOf(dict))), emissions)
        }

    @Test
    fun `Dictionaries flow throws - degrades to empty list and logs`() =
        runTest {
            every { useCase.flowDictionaries() } returns flow { throw RuntimeException("boom") }

            val emissions = handler.flow(ComponentsManagerSub.Dictionaries).toList()

            assertEquals(listOf<Msg>(Msg.DictionariesLoaded(emptyList())), emissions)
            coVerify { logger.e(any(), any()) }
        }
}
