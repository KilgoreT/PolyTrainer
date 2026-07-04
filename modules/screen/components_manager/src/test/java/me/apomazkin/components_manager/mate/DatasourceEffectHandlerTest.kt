@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package me.apomazkin.components_manager.mate

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.RenameOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.logger.LexemeLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Unit tests для [DatasourceEffectHandler] (CM Mate).
 *
 * Покрывает все 4 Effect ветки + exception path для каждой + F125 CancellationException
 * propagation.
 */
class DatasourceEffectHandlerTest {

    private val useCase = mockk<ComponentsManagerUseCase>()
    private val logger = mockk<LexemeLogger>(relaxed = true)
    private val dictionariesFlowHandler = mockk<DictionariesFlowHandler>(relaxed = true)
    private val handler = DatasourceEffectHandler(useCase, dictionariesFlowHandler, logger)

    private val now = Date(0L)

    private val ctype = ComponentType(
        id = ComponentTypeId(1L),
        systemKey = null,
        dictionaryId = null,
        name = "Notes",
        template = ComponentTemplate.TEXT,
        position = 0,
        createdAt = now,
        updatedAt = now,
    )

    private suspend fun run(effect: DatasourceEffect): Msg {
        var captured: Msg = Msg.Empty
        handler.runEffect(effect) { captured = it }
        return captured
    }

    // ===== CreateComponent =====

    @Test
    fun `CreateComponent useCase returns Success - emits CreateResult with epochId`() = runTest {
        coEvery {
            useCase.createUserDefinedComponent(any(), any(), any(), any())
        } returns CreateOutcome.Success(listOf(ctype))

        val msg = run(
            DatasourceEffect.CreateComponent(
                epochId = 7L,
                name = "Notes",
                template = ComponentTemplate.TEXT,
                isMultiple = false,
                scope = Scope.Global,
            )
        )

        assertTrue(msg is Msg.CreateResult)
        val res = msg as Msg.CreateResult
        assertEquals(7L, res.epochId)
        assertTrue(res.outcome is CreateOutcome.Success)
    }

    @Test
    fun `CreateComponent useCase returns SameScopeCollision - emits CreateResult`() = runTest {
        coEvery {
            useCase.createUserDefinedComponent(any(), any(), any(), any())
        } returns CreateOutcome.SameScopeCollision

        val msg = run(
            DatasourceEffect.CreateComponent(1L, "Notes", ComponentTemplate.TEXT, false, Scope.Global)
        )

        assertTrue(msg is Msg.CreateResult)
        assertEquals(CreateOutcome.SameScopeCollision, (msg as Msg.CreateResult).outcome)
    }

    @Test
    fun `CreateComponent useCase throws - emits Failure outcome with epochId, logged`() = runTest {
        val boom = RuntimeException("boom")
        coEvery {
            useCase.createUserDefinedComponent(any(), any(), any(), any())
        } throws boom

        val msg = run(
            DatasourceEffect.CreateComponent(3L, "Notes", ComponentTemplate.TEXT, false, Scope.Global)
        )

        assertTrue(msg is Msg.CreateResult)
        val res = msg as Msg.CreateResult
        assertEquals(3L, res.epochId)
        assertTrue(res.outcome is CreateOutcome.Failure)
        assertEquals(boom, (res.outcome as CreateOutcome.Failure).cause)
        coVerify { logger.e(any(), any()) }
    }

    @Test
    fun `CreateComponent useCase throws CancellationException - re-thrown (F125)`() {
        coEvery {
            useCase.createUserDefinedComponent(any(), any(), any(), any())
        } throws CancellationException("cancelled")

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                run(DatasourceEffect.CreateComponent(1L, "Notes", ComponentTemplate.TEXT, false, Scope.Global))
            }
        }
    }

    // ===== RenameComponent =====

    @Test
    fun `RenameComponent useCase returns Success - emits RenameResult with epochId`() = runTest {
        coEvery {
            useCase.renameComponent(ComponentTypeId(1L), "Y")
        } returns RenameOutcome.Success(ctype.copy(name = "Y"))

        val msg = run(DatasourceEffect.RenameComponent(epochId = 9L, typeId = ComponentTypeId(1L), newName = "Y"))

        assertTrue(msg is Msg.RenameResult)
        val res = msg as Msg.RenameResult
        assertEquals(9L, res.epochId)
        assertTrue(res.outcome is RenameOutcome.Success)
    }

    @Test
    fun `RenameComponent useCase throws - emits Failure outcome, logged`() = runTest {
        coEvery {
            useCase.renameComponent(any(), any())
        } throws RuntimeException("boom")

        val msg = run(DatasourceEffect.RenameComponent(epochId = 9L, typeId = ComponentTypeId(1L), newName = "Y"))

        assertTrue(msg is Msg.RenameResult)
        assertTrue((msg as Msg.RenameResult).outcome is RenameOutcome.Failure)
    }

    @Test
    fun `RenameComponent useCase throws CancellationException - re-thrown (F125)`() {
        coEvery {
            useCase.renameComponent(any(), any())
        } throws CancellationException("cancelled")

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                run(DatasourceEffect.RenameComponent(1L, ComponentTypeId(1L), "Y"))
            }
        }
    }

    // ===== LoadImpact =====

    @Test
    fun `LoadImpact useCase returns impact - emits ImpactPreviewLoaded with typeId`() = runTest {
        val impact = DeletionImpact(
            valueCount = 3,
            dictionariesWithValues = emptyList(),
            affectedQuizConfigs = emptyList(),
            affectedPrefs = emptyList(),
        )
        coEvery { useCase.previewDeletionImpact(ComponentTypeId(1L)) } returns impact

        val msg = run(DatasourceEffect.LoadImpact(ComponentTypeId(1L)))

        assertTrue(msg is Msg.ImpactPreviewLoaded)
        val res = msg as Msg.ImpactPreviewLoaded
        assertEquals(ComponentTypeId(1L), res.typeId)
        assertEquals(impact, res.impact)
    }

    // F145: previewDeletionImpact returns null → Msg.ImpactPreviewFailed dispatched
    // с `cause == null` (НЕ synthetic IllegalStateException).
    @Test
    fun `LoadImpact useCase returns null - emits ImpactPreviewFailed with null cause (no synthetic exception)`() = runTest {
        coEvery { useCase.previewDeletionImpact(any()) } returns null

        val msg = run(DatasourceEffect.LoadImpact(ComponentTypeId(1L)))

        assertTrue(msg is Msg.ImpactPreviewFailed)
        val failed = msg as Msg.ImpactPreviewFailed
        assertEquals(ComponentTypeId(1L), failed.typeId)
        assertNull("F145: null-return path must not wrap synthetic exception", failed.cause)
    }

    // F145: exception path emits ImpactPreviewFailed with the real cause (non-null).
    @Test
    fun `LoadImpact useCase throws - emits ImpactPreviewFailed with original cause`() = runTest {
        val ex = RuntimeException("boom")
        coEvery { useCase.previewDeletionImpact(any()) } throws ex

        val msg = run(DatasourceEffect.LoadImpact(ComponentTypeId(1L)))

        assertTrue(msg is Msg.ImpactPreviewFailed)
        val failed = msg as Msg.ImpactPreviewFailed
        assertEquals(ComponentTypeId(1L), failed.typeId)
        assertNotNull("exception path must carry real cause", failed.cause)
        assertEquals(ex, failed.cause)
    }

    // F146: CancellationException на previewDeletionImpact должен re-throw,
    // а не оборачиваться в Msg.ImpactPreviewFailed (parity с CreateComponent/RenameComponent).
    @Test
    fun `LoadImpact useCase throws CancellationException - re-thrown (F146)`() {
        coEvery {
            useCase.previewDeletionImpact(any())
        } throws CancellationException("cancelled")

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                run(DatasourceEffect.LoadImpact(ComponentTypeId(1L)))
            }
        }
    }

    // ===== SoftDeleteComponent =====

    @Test
    fun `SoftDeleteComponent useCase returns Success - emits DeleteResult with epochId`() = runTest {
        val impact = DeletionImpact(
            valueCount = 0,
            dictionariesWithValues = emptyList(),
            affectedQuizConfigs = emptyList(),
            affectedPrefs = emptyList(),
        )
        coEvery {
            useCase.softDeleteComponent(ComponentTypeId(1L))
        } returns DeleteOutcome.Success(impact)

        val msg = run(DatasourceEffect.SoftDeleteComponent(epochId = 11L, typeId = ComponentTypeId(1L)))

        assertTrue(msg is Msg.DeleteResult)
        val res = msg as Msg.DeleteResult
        assertEquals(11L, res.epochId)
        assertTrue(res.outcome is DeleteOutcome.Success)
    }

    @Test
    fun `SoftDeleteComponent useCase throws - emits Failure outcome`() = runTest {
        coEvery { useCase.softDeleteComponent(any()) } throws RuntimeException("boom")

        val msg = run(DatasourceEffect.SoftDeleteComponent(epochId = 11L, typeId = ComponentTypeId(1L)))

        assertTrue(msg is Msg.DeleteResult)
        assertTrue((msg as Msg.DeleteResult).outcome is DeleteOutcome.Failure)
    }

    // F146: CancellationException на softDeleteComponent должен re-throw,
    // а не оборачиваться в DeleteOutcome.Failure (parity с CreateComponent/RenameComponent).
    @Test
    fun `SoftDeleteComponent useCase throws CancellationException - re-thrown (F146)`() {
        coEvery {
            useCase.softDeleteComponent(any())
        } throws CancellationException("cancelled")

        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking {
                run(DatasourceEffect.SoftDeleteComponent(epochId = 11L, typeId = ComponentTypeId(1L)))
            }
        }
    }
}
