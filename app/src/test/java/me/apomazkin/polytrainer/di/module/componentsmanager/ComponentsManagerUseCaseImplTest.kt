package me.apomazkin.polytrainer.di.module.componentsmanager

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.CreateComponentOutcome
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_db_api.entity.EditComponentOutcome
import me.apomazkin.core_db_api.entity.SoftDeleteComponentOutcome
import me.apomazkin.core_db_api.entity.UserDefinedTypesUsageSnapshot
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.CreateOutcome
import me.apomazkin.lexeme.DeleteOutcome
import me.apomazkin.lexeme.DeletionImpact
import me.apomazkin.lexeme.EditOutcome
import me.apomazkin.lexeme.Scope
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.prefs.quizPickerPrefKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.sql.SQLException
import java.util.Date

/**
 * Unit tests для `ComponentsManagerUseCaseImpl` (business_test.md § 1).
 *
 * Coverage:
 * - 1.1 flowAllUserDefinedTypes (3 теста)
 * - 1.2 createUserDefinedComponent (6 тестов)
 * - 1.4 previewDeletionImpact (3 теста)
 * - 1.5 softDeleteComponent (5 тестов, включая 1.5.6 orphan prefs)
 */
class ComponentsManagerUseCaseImplTest {

    private val lexemeApi = mockk<CoreDbApi.LexemeApi>()
    private val dictionaryApi = mockk<CoreDbApi.DictionaryApi>()
    private val prefsProvider = mockk<PrefsProvider>(relaxed = true)
    private val logger = mockk<LexemeLogger>(relaxed = true)

    private val useCase = ComponentsManagerUseCaseImpl(
        lexemeApi = lexemeApi,
        dictionaryApi = dictionaryApi,
        prefsProvider = prefsProvider,
        logger = logger,
    )

    private val createdAt = Date(1_700_000_000_000L)
    private val updatedAt = Date(1_700_000_000_000L)

    private fun apiEntity(
        id: Long = 1L,
        name: String? = "Definition",
        dictionaryId: Long? = null,
    ) = ComponentTypeApiEntity(
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
    // 1.1 flowAllUserDefinedTypes
    // =========================================================================

    @Test
    fun `flowAllUserDefinedTypes empty snapshot maps to empty domain types`() = runTest {
        coEvery { lexemeApi.flowAllUserDefinedTypesWithUsage() } returns flowOf(
            UserDefinedTypesUsageSnapshot(
                types = emptyList(),
                valueCountByType = emptyMap(),
                dictionaryIdsByType = emptyMap(),
                dictionaryNames = emptyMap(),
            )
        )

        val result = useCase.flowAllUserDefinedTypes().toList()

        assertEquals(1, result.size)
        assertTrue(result[0].types.isEmpty())
        assertTrue(result[0].usage.valueCountByType.isEmpty())
        assertTrue(result[0].usage.dictionaryIdsByType.isEmpty())
        assertTrue(result[0].usage.dictionaryNames.isEmpty())
    }

    @Test
    fun `flowAllUserDefinedTypes maps types via toDomain`() = runTest {
        coEvery { lexemeApi.flowAllUserDefinedTypesWithUsage() } returns flowOf(
            UserDefinedTypesUsageSnapshot(
                types = listOf(apiEntity(id = 5L, name = "Pronunciation")),
                valueCountByType = mapOf(5L to 3),
                dictionaryIdsByType = mapOf(5L to setOf(7L)),
                dictionaryNames = mapOf(7L to "Spanish"),
            )
        )

        val result = useCase.flowAllUserDefinedTypes().toList().first()

        assertEquals(1, result.types.size)
        assertEquals(ComponentTypeId(5L), result.types[0].id)
        assertEquals("Pronunciation", result.types[0].name)
    }

    @Test
    fun `flowAllUserDefinedTypes wraps Long keys as ComponentTypeId`() = runTest {
        coEvery { lexemeApi.flowAllUserDefinedTypesWithUsage() } returns flowOf(
            UserDefinedTypesUsageSnapshot(
                types = listOf(apiEntity(id = 5L), apiEntity(id = 6L)),
                valueCountByType = mapOf(5L to 2, 6L to 4),
                dictionaryIdsByType = mapOf(5L to setOf(1L, 2L), 6L to setOf(3L)),
                dictionaryNames = mapOf(1L to "A", 2L to "B", 3L to "C"),
            )
        )

        val result = useCase.flowAllUserDefinedTypes().toList().first()

        assertEquals(2, result.usage.valueCountByType[ComponentTypeId(5L)])
        assertEquals(4, result.usage.valueCountByType[ComponentTypeId(6L)])
        assertEquals(setOf(1L, 2L), result.usage.dictionaryIdsByType[ComponentTypeId(5L)])
        assertEquals("A", result.usage.dictionaryNames[1L])
    }

    // =========================================================================
    // 1.2 createUserDefinedComponent
    // =========================================================================

    @Test
    fun `createUserDefinedComponent blank name returns NameEmpty without api call`() = runTest {
        val result = useCase.createUserDefinedComponent(
            name = "",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            scope = Scope.Global,
        )

        assertEquals(CreateOutcome.NameEmpty, result)
        coVerify(exactly = 0) { lexemeApi.createUserDefinedComponent(any(), any(), any(), any()) }
    }

    @Test
    fun `createUserDefinedComponent success returns mapped list`() = runTest {
        coEvery {
            lexemeApi.createUserDefinedComponent("Definition", ComponentTemplate.TEXT, false, Scope.Global)
        } returns CreateComponentOutcome.Success(listOf(apiEntity(id = 10L)))

        val result = useCase.createUserDefinedComponent(
            name = "Definition",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            scope = Scope.Global,
        )

        assertTrue(result is CreateOutcome.Success)
        val success = result as CreateOutcome.Success
        assertEquals(1, success.created.size)
        assertEquals(ComponentTypeId(10L), success.created[0].id)
    }

    @Test
    fun `createUserDefinedComponent multi-dict scope returns N rows`() = runTest {
        val scope = Scope.PerDictionaries(listOf(1L, 2L, 3L))
        coEvery {
            lexemeApi.createUserDefinedComponent("Quote", ComponentTemplate.TEXT, false, scope)
        } returns CreateComponentOutcome.Success(
            listOf(
                apiEntity(id = 11L, dictionaryId = 1L),
                apiEntity(id = 12L, dictionaryId = 2L),
                apiEntity(id = 13L, dictionaryId = 3L),
            )
        )

        val result = useCase.createUserDefinedComponent(
            name = "Quote",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            scope = scope,
        )

        assertTrue(result is CreateOutcome.Success)
        assertEquals(3, (result as CreateOutcome.Success).created.size)
    }

    @Test
    fun `createUserDefinedComponent SameScopeCollision passes through`() = runTest {
        coEvery {
            lexemeApi.createUserDefinedComponent(any(), any(), any(), any())
        } returns CreateComponentOutcome.SameScopeCollision

        val result = useCase.createUserDefinedComponent(
            name = "Definition",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            scope = Scope.Global,
        )

        assertEquals(CreateOutcome.SameScopeCollision, result)
    }

    @Test
    fun `createUserDefinedComponent CrossScopeCollision passes through`() = runTest {
        coEvery {
            lexemeApi.createUserDefinedComponent(any(), any(), any(), any())
        } returns CreateComponentOutcome.CrossScopeCollision

        val result = useCase.createUserDefinedComponent(
            name = "Definition",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            scope = Scope.PerDictionaries(listOf(1L)),
        )

        assertEquals(CreateOutcome.CrossScopeCollision, result)
    }

    @Test
    fun `createUserDefinedComponent api throws returns Failure with cause`() = runTest {
        val sqlEx = SQLException("FK violation")
        coEvery {
            lexemeApi.createUserDefinedComponent(any(), any(), any(), any())
        } throws sqlEx

        val result = useCase.createUserDefinedComponent(
            name = "Definition",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            scope = Scope.Global,
        )

        assertTrue(result is CreateOutcome.Failure)
        assertSame(sqlEx, (result as CreateOutcome.Failure).cause)
        coVerify(exactly = 1) { logger.e(any(), any()) }
    }

    @Test
    fun `createUserDefinedComponent trims whitespace from name`() = runTest {
        coEvery {
            lexemeApi.createUserDefinedComponent("Definition", any(), any(), any())
        } returns CreateComponentOutcome.Success(listOf(apiEntity()))

        useCase.createUserDefinedComponent(
            name = "  Definition  ",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
            scope = Scope.Global,
        )

        coVerify(exactly = 1) {
            lexemeApi.createUserDefinedComponent(
                name = "Definition",
                template = ComponentTemplate.TEXT,
                isMultiple = false,
                scope = Scope.Global,
            )
        }
    }

    // =========================================================================
    // 1.4 previewDeletionImpact
    // =========================================================================

    @Test
    fun `previewDeletionImpact success returns impact`() = runTest {
        val impact = DeletionImpact(
            valueCount = 5,
            dictionariesWithValues = listOf(7L),
            affectedQuizConfigs = emptyList(),
            affectedPrefs = emptyList(),
        )
        coEvery { lexemeApi.previewDeletionImpact(1L) } returns impact

        val result = useCase.previewDeletionImpact(ComponentTypeId(1L))

        assertSame(impact, result)
    }

    @Test
    fun `previewDeletionImpact null returns null`() = runTest {
        coEvery { lexemeApi.previewDeletionImpact(1L) } returns null

        val result = useCase.previewDeletionImpact(ComponentTypeId(1L))

        assertNull(result)
    }

    @Test
    fun `previewDeletionImpact api throws returns null and logs`() = runTest {
        coEvery { lexemeApi.previewDeletionImpact(any()) } throws SQLException("query failed")

        val result = useCase.previewDeletionImpact(ComponentTypeId(1L))

        assertNull(result)
        coVerify(exactly = 1) { logger.e(any(), any()) }
    }

    // =========================================================================
    // 1.5 softDeleteComponent
    // =========================================================================

    @Test
    fun `softDeleteComponent success with no affected prefs writes none`() = runTest {
        val impact = DeletionImpact(
            valueCount = 0,
            dictionariesWithValues = emptyList(),
            affectedQuizConfigs = emptyList(),
            affectedPrefs = emptyList(),
        )
        coEvery {
            lexemeApi.softDeleteComponentType(1L)
        } returns SoftDeleteComponentOutcome.Success(impact)

        val result = useCase.softDeleteComponent(ComponentTypeId(1L))

        assertTrue(result is DeleteOutcome.Success)
        assertSame(impact, (result as DeleteOutcome.Success).impact)
        coVerify(exactly = 0) { prefsProvider.setStringByRawKey(any(), any()) }
    }

    @Test
    fun `softDeleteComponent success with affected prefs resets each via quizPickerPrefKey`() = runTest {
        val impact = DeletionImpact(
            valueCount = 0,
            dictionariesWithValues = emptyList(),
            affectedQuizConfigs = emptyList(),
            affectedPrefs = listOf(1L, 5L),
        )
        coEvery {
            lexemeApi.softDeleteComponentType(2L)
        } returns SoftDeleteComponentOutcome.Success(impact)
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } just Runs

        val result = useCase.softDeleteComponent(ComponentTypeId(2L))

        assertTrue(result is DeleteOutcome.Success)
        coVerify(exactly = 1) { prefsProvider.setStringByRawKey(quizPickerPrefKey(1L), null) }
        coVerify(exactly = 1) { prefsProvider.setStringByRawKey(quizPickerPrefKey(5L), null) }
    }

    @Test
    fun `softDeleteComponent BuiltInProtected returns protected and writes no prefs`() = runTest {
        coEvery {
            lexemeApi.softDeleteComponentType(1L)
        } returns SoftDeleteComponentOutcome.BuiltInProtected

        val result = useCase.softDeleteComponent(ComponentTypeId(1L))

        assertEquals(DeleteOutcome.BuiltInProtected, result)
        coVerify(exactly = 0) { prefsProvider.setStringByRawKey(any(), any()) }
    }

    @Test
    fun `softDeleteComponent api throws returns Failure and logs`() = runTest {
        val ex = SQLException("transaction")
        coEvery { lexemeApi.softDeleteComponentType(any()) } throws ex

        val result = useCase.softDeleteComponent(ComponentTypeId(1L))

        assertTrue(result is DeleteOutcome.Failure)
        assertSame(ex, (result as DeleteOutcome.Failure).cause)
        coVerify(exactly = 1) { logger.e(any(), any()) }
    }

    // 1.5.6 — F103 best-effort orphan prefs
    @Test
    fun `softDeleteComponent prefs reset throws still returns Success with warning log`() = runTest {
        // DB soft-delete succeeded, but prefs reset throws on each pref.
        val impact = DeletionImpact(
            valueCount = 0,
            dictionariesWithValues = emptyList(),
            affectedQuizConfigs = emptyList(),
            affectedPrefs = listOf(1L, 5L),
        )
        coEvery {
            lexemeApi.softDeleteComponentType(3L)
        } returns SoftDeleteComponentOutcome.Success(impact)
        coEvery { prefsProvider.setStringByRawKey(any(), any()) } throws RuntimeException("datastore IO")

        val result = useCase.softDeleteComponent(ComponentTypeId(3L))

        // Best-effort: outcome остаётся Success, prefs failure НЕ propagate.
        assertTrue("orphan prefs reset failure не ломает overall Success", result is DeleteOutcome.Success)
        assertSame(impact, (result as DeleteOutcome.Success).impact)
        // Warning logged per pref (2 раза).
        coVerify(atLeast = 2) { logger.w(any(), any()) }
    }

    // =========================================================================
    // ===== PHASE 2 ============================================================
    // =========================================================================

    // =========================================================================
    // P2.1 editComponent — validations & happy path
    // =========================================================================

    @Test
    fun `editComponent_emptyName_returnsNameEmpty_withoutApiCall`() = runTest {
        val result = useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "   ",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
        )

        assertEquals(EditOutcome.NameEmpty, result)
        coVerify(exactly = 0) { lexemeApi.editComponentType(any(), any(), any(), any()) }
    }

    @Test
    fun `editComponent_apiSuccess_returnsDomainSuccess_andMapsEntity`() = runTest {
        coEvery {
            lexemeApi.editComponentType(1L, "Quote", ComponentTemplate.TEXT, false)
        } returns EditComponentOutcome.Success(apiEntity(id = 1L, name = "Quote"))

        val result = useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "Quote",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
        )

        assertTrue(result is EditOutcome.Success)
        assertEquals("Quote", (result as EditOutcome.Success).updated.name)
        assertEquals(ComponentTypeId(1L), result.updated.id)
    }

    @Test
    fun `editComponent_apiSameScopeCollision_returnsDomain`() = runTest {
        coEvery {
            lexemeApi.editComponentType(any(), any(), any(), any())
        } returns EditComponentOutcome.SameScopeCollision

        val result = useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "Annotations",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
        )

        assertEquals(EditOutcome.SameScopeCollision, result)
    }

    @Test
    fun `editComponent_apiCrossScopeCollision_returnsDomain`() = runTest {
        coEvery {
            lexemeApi.editComponentType(any(), any(), any(), any())
        } returns EditComponentOutcome.CrossScopeCollision

        val result = useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "Annotations",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
        )

        assertEquals(EditOutcome.CrossScopeCollision, result)
    }

    @Test
    fun `editComponent_apiCardinalityDowngradeBlocked_returnsDomainWithIds`() = runTest {
        val ids = listOf(10L, 20L, 30L, 40L)
        coEvery {
            lexemeApi.editComponentType(any(), any(), any(), any())
        } returns EditComponentOutcome.CardinalityDowngradeBlocked(impactedLexemeIds = ids)

        val result = useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "Notes",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
        )

        assertTrue(result is EditOutcome.CardinalityDowngradeBlocked)
        assertEquals(ids, (result as EditOutcome.CardinalityDowngradeBlocked).impactedLexemeIds)
    }

    @Test
    fun `editComponent_apiTemplateImmutable_returnsDomain`() = runTest {
        coEvery {
            lexemeApi.editComponentType(any(), any(), any(), any())
        } returns EditComponentOutcome.TemplateImmutable

        val result = useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "Notes",
            template = ComponentTemplate.IMAGE,
            isMultiple = false,
        )

        assertEquals(EditOutcome.TemplateImmutable, result)
    }

    @Test
    fun `editComponent_apiBuiltInProtected_returnsDomain`() = runTest {
        coEvery {
            lexemeApi.editComponentType(any(), any(), any(), any())
        } returns EditComponentOutcome.BuiltInProtected

        val result = useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "Translation2",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
        )

        assertEquals(EditOutcome.BuiltInProtected, result)
    }

    @Test
    fun `whenSoftDeleteApiReturnsRemoved_thenDomainDeleteOutcomeRemoved`() = runTest {
        coEvery {
            lexemeApi.softDeleteComponentType(1L)
        } returns SoftDeleteComponentOutcome.Removed

        val result = useCase.softDeleteComponent(ComponentTypeId(1L))

        assertEquals(DeleteOutcome.Removed, result)
    }

    @Test
    fun `editComponent_apiRemoved_returnsDomainRemoved`() = runTest {
        coEvery {
            lexemeApi.editComponentType(any(), any(), any(), any())
        } returns EditComponentOutcome.Removed

        val result = useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "Notes",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
        )

        assertEquals(EditOutcome.Removed, result)
    }

    @Test
    fun `editComponent_cancellationException_rethrows`() = runTest {
        coEvery {
            lexemeApi.editComponentType(any(), any(), any(), any())
        } throws CancellationException("cancel")

        try {
            useCase.editComponent(
                typeId = ComponentTypeId(1L),
                name = "Notes",
                template = ComponentTemplate.TEXT,
                isMultiple = false,
            )
            fail("CancellationException must propagate, not be wrapped in Failure")
        } catch (e: CancellationException) {
            // expected
        }
    }

    @Test
    fun `editComponent_genericException_returnsFailure_andLogs`() = runTest {
        val ex = SQLException("constraint")
        coEvery {
            lexemeApi.editComponentType(any(), any(), any(), any())
        } throws ex

        val result = useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "Notes",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
        )

        assertTrue(result is EditOutcome.Failure)
        assertSame(ex, (result as EditOutcome.Failure).cause)
        coVerify(exactly = 1) { logger.e(any(), any()) }
    }

    @Test
    fun `editComponent_trimsWhitespaceFromName`() = runTest {
        coEvery {
            lexemeApi.editComponentType(1L, "Notes", any(), any())
        } returns EditComponentOutcome.Success(apiEntity(id = 1L, name = "Notes"))

        useCase.editComponent(
            typeId = ComponentTypeId(1L),
            name = "  Notes  ",
            template = ComponentTemplate.TEXT,
            isMultiple = false,
        )

        coVerify(exactly = 1) {
            lexemeApi.editComponentType(
                typeId = 1L,
                name = "Notes",
                template = ComponentTemplate.TEXT,
                isMultiple = false,
            )
        }
    }

    // =========================================================================
    // P2.2 flowDictionaries — delegation
    // =========================================================================

    @Test
    fun `flowDictionaries_delegatesToDictionaryApi`() = runTest {
        val list = listOf(
            DictionaryApiEntity(id = 1L, numericCode = null, name = "EN", addDate = Date(0L)),
            DictionaryApiEntity(id = 2L, numericCode = null, name = "DE", addDate = Date(0L)),
        )
        coEvery { dictionaryApi.flowDictionaryList() } returns flowOf(list)

        val result = useCase.flowDictionaries().toList()

        assertEquals(1, result.size)
        assertSame(list, result.first())
        coVerify(exactly = 1) { dictionaryApi.flowDictionaryList() }
    }
}
