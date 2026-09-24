package me.apomazkin.polytrainer.di.module.groupstab

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.apomazkin.core_db_api.CoreDbApi
import me.apomazkin.core_db_api.entity.MembershipSliceApiEntity
import me.apomazkin.core_db_api.entity.TermApiEntity
import me.apomazkin.core_db_api.entity.WordApiEntity
import me.apomazkin.group.MembershipEntry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Test cases for GroupsTabUseCaseImpl (IS493 Э2, D11, живое окно):
 * 1. membershipSlice: Api-entity маппится в domain MembershipEntry 1:1
 *    (порядок сохраняется, nullable groupId проходит)
 * 2. flowWordsWindow: TermApiEntity → TermUiItem (id/value/dictionaryId/даты)
 * 3. flowWordsWindow: limit прокидывается в API как есть
 */
class GroupsTabUseCaseImplTest {

    private lateinit var groupApi: CoreDbApi.GroupApi
    private lateinit var termApi: CoreDbApi.TermApi
    private lateinit var useCase: GroupsTabUseCaseImpl

    private val now = Date(1_700_000_000_000L)

    @Before
    fun setUp() {
        groupApi = mockk(relaxed = true)
        termApi = mockk(relaxed = true)
        useCase = GroupsTabUseCaseImpl(
            groupApi = groupApi,
            termApi = termApi,
            dictionaryApi = mockk(relaxed = true),
            prefsProvider = mockk(relaxed = true),
        )
    }

    @Test
    fun `membershipSlice maps api entities to domain preserving order`() = runTest {
        every { groupApi.membershipSlice(1L) } returns flowOf(
            listOf(
                MembershipSliceApiEntity(wordId = 30L, groupId = 5L),
                MembershipSliceApiEntity(wordId = 20L, groupId = null),
            )
        )

        val result = useCase.membershipSlice(1L).first()

        assertEquals(
            listOf(
                MembershipEntry(wordId = 30L, groupId = 5L),
                MembershipEntry(wordId = 20L, groupId = null),
            ),
            result,
        )
    }

    @Test
    fun `flowWordsWindow maps term api entity to ui item`() = runTest {
        every { termApi.flowTermsWindow(dictionaryId = 1L, limit = 50) } returns flowOf(
            listOf(
                TermApiEntity(
                    word = WordApiEntity(
                        id = 30L,
                        dictionaryId = 1L,
                        value = "дом",
                        addDate = now,
                        changeDate = null,
                    ),
                    lexemes = emptyList(),
                )
            )
        )

        val result = useCase.flowWordsWindow(dictionaryId = 1L, limit = 50).first()

        assertEquals(1, result.size)
        assertEquals(30L, result.first().id)
        assertEquals("дом", result.first().wordValue)
        assertEquals(1L, result.first().dictionaryId)
        assertEquals(now, result.first().addDate)
    }

    @Test
    fun `groupTree maps api entities to domain nodes`() = runTest {
        every { groupApi.groupTree(1L) } returns flowOf(
            listOf(
                me.apomazkin.core_db_api.entity.GroupApiEntity(
                    id = 5L, dictionaryId = 1L, parentGroupId = null, name = "Дом",
                )
            )
        )

        val result = useCase.groupTree(1L).first()

        assertEquals(
            listOf(me.apomazkin.group.GroupNode(id = 5L, parentGroupId = null, name = "Дом")),
            result,
        )
    }

    @Test
    fun `mutations delegate to group api verbatim`() = runTest {
        coEvery { groupApi.createGroup(1L, "Дом") } returns
            me.apomazkin.group.CreateGroupOutcome.Success(groupId = 7L)
        coEvery { groupApi.renameGroup(7L, "Быт") } returns
            me.apomazkin.group.RenameGroupOutcome.Success
        coEvery { groupApi.deleteGroup(7L) } returns
            me.apomazkin.group.DeleteGroupOutcome.Success

        assertEquals(
            me.apomazkin.group.CreateGroupOutcome.Success(groupId = 7L),
            useCase.createGroup(1L, "Дом"),
        )
        assertEquals(me.apomazkin.group.RenameGroupOutcome.Success, useCase.renameGroup(7L, "Быт"))
        assertEquals(me.apomazkin.group.DeleteGroupOutcome.Success, useCase.deleteGroup(7L))
    }

    @Test
    fun `flowWordsWindow passes limit through`() = runTest {
        every { termApi.flowTermsWindow(dictionaryId = 1L, limit = 100) } returns flowOf(emptyList())

        val result = useCase.flowWordsWindow(dictionaryId = 1L, limit = 100).first()

        assertEquals(0, result.size)
        io.mockk.verify { termApi.flowTermsWindow(dictionaryId = 1L, limit = 100) }
    }
}
