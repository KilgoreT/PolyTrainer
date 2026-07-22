package me.apomazkin.polytrainer.di.module.perdictionarycomponents

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.DictionaryTypesSnapshot
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.components_manager.deps.ComponentsManagerUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Unit tests для `PerDictionaryComponentsUseCaseImpl` (business_test.md § 2).
 *
 * Coverage:
 * - 2.1 flowComponentsForDictionary (2 теста)
 * - 2.2 Delegation sanity-check (4 теста)
 *
 * F126 retrofit: `sharedCrud` теперь interface-type параметр; mock'аем `ComponentsManagerUseCase`.
 */
class PerDictionaryComponentsUseCaseImplTest {

    private val lexemeApi = mockk<CoreDbApi.LexemeApi>()
    private val sharedCrud = mockk<ComponentsManagerUseCase>()

    private val useCase = PerDictionaryComponentsUseCaseImpl(
        lexemeApi = lexemeApi,
        sharedCrud = sharedCrud,
        logger = object : me.apomazkin.logger.LexemeLogger {
            override fun log(
                level: me.apomazkin.logger.LogLevel,
                tag: String,
                message: String,
                throwable: Throwable?,
            ) {}
        },
    )

    private val createdAt = Date(1_700_000_000_000L)
    private val updatedAt = Date(1_700_000_000_000L)

    private fun apiEntity(id: Long, name: String = "Definition", dictionaryId: Long? = 7L) =
        ComponentTypeApiEntity(
            id = id,
            systemKey = null,
            dictionaryId = dictionaryId,
            name = name,
            template = ComponentTemplate.TEXT,
            position = 0,
            isMultiple = false,
            createdAt = createdAt,
            updatedAt = updatedAt,
            removedAt = null,
        )

    // =========================================================================
    // 2.1 flowComponentsForDictionary
    // =========================================================================

    @Test
    fun `flowComponentsForDictionary maps snapshot with ComponentTypeId-wrapped keys`() = runTest {
        coEvery {
            lexemeApi.flowUserDefinedTypesForDictionary(7L)
        } returns flowOf(
            DictionaryTypesSnapshot(
                dictionaryId = 7L,
                dictionaryName = "Spanish",
                types = listOf(apiEntity(id = 1L, name = "Definition"), apiEntity(id = 2L, name = "Pronunciation")),
                valueCountByType = mapOf(1L to 3, 2L to 1),
            )
        )

        val result = useCase.flowComponentsForDictionary(7L).toList().first()

        assertEquals(7L, result.dictionaryId)
        assertEquals("Spanish", result.dictionaryName)
        assertEquals(2, result.types.size)
        assertEquals(ComponentTypeId(1L), result.types[0].id)
        assertEquals(3, result.valueCountByType[ComponentTypeId(1L)])
        assertEquals(1, result.valueCountByType[ComponentTypeId(2L)])
    }

    @Test
    fun `flowComponentsForDictionary empty snapshot emits empty types`() = runTest {
        coEvery {
            lexemeApi.flowUserDefinedTypesForDictionary(7L)
        } returns flowOf(
            DictionaryTypesSnapshot(
                dictionaryId = 7L,
                dictionaryName = "Spanish",
                types = emptyList(),
                valueCountByType = emptyMap(),
            )
        )

        val result = useCase.flowComponentsForDictionary(7L).toList().first()

        assertTrue(result.types.isEmpty())
        assertTrue(result.valueCountByType.isEmpty())
    }

    // =========================================================================
    // 2.2 Delegation sanity-check
    // =========================================================================

    @Test
    fun `createUserDefinedComponent delegates to sharedCrud`() = runTest {
        val scope = Scope.PerDictionaries(listOf(7L))
        coEvery {
            sharedCrud.createUserDefinedComponent("Quote", ComponentTemplate.TEXT, false, scope)
        } returns CreateOutcome.NameEmpty

        val result = useCase.createUserDefinedComponent(
            name = "Quote",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            scope = scope,
        )

        assertEquals(CreateOutcome.NameEmpty, result)
        coVerify(exactly = 1) {
            sharedCrud.createUserDefinedComponent("Quote", ComponentTemplate.TEXT, false, scope)
        }
    }

    @Test
    fun `previewDeletionImpact delegates to sharedCrud`() = runTest {
        val impact = DeletionImpact(
            valueCount = 2,
            dictionariesWithValues = listOf(7L),
            affectedQuizConfigs = emptyList(),
            affectedPrefs = emptyList(),
        )
        coEvery { sharedCrud.previewDeletionImpact(ComponentTypeId(1L)) } returns impact

        val result = useCase.previewDeletionImpact(ComponentTypeId(1L))

        assertSame(impact, result)
        coVerify(exactly = 1) { sharedCrud.previewDeletionImpact(ComponentTypeId(1L)) }
    }

    @Test
    fun `softDeleteComponent delegates to sharedCrud`() = runTest {
        coEvery {
            sharedCrud.softDeleteComponent(ComponentTypeId(1L))
        } returns DeleteOutcome.BuiltInProtected

        val result = useCase.softDeleteComponent(ComponentTypeId(1L))

        assertEquals(DeleteOutcome.BuiltInProtected, result)
        coVerify(exactly = 1) { sharedCrud.softDeleteComponent(ComponentTypeId(1L)) }
    }

    // =========================================================================
    // ===== PHASE 2 ============================================================
    // =========================================================================

    @Test
    fun `editComponent_delegatesToSharedCrud`() = runTest {
        coEvery {
            sharedCrud.editComponent(
                typeId = ComponentTypeId(1L),
                name = "Notes",
                template = ComponentTemplate.TEXT,
                isMultiple = true,
            )
        } returns EditOutcome.Success(
            updated = me.apomazkin.lexeme.ComponentType(
                id = ComponentTypeId(1L),
                systemKey = null,
                dictionaryId = 7L,
                name = "Notes",
                template = ComponentTemplate.TEXT,
                position = 0,
                isMultiple = true,
                createdAt = createdAt,
                updatedAt = updatedAt,
            ),
        )

        val result = useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "Notes",
            template = ComponentTemplate.TEXT,
            isMultiple = true,
        )

        assertTrue(result is EditOutcome.Success)
        coVerify(exactly = 1) {
            sharedCrud.editComponent(
                typeId = ComponentTypeId(1L),
                name = "Notes",
                template = ComponentTemplate.TEXT,
                isMultiple = true,
            )
        }
    }

    @Test
    fun `editComponent_apiReturnsRemoved_delegatesAndReturnsRemoved`() = runTest {
        coEvery {
            sharedCrud.editComponent(any(), any(), any(), any())
        } returns EditOutcome.Removed

        val result = useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "Notes",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
        )

        assertEquals(EditOutcome.Removed, result)
    }

    @Test
    fun `editComponent_apiReturnsTemplateImmutable_delegatesAndReturns`() = runTest {
        coEvery {
            sharedCrud.editComponent(any(), any(), any(), any())
        } returns EditOutcome.TemplateImmutable

        val result = useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "Notes",
            template = ComponentTemplate.IMAGE,
            isMultiple = false,
        )

        assertEquals(EditOutcome.TemplateImmutable, result)
    }

    @Test
    fun `editComponent_apiReturnsCardinalityDowngradeBlocked_delegatesAndReturns`() = runTest {
        val ids = listOf(10L, 20L)
        coEvery {
            sharedCrud.editComponent(any(), any(), any(), any())
        } returns EditOutcome.CardinalityDowngradeBlocked(impactedLexemeIds = ids)

        val result = useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "Notes",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
        )

        assertTrue(result is EditOutcome.CardinalityDowngradeBlocked)
        assertEquals(ids, (result as EditOutcome.CardinalityDowngradeBlocked).impactedLexemeIds)
    }
}
