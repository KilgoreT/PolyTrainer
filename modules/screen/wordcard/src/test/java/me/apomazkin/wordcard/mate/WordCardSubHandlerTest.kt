package me.apomazkin.wordcard.mate

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import me.apomazkin.group.GroupNode
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.wordcard.deps.AvailableComponents
import me.apomazkin.wordcard.deps.WordCardUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Тесты [WordCardSubHandler] — исполнителя семейства подписок
 * [WordCardSub]. `flow(sub)` — чистая фабрика Flow: тестируем сбором
 * потока в runTest, без раннера mate.
 *
 * Контракт ошибок различается по веткам:
 * - ComponentTypes: ошибка → [Msg.ComponentTypesLoadFailed] (снек с
 *   Retry), CancellationException пробрасывается;
 * - WordGroups / DictGroups: ошибка только логируется, fail-Msg нет —
 *   блок групп деградирует молча.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WordCardSubHandlerTest {

    private val tr = ctype(50L, TR)
    private val ex = ctype(51L, ComponentTypeRef.UserDefined("Example"))

    private fun typesUseCase(flowImpl: (Long) -> Flow<AvailableComponents>): WordCardUseCase =
        object : WordCardUseCase by NotImplementedUseCase {
            override fun flowAvailableComponentTypes(dictionaryId: Long) = flowImpl(dictionaryId)
        }

    private fun groupsUseCase(
        wordGroupsImpl: (Long) -> Flow<List<GroupNode>> = { TODO() },
        dictGroupsImpl: (Long) -> Flow<List<GroupNode>> = { TODO() },
    ): WordCardUseCase = object : WordCardUseCase by NotImplementedUseCase {
        override fun wordGroups(wordId: Long) = wordGroupsImpl(wordId)
        override fun dictGroups(dictionaryId: Long) = dictGroupsImpl(dictionaryId)
    }

    private fun handler(useCase: WordCardUseCase) = WordCardSubHandler(useCase, NoopLogger)

    // ===== WordCardSub.ComponentTypes =====

    @Test
    fun `ComponentTypes happy path emits ComponentTypesLoaded`() = runTest {
        val handler = handler(typesUseCase { flowOf(AvailableComponents(listOf(tr, ex))) })

        val msgs = handler
            .flow(WordCardSub.ComponentTypes(dictionaryId = 1L, generation = 0))
            .toList()

        assertEquals(listOf<Msg>(Msg.ComponentTypesLoaded(AvailableComponents(listOf(tr, ex)))), msgs)
    }

    @Test
    fun `ComponentTypes flow error emits ComponentTypesLoadFailed`() = runTest {
        val handler = handler(typesUseCase { flow { throw IOException("boom") } })

        val msgs = handler
            .flow(WordCardSub.ComponentTypes(dictionaryId = 1L, generation = 0))
            .toList()

        assertTrue(msgs.single() is Msg.ComponentTypesLoadFailed)
    }

    @Test
    fun `ComponentTypes CancellationException not mapped to ComponentTypesLoadFailed`() = runTest {
        val handler = handler(typesUseCase { flow { throw CancellationException() } })
        val msgs = mutableListOf<Msg>()

        var cancelled = false
        try {
            handler
                .flow(WordCardSub.ComponentTypes(dictionaryId = 1L, generation = 0))
                .collect { msgs += it }
        } catch (e: CancellationException) {
            cancelled = true
        }

        assertTrue("Cancellation пробрасывается наружу", cancelled)
        assertTrue("Cancellation НЕ превращается в Failed", msgs.isEmpty())
    }

    @Test
    fun `ComponentTypes passes subscription dictionaryId to useCase`() = runTest {
        var requestedDictId: Long? = null
        val handler = handler(
            typesUseCase { dictId ->
                requestedDictId = dictId
                flowOf(AvailableComponents(listOf(tr)))
            },
        )

        handler.flow(WordCardSub.ComponentTypes(dictionaryId = 9L, generation = 0)).toList()

        assertEquals(9L, requestedDictId)
    }

    // ===== WordCardSub.WordGroups =====

    @Test
    fun `WordGroups maps groups to id set`() = runTest {
        val handler = handler(
            groupsUseCase(
                wordGroupsImpl = {
                    flowOf(
                        listOf(
                            GroupNode(id = 5L, parentGroupId = null, name = "B"),
                            GroupNode(id = 3L, parentGroupId = null, name = "A"),
                        ),
                    )
                },
            ),
        )

        val msgs = handler.flow(WordCardSub.WordGroups(wordId = 7L)).toList()

        assertEquals(listOf<Msg>(Msg.WordGroupsLoaded(setOf(5L, 3L))), msgs)
    }

    @Test
    fun `WordGroups flow error - silent degradation, no fail msg`() = runTest {
        val handler = handler(
            groupsUseCase(wordGroupsImpl = { flow { throw IOException("boom") } }),
        )

        val msgs = handler.flow(WordCardSub.WordGroups(wordId = 7L)).toList()

        assertTrue("ошибка групп только логируется", msgs.isEmpty())
    }

    // ===== WordCardSub.DictGroups =====

    @Test
    fun `DictGroups sorts groups by name and maps to GroupUi`() = runTest {
        val handler = handler(
            groupsUseCase(
                dictGroupsImpl = {
                    flowOf(
                        listOf(
                            GroupNode(id = 2L, parentGroupId = null, name = "b"),
                            GroupNode(id = 1L, parentGroupId = null, name = "a"),
                        ),
                    )
                },
            ),
        )

        val msgs = handler.flow(WordCardSub.DictGroups(dictionaryId = 3L)).toList()

        assertEquals(
            listOf<Msg>(
                Msg.DictGroupsLoaded(
                    listOf(
                        GroupUi(id = 1L, name = "a"),
                        GroupUi(id = 2L, name = "b"),
                    ),
                ),
            ),
            msgs,
        )
    }

    @Test
    fun `DictGroups flow error - silent degradation, no fail msg`() = runTest {
        val handler = handler(
            groupsUseCase(dictGroupsImpl = { flow { throw IOException("boom") } }),
        )

        val msgs = handler.flow(WordCardSub.DictGroups(dictionaryId = 3L)).toList()

        assertTrue("ошибка групп только логируется", msgs.isEmpty())
    }
}
