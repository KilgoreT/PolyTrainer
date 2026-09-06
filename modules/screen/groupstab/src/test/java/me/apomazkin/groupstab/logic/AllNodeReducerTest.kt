package me.apomazkin.groupstab.logic

import me.apomazkin.group.DisplayNode
import me.apomazkin.group.DisplayTree
import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import me.apomazkin.wordrow.entity.TermUiItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * IS493 | Узел «Все»: создание из slice, живое окно контента
 * (раскрытие/сворачивание/«Ещё»), компенсация вытеснения при росте
 * словаря, ошибки окна.
 */
class AllNodeReducerTest {

    private val reducer = GroupsTabReducer(logger = NoopLogger)

    private fun tree(vararg ids: Long) = DisplayTree(
        allWords = DisplayNode.AllWords(words = ids.toList(), count = ids.size),
        groups = emptyList(),
    )

    private fun term(id: Long) = TermUiItem(
        id = id,
        wordValue = "w$id",
        dictionaryId = 1L,
        addDate = Date(0),
    )

    private fun stateWithNode(node: AllNodeState) = GroupsTabState(
        isLoading = false,
        dictionaryId = 1L,
        allNode = node,
    )

    // === SliceLoaded → узел ===

    @Test
    fun `first slice - node created, loading off`() {
        val result = reducer.reduce(
            GroupsTabState(isLoading = true, dictionaryId = 1L),
            Msg.SliceLoaded(tree(30, 20, 10)),
        )

        val node = requireNotNull(result.state().allNode)
        assertFalse(result.state().isLoading)
        assertEquals(3, node.count)
        assertFalse(node.isExpanded)
        assertTrue(node.hasMore)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `dictionary growth under expanded node - window widened by delta`() {
        // Окно 50, загружено 3 из 3; вставилось слово (count 3 → 4).
        val current = stateWithNode(
            AllNodeState(
                count = 3,
                isExpanded = true,
                window = 50,
                loadedWords = listOf(term(30), term(20), term(10)),
                hasMore = false,
            )
        )

        val result = reducer.reduce(current, Msg.SliceLoaded(tree(40, 30, 20, 10)))

        val node = requireNotNull(result.state().allNode)
        assertEquals(4, node.count)
        // Компенсация вытеснения: окно расширено на дельту роста.
        assertEquals(51, node.window)
        assertEquals(setOf(GroupsEffect.SetWindow(limit = 51)), result.effects())
        // Контент придёт живой эмиссией окна — здесь loadedWords не трогаются.
        assertEquals(listOf(30L, 20L, 10L), node.loadedWords.map { it.id })
        assertTrue(node.hasMore)
    }

    @Test
    fun `dictionary growth under collapsed node - window untouched`() {
        val current = stateWithNode(AllNodeState(count = 3, isExpanded = false, hasMore = true))

        val result = reducer.reduce(current, Msg.SliceLoaded(tree(40, 30, 20, 10)))

        val node = requireNotNull(result.state().allNode)
        assertEquals(4, node.count)
        assertEquals(0, node.window)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `dictionary shrink - count and hasMore recalculated, window untouched`() {
        val current = stateWithNode(
            AllNodeState(
                count = 3,
                isExpanded = true,
                window = 50,
                loadedWords = listOf(term(30), term(20), term(10)),
                hasMore = false,
            )
        )

        val result = reducer.reduce(current, Msg.SliceLoaded(tree(30, 10)))

        val node = requireNotNull(result.state().allNode)
        assertEquals(2, node.count)
        assertEquals(50, node.window)
        assertTrue(result.effects().isEmpty())
        // Сам список подчистит живая эмиссия окна (WindowLoaded).
    }

    // === ToggleAll / LoadMore / окно ===

    @Test
    fun `expand - window subscription started`() {
        val current = stateWithNode(AllNodeState(count = 100, hasMore = true))

        val result = reducer.reduce(current, Msg.ToggleAll)

        val node = requireNotNull(result.state().allNode)
        assertTrue(node.isExpanded)
        assertTrue(node.isWindowLoading)
        assertEquals(CHUNK_SIZE, node.window)
        assertEquals(setOf(GroupsEffect.SetWindow(limit = CHUNK_SIZE)), result.effects())
    }

    @Test
    fun `expand empty node - no subscription`() {
        val current = stateWithNode(AllNodeState(count = 0))

        val result = reducer.reduce(current, Msg.ToggleAll)

        val node = requireNotNull(result.state().allNode)
        assertTrue(node.isExpanded)
        assertFalse(node.isWindowLoading)
        assertFalse(node.hasMore)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `collapse - window off, content dropped`() {
        val current = stateWithNode(
            AllNodeState(
                count = 3,
                isExpanded = true,
                window = 50,
                loadedWords = listOf(term(30), term(20)),
                hasMore = true,
            )
        )

        val result = reducer.reduce(current, Msg.ToggleAll)

        val node = requireNotNull(result.state().allNode)
        assertFalse(node.isExpanded)
        assertEquals(0, node.window)
        assertTrue(node.loadedWords.isEmpty())
        assertTrue(node.hasMore)
        assertEquals(setOf(GroupsEffect.SetWindow(limit = null)), result.effects())
    }

    @Test
    fun `load more - window widened by chunk`() {
        val current = stateWithNode(
            AllNodeState(
                count = 120,
                isExpanded = true,
                window = CHUNK_SIZE,
                loadedWords = (170L downTo (171L - CHUNK_SIZE)).map { term(it) },
                hasMore = true,
            )
        )

        val result = reducer.reduce(current, Msg.LoadMore)

        val node = requireNotNull(result.state().allNode)
        assertEquals(CHUNK_SIZE * 2, node.window)
        assertTrue(node.isWindowLoading)
        assertEquals(
            setOf(GroupsEffect.SetWindow(limit = CHUNK_SIZE * 2)),
            result.effects(),
        )
    }

    @Test
    fun `load more while window loading - ignored`() {
        val current = stateWithNode(
            AllNodeState(
                count = 100,
                isExpanded = true,
                window = 50,
                loadedWords = listOf(term(30)),
                isWindowLoading = true,
                hasMore = true,
            )
        )

        val result = reducer.reduce(current, Msg.LoadMore)

        assertEquals(current, result.state())
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `load more when everything shown - hasMore off, no effect`() {
        val current = stateWithNode(
            AllNodeState(
                count = 2,
                isExpanded = true,
                window = 50,
                loadedWords = listOf(term(30), term(20)),
                hasMore = true,
            )
        )

        val result = reducer.reduce(current, Msg.LoadMore)

        assertFalse(requireNotNull(result.state().allNode).hasMore)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `window loaded - content replaced, spinner off, hasMore recalculated`() {
        val current = stateWithNode(
            AllNodeState(
                count = 3,
                isExpanded = true,
                window = 50,
                loadedWords = listOf(term(30)),
                isWindowLoading = true,
                hasMore = true,
            )
        )

        val result = reducer.reduce(
            current,
            Msg.WindowLoaded(listOf(term(30), term(20), term(10))),
        )

        val node = requireNotNull(result.state().allNode)
        assertEquals(listOf(30L, 20L, 10L), node.loadedWords.map { it.id })
        assertFalse(node.isWindowLoading)
        assertFalse(node.hasMore)
    }

    @Test
    fun `window loaded after collapse - ignored`() {
        val current = stateWithNode(
            AllNodeState(count = 2, isExpanded = false, hasMore = true)
        )

        val result = reducer.reduce(current, Msg.WindowLoaded(listOf(term(30))))

        assertEquals(current, result.state())
    }

    // === Ошибки (T-6) ===

    @Test
    fun `window load failed - spinner off, button alive`() {
        val current = stateWithNode(
            AllNodeState(
                count = 2,
                isExpanded = true,
                window = 50,
                isWindowLoading = true,
                hasMore = true,
            )
        )

        val result = reducer.reduce(current, Msg.WindowLoadFailed)

        val node = requireNotNull(result.state().allNode)
        assertFalse(node.isWindowLoading)
        assertTrue(node.hasMore)
    }
}
