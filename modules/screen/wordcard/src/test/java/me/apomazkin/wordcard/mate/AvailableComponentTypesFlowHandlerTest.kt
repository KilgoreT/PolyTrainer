package me.apomazkin.wordcard.mate

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.wordcard.deps.AvailableComponents
import me.apomazkin.wordcard.deps.WordCardUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * §9.4 AvailableComponentTypesFlowHandler — race-critical (runTest + TestScope).
 * TDD red — handler ещё не написан, нужен kotlinx-coroutines-test (добавлен в build.gradle).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AvailableComponentTypesFlowHandlerTest {

    private object NoopLogger : LexemeLogger {
        override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
    }

    private val tr = ctype(50L, TR)
    private val ex = ctype(51L, ComponentTypeRef.UserDefined("Example"))

    private fun fake(flowImpl: (Long) -> Flow<AvailableComponents>): WordCardUseCase =
        object : WordCardUseCase by NotImplementedUseCase {
            override fun flowAvailableComponentTypes(dictionaryId: Long) = flowImpl(dictionaryId)
        }

    @Test
    fun `runEffect without subscribe is no-op`() = runTest {
        val handler = AvailableComponentTypesFlowHandler(fake { flowOf(AvailableComponents(listOf(tr))) }, NoopLogger)
        val msgs = mutableListOf<Msg>()
        handler.runEffect(DatasourceEffect.LoadAvailableComponentTypes(1L)) { msgs += it }
        advanceUntilIdle()
        assertTrue("scope ?: return — без краша и Msg", msgs.isEmpty())
    }

    @Test
    fun `happy path emits ComponentTypesLoaded`() = runTest {
        val handler = AvailableComponentTypesFlowHandler(fake { flowOf(AvailableComponents(listOf(tr, ex))) }, NoopLogger)
        val msgs = mutableListOf<Msg>()
        handler.subscribe(this) { msgs += it }
        handler.runEffect(DatasourceEffect.LoadAvailableComponentTypes(1L)) { msgs += it }
        advanceUntilIdle()
        assertEquals(listOf<Msg>(Msg.ComponentTypesLoaded(AvailableComponents(listOf(tr, ex)))), msgs)
        handler.unsubscribe()
    }

    @Test
    fun `flow error emits ComponentTypesLoadFailed`() = runTest {
        val handler = AvailableComponentTypesFlowHandler(
            fake { flow { throw IOException("boom") } },
            NoopLogger,
        )
        val msgs = mutableListOf<Msg>()
        handler.subscribe(this) { msgs += it }
        handler.runEffect(DatasourceEffect.LoadAvailableComponentTypes(1L)) { msgs += it }
        advanceUntilIdle()
        assertTrue(msgs.single() is Msg.ComponentTypesLoadFailed)
        handler.unsubscribe()
    }

    @Test
    fun `CancellationException not mapped to ComponentTypesLoadFailed`() = runTest {
        val handler = AvailableComponentTypesFlowHandler(
            fake { flow { throw kotlinx.coroutines.CancellationException() } },
            NoopLogger,
        )
        val msgs = mutableListOf<Msg>()
        handler.subscribe(this) { msgs += it }
        handler.runEffect(DatasourceEffect.LoadAvailableComponentTypes(1L)) { msgs += it }
        advanceUntilIdle()
        assertTrue("Cancellation НЕ превращается в Failed", msgs.none { it is Msg.ComponentTypesLoadFailed })
        handler.unsubscribe()
    }

    @Test
    fun `resubscribe cancels previous job`() = runTest {
        val d1 = MutableSharedFlow<AvailableComponents>(replay = 0)
        val d2 = MutableSharedFlow<AvailableComponents>(replay = 0)
        val handler = AvailableComponentTypesFlowHandler(
            fake { dictId -> if (dictId == 1L) d1 else d2 },
            NoopLogger,
        )
        val msgs = mutableListOf<Msg>()
        handler.subscribe(this) { msgs += it }

        handler.runEffect(DatasourceEffect.LoadAvailableComponentTypes(1L)) { msgs += it }
        d1.emit(AvailableComponents(listOf(tr))); advanceUntilIdle()

        handler.runEffect(DatasourceEffect.LoadAvailableComponentTypes(2L)) { msgs += it }
        d1.emit(AvailableComponents(listOf(ex))); advanceUntilIdle()   // старый job отменён → НЕ получен
        d2.emit(AvailableComponents(listOf(tr, ex))); advanceUntilIdle()

        val loaded = msgs.filterIsInstance<Msg.ComponentTypesLoaded>()
        assertEquals("эмиссия на d1 после переподписки проигнорирована", listOf(listOf(tr), listOf(tr, ex)), loaded.map { it.available.types })
        handler.unsubscribe()
    }
}
