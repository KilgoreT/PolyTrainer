package me.apomazkin.polytrainer.di.module.wordCard

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.ComponentTypeApiEntity
import me.apomazkin.core_db_api.entity.ComponentValueApiEntity
import me.apomazkin.core_db_api.entity.DictionaryApiEntity
import me.apomazkin.core_db_api.entity.LexemeApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.WordApiEntity
import me.apomazkin.flags.CountryProvider
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.lexeme.ComponentValueId
import me.apomazkin.lexeme.Primitive
import me.apomazkin.lexeme.TextValues
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.wordcard.deps.AddComponentValueResult
import me.apomazkin.wordcard.deps.RemoveComponentResult
import me.apomazkin.wordcard.deps.RemoveLexemeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * §4 WordCardUseCaseImpl (REWRITE, generic API) — T1–T18.
 * TDD red — против НОВЫХ методов UseCase (addLexemeWithComponent / addComponentValue→
 * AddComponentValueResult / updateComponentValue(cvId,lexemeId,data) / deleteComponentValue(cvId,lexemeId)→
 * RemoveComponentResult / restoreLexemeWithComponents / flowAvailableComponentTypes) и
 * нового LexemeApi.flowTypesForDictionary.
 */
class WordCardUseCaseImplTest {

    private val wordApi = mockk<CoreDbApi.WordApi>(relaxed = true)
    private val dictionaryApi = mockk<CoreDbApi.DictionaryApi>(relaxed = true)
    private val termApi = mockk<CoreDbApi.TermApi>(relaxed = true)
    private val lexemeApi = mockk<CoreDbApi.LexemeApi>(relaxed = true)
    private val prefsProvider = mockk<PrefsProvider>(relaxed = true)
    private val logger = mockk<LexemeLogger>(relaxed = true)
    private val countryProvider = mockk<CountryProvider>(relaxed = true)

    private val useCase = WordCardUseCaseImpl(
        wordApi = wordApi,
        dictionaryApi = dictionaryApi,
        termApi = termApi,
        lexemeApi = lexemeApi,
        prefsProvider = prefsProvider,
        logger = logger,
        countryProvider = countryProvider,
    )

    private val D0 = Date(0L)
    private val TR = ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)

    private fun typeEntity(id: Long, ref: ComponentTypeRef, isMultiple: Boolean = false) = ComponentTypeApiEntity(
        id = id,
        systemKey = (ref as? ComponentTypeRef.BuiltIn)?.key,
        dictionaryId = if (ref is ComponentTypeRef.BuiltIn) null else 3L,
        name = (ref as? ComponentTypeRef.UserDefined)?.name,
        template = ComponentTemplate.TEXT, position = 0, isMultiple = isMultiple,
        createdAt = D0, updatedAt = D0,
    )

    private fun cvEntity(id: Long, lexemeId: Long, text: String, typeId: Long = 50L, ref: ComponentTypeRef = TR) =
        ComponentValueApiEntity(
            id = id, lexemeId = lexemeId, type = typeEntity(typeId, ref),
            data = TextValues(Primitive.Text(text)), createdAt = D0, updatedAt = D0,
        )

    private fun lexemeEntity(id: Long, comps: List<ComponentValueApiEntity> = emptyList()) =
        LexemeApiEntity(id = id, components = comps, addDate = D0)

    private fun data(text: String) = TextValues(Primitive.Text(text))

    private fun termEntity(dictionaryId: Long = 3L) = TermApiEntity(
        word = WordApiEntity(id = 7L, dictionaryId = dictionaryId, value = "book", addDate = D0),
        lexemes = emptyList(),
    )

    /** IS485: getTermById резолвит флаг словаря (numericCode → flagRes через CountryProvider). */
    @Test
    fun `getTermById_resolves_dictionary_flag`() = runTest {
        coEvery { termApi.getTermById(7L) } returns termEntity(dictionaryId = 3L)
        coEvery { dictionaryApi.getDictionaryById(3L) } returns DictionaryApiEntity(
            id = 3L, numericCode = 840, name = "EN", addDate = D0,
        )
        every { countryProvider.getFlagRes(840) } returns 42

        val term = useCase.getTermById(7L)

        assertEquals(42, term?.dictionaryFlagRes)
    }

    /** IS485: словарь без numericCode (флаг не выбран) → dictionaryFlagRes = null, без падений. */
    @Test
    fun `getTermById_dictionary_without_flag_gives_null`() = runTest {
        coEvery { termApi.getTermById(7L) } returns termEntity(dictionaryId = 3L)
        coEvery { dictionaryApi.getDictionaryById(3L) } returns DictionaryApiEntity(
            id = 3L, numericCode = null, name = "EN", addDate = D0,
        )

        val term = useCase.getTermById(7L)

        assertNull(term?.dictionaryFlagRes)
        verify(exactly = 0) { countryProvider.getFlagRes(any()) }
    }

    // T1
    @Test
    fun `addLexemeWithComponent_happy_BuiltIn`() = runTest {
        coEvery { lexemeApi.addLexemeWithComponents(7L, 3L, any()) } returns 100L
        coEvery { lexemeApi.getLexemeById(100L) } returns lexemeEntity(100L, listOf(cvEntity(60L, 100L, "hi")))
        val result = useCase.addLexemeWithComponent(7L, 3L, TR, data("hi"))
        assertEquals(100L, result?.lexemeId?.id)
        coVerify { lexemeApi.addLexemeWithComponents(7L, 3L, match { it.single().first is ComponentTypeRef.BuiltIn }) }
    }

    // T2
    @Test
    fun `addLexemeWithComponent_UserDefined_branch`() = runTest {
        val ref = ComponentTypeRef.UserDefined("Example")
        coEvery { lexemeApi.addLexemeWithComponents(7L, 3L, any()) } returns 100L
        coEvery { lexemeApi.getLexemeById(100L) } returns lexemeEntity(100L)
        useCase.addLexemeWithComponent(7L, 3L, ref, data("e"))
        coVerify { lexemeApi.addLexemeWithComponents(7L, 3L, match { it.single().first is ComponentTypeRef.UserDefined }) }
    }

    // T3
    @Test
    fun `addLexemeWithComponent_null_type_not_found`() = runTest {
        coEvery { lexemeApi.addLexemeWithComponents(any(), any(), any()) } returns null
        val result = useCase.addLexemeWithComponent(7L, 3L, TR, data("x"))
        assertNull(result)
        coVerify(exactly = 0) { lexemeApi.getLexemeById(any()) }
    }

    // T4
    @Test
    fun `addComponentValue_returns_result_with_exact_newId`() = runTest {
        coEvery { lexemeApi.addComponentValue(100L, 50L, any()) } returns 60L
        coEvery { lexemeApi.getLexemeById(100L) } returns lexemeEntity(100L, listOf(cvEntity(60L, 100L, "v")))
        val result = useCase.addComponentValue(100L, ComponentTypeId(50L), data("v"))
        assertEquals(ComponentValueId(60L), result?.newComponentValueId)
        assertEquals(100L, result?.lexeme?.lexemeId?.id)
    }

    // T5
    @Test
    fun `addComponentValue_getLexemeById_null_returns_null`() = runTest {
        coEvery { lexemeApi.addComponentValue(any(), any(), any()) } returns 60L
        coEvery { lexemeApi.getLexemeById(any()) } returns null
        assertNull(useCase.addComponentValue(100L, ComponentTypeId(50L), data("v")))
    }

    // T6
    @Test
    fun `addComponentValue_exception_returns_null`() = runTest {
        coEvery { lexemeApi.addComponentValue(any(), any(), any()) } throws IllegalStateException("boom")
        assertNull(useCase.addComponentValue(100L, ComponentTypeId(50L), data("v")))
    }

    // T7
    @Test
    fun `updateComponentValue_happy_via_lexemeId_param`() = runTest {
        coEvery { lexemeApi.updateComponentValue(50L, any()) } returns 1
        coEvery { lexemeApi.getLexemeById(100L) } returns lexemeEntity(100L)
        val result = useCase.updateComponentValue(ComponentValueId(50L), 100L, data("new"))
        assertEquals(100L, result?.lexemeId?.id)
        coVerify { lexemeApi.getLexemeById(100L) }
    }

    // T8
    @Test
    fun `updateComponentValue_returns_0_returns_null`() = runTest {
        coEvery { lexemeApi.updateComponentValue(50L, any()) } returns 0
        assertNull(useCase.updateComponentValue(ComponentValueId(50L), 100L, data("new")))
        coVerify(exactly = 0) { lexemeApi.getLexemeById(any()) }
    }

    // T9
    @Test
    fun `updateComponentValue_softDeletedType_B1_5_caught_returns_null`() = runTest {
        coEvery { lexemeApi.updateComponentValue(50L, any()) } throws IllegalStateException("soft-deleted")
        assertNull(useCase.updateComponentValue(ComponentValueId(50L), 100L, data("new")))
    }

    // T10
    @Test
    fun `updateComponentValue_getLexemeById_null_returns_null`() = runTest {
        coEvery { lexemeApi.updateComponentValue(50L, any()) } returns 1
        coEvery { lexemeApi.getLexemeById(100L) } returns null
        assertNull(useCase.updateComponentValue(ComponentValueId(50L), 100L, data("new")))
    }

    // T11 (IS486 фаза 3: before-snapshot упразднён — delete → один after-reread)
    @Test
    fun `deleteComponentValue_ComponentRemoved_when_remaining`() = runTest {
        coEvery { lexemeApi.deleteComponentValue(50L) } returns 1
        coEvery { lexemeApi.getLexemeById(100L) } returns
            lexemeEntity(100L, listOf(cvEntity(61L, 100L, "rest")))
        val result = useCase.deleteComponentValue(ComponentValueId(50L), 100L)
        assertTrue(result is RemoveComponentResult.ComponentRemoved)
        assertEquals(1, (result as RemoveComponentResult.ComponentRemoved).lexeme.components.size)
        coVerify(exactly = 0) { lexemeApi.deleteLexeme(any()) }
    }

    // T13
    @Test
    fun `deleteComponentValue_works_without_removedAt_check`() = runTest {
        coEvery { lexemeApi.deleteComponentValue(50L) } returns 1
        coEvery { lexemeApi.getLexemeById(100L) } returns lexemeEntity(100L, listOf(cvEntity(61L, 100L, "rest")))
        val result = useCase.deleteComponentValue(ComponentValueId(50L), 100L)
        assertTrue("delete не валидирует removedAt типа — success", result is RemoveComponentResult.ComponentRemoved)
    }

    // T12 (IS486 фаза 3, В4): remaining == 0 больше НЕ убивает лексему —
    // она деградирует в черновик (ComponentRemoved с пустым списком компонентов).
    @Test
    fun `deleteComponentValue_zero_remaining_degrades_to_draft`() = runTest {
        coEvery { lexemeApi.deleteComponentValue(50L) } returns 0
        coEvery { lexemeApi.getLexemeById(100L) } returns lexemeEntity(100L, emptyList())
        val result = useCase.deleteComponentValue(ComponentValueId(50L), 100L)
        assertTrue(result is RemoveComponentResult.ComponentRemoved)
        val lexeme = (result as RemoveComponentResult.ComponentRemoved).lexeme
        assertEquals(100L, lexeme.lexemeId.id)
        assertTrue("лексема пуста — черновик", lexeme.components.isEmpty())
        coVerify(exactly = 0) { lexemeApi.deleteLexeme(any()) }
    }

    // T14 (IS486 фаза 3: before-snapshot упразднён — null возможен только на after-reread)
    @Test
    fun `deleteComponentValue_reread_null_returns_null`() = runTest {
        coEvery { lexemeApi.deleteComponentValue(50L) } returns 0
        coEvery { lexemeApi.getLexemeById(100L) } returns null
        assertNull(useCase.deleteComponentValue(ComponentValueId(50L), 100L))
    }

    // T15
    @Test
    fun `restoreLexemeWithComponents_maps_and_returns`() = runTest {
        val snapshot = me.apomazkin.lexeme.Lexeme(
            lexemeId = me.apomazkin.lexeme.LexemeId(8L),
            components = emptyList(),
            addDate = D0,
        )
        coEvery { lexemeApi.addLexemeWithComponents(7L, 3L, any()) } returns 900L
        coEvery { lexemeApi.getLexemeById(900L) } returns lexemeEntity(900L)
        assertEquals(900L, useCase.restoreLexemeWithComponents(7L, 3L, snapshot)?.lexemeId?.id)
    }

    // T15 _null
    @Test
    fun `restoreLexemeWithComponents_null_when_api_null`() = runTest {
        val snapshot = me.apomazkin.lexeme.Lexeme(
            lexemeId = me.apomazkin.lexeme.LexemeId(8L), components = emptyList(), addDate = D0,
        )
        coEvery { lexemeApi.addLexemeWithComponents(any(), any(), any()) } returns null
        assertNull(useCase.restoreLexemeWithComponents(7L, 3L, snapshot))
    }

    // T16 (IS486: контракт → AvailableComponents)
    @Test
    fun `flowAvailableComponentTypes_maps_domain`() = runTest {
        // IS486 (девайс-баг 2026-07-21): источник — единый реактивный снапшот.
        every { lexemeApi.flowUserDefinedTypesForDictionary(10L) } returns flowOf(
            me.apomazkin.core_db_api.entity.DictionaryTypesSnapshot(
                dictionaryId = 10L,
                dictionaryName = "ES",
                types = listOf(typeEntity(1L, TR), typeEntity(2L, ComponentTypeRef.UserDefined("Example"), isMultiple = true)),
                valueCountByType = emptyMap(),
            ),
        )
        val available = useCase.flowAvailableComponentTypes(10L).first()
        assertEquals(2, available.types.size)
        assertEquals(BuiltInComponent.TRANSLATION, available.types[0].systemKey)
        assertTrue(available.types[1].isMultiple)
        assertTrue(available.optionsByType.isEmpty())
    }

    // T17
    @Test
    fun `deleteLexeme_returns_Removed_snapshot`() = runTest {
        coEvery { lexemeApi.getLexemeById(8L) } returns lexemeEntity(8L, listOf(cvEntity(60L, 8L, "x")))
        coEvery { lexemeApi.deleteLexeme(8L) } returns 1
        val result = useCase.deleteLexeme(7L, 8L)
        assertTrue(result is RemoveLexemeResult.Removed)
    }

    // T18
    @Test
    fun `addComponentValue_trims_before_write`() = runTest {
        coEvery { lexemeApi.addComponentValue(100L, 50L, any()) } returns 60L
        coEvery { lexemeApi.getLexemeById(100L) } returns lexemeEntity(100L)
        useCase.addComponentValue(100L, ComponentTypeId(50L), data("  v  "))
        coVerify { lexemeApi.addComponentValue(100L, 50L, match { (it as TextValues).value.value == "v" }) }
    }

    // IS486 фаза 2: времянка-фильтр снят — CHOICE доезжает вместе с опциями.
    @Test
    fun `flowAvailableComponentTypes_delivers_choice_with_options`() = runTest {
        val choiceType = ComponentTypeApiEntity(
            id = 3L,
            systemKey = BuiltInComponent.PART_OF_SPEECH,
            dictionaryId = 10L,
            name = null,
            template = ComponentTemplate.CHOICE,
            position = 1,
            createdAt = D0,
            updatedAt = D0,
        )
        every { lexemeApi.flowUserDefinedTypesForDictionary(10L) } returns flowOf(
            me.apomazkin.core_db_api.entity.DictionaryTypesSnapshot(
                dictionaryId = 10L,
                dictionaryName = "ES",
                types = listOf(typeEntity(1L, TR), choiceType),
                valueCountByType = emptyMap(),
                optionsByType = mapOf(
                    3L to listOf(
                        me.apomazkin.core_db_api.entity.ComponentOptionApiEntity(
                            id = 601L, componentTypeId = 3L, systemKey = "noun", label = null, position = 0,
                        ),
                        me.apomazkin.core_db_api.entity.ComponentOptionApiEntity(
                            id = 602L, componentTypeId = 3L, systemKey = "verb", label = null, position = 1,
                        ),
                    ),
                ),
            ),
        )

        val available = useCase.flowAvailableComponentTypes(10L).first()

        assertEquals(2, available.types.size)
        val options = available.optionsByType[ComponentTypeId(3L)]
        assertEquals(listOf("noun", "verb"), options?.map { it.systemKey })
    }
}
