package me.apomazkin.quiztab.logic

import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import me.apomazkin.quiz.QuizGroup
import me.apomazkin.quiz.QuizGroupOptions
import me.apomazkin.quiz.QuizTypes
import me.apomazkin.quiztab.QuizTabNavigationEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * IS500 | Пикер группы карточки chat-квиза: дефолт state, применение
 * эмиссий подписки (опции + валидированный выбор + явный enabled-флаг),
 * выбор с персистом, guard-ветки, вход в чат.
 */
class QuizTabReducerTest {
    private val reducer = QuizTabReducer(logger = NoopLogger)

    private fun options(
        vararg groups: QuizGroup,
        isAllEligible: Boolean = true,
        allWordCount: Int = 10,
    ) = QuizGroupOptions(
        isAllEligible = isAllEligible,
        allWordCount = allWordCount,
        groups = groups.toList(),
    )

    private fun group(id: Long, count: Int = 5, eligible: Boolean = true) =
        QuizGroup(id = id, name = "g$id", wordCount = count, isEligible = eligible)

    private fun loaded(
        dictionaryId: Long? = 1L,
        options: QuizGroupOptions = options(group(5L)),
        selectedGroupId: Long? = null,
        selectionInvalidated: Boolean = false,
    ) = Msg.GroupOptionsLoaded(
        quizType = QuizTypes.CHAT,
        dictionaryId = dictionaryId,
        options = options,
        selectedGroupId = selectedGroupId,
        selectionInvalidated = selectionInvalidated,
    )

    // === Дефолт state ===

    @Test
    fun `default state - card enabled, All selected, no options`() {
        val state = QuizTabState()

        assertTrue(state.isChatCardEnabled)
        assertEquals(null, state.selectedGroupId)
        assertTrue(state.groupOptions.isEmpty())
        assertTrue(state.isAllEligible)
    }

    // === GroupOptionsLoaded ===

    @Test
    fun `options loaded - dict, groups, counts, selection applied`() {
        val result = reducer.reduce(
            QuizTabState(),
            loaded(
                dictionaryId = 7L,
                options = options(allWordCount = 20, groups = arrayOf(group(5L))),
                selectedGroupId = 5L,
            ),
        )

        val state = result.state()
        assertEquals(7L, state.dictionaryId)
        assertEquals(listOf(5L), state.groupOptions.map { it.id })
        assertEquals(20, state.allWordCount)
        assertEquals(5L, state.selectedGroupId)
        assertTrue(state.isChatCardEnabled)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `options loaded without eligible items - card disabled`() {
        val result = reducer.reduce(
            QuizTabState(),
            loaded(
                options = options(
                    isAllEligible = false,
                    allWordCount = 2,
                    groups = arrayOf(group(5L, count = 1, eligible = false)),
                ),
            ),
        )

        assertFalse(result.state().isChatCardEnabled)
    }

    @Test
    fun `options emission with null selection - previous selection falls back to All`() {
        // Транзиент «группа умерла/усохла»: воронка подписки резолвит
        // выбор в null на каждом эмите — reducer применяет без мигания.
        val selected = reducer.reduce(QuizTabState(), loaded(selectedGroupId = 5L)).state()

        val result = reducer.reduce(selected, loaded(selectedGroupId = null))

        assertEquals(null, result.state().selectedGroupId)
    }

    @Test
    fun `invalidated persist - fallback pinned by erasing pref`() {
        // Решение прогона 2026-09-26: после сброса на «Все» выбор
        // ОСТАЁТСЯ «Все» — исцеление группы его не воскрешает.
        val result = reducer.reduce(
            QuizTabState(),
            loaded(
                dictionaryId = 7L,
                selectedGroupId = null,
                selectionInvalidated = true,
            ),
        )

        assertEquals(
            setOf(
                QuizTabDatasourceEffect.PersistGroupSelection(
                    quizType = QuizTypes.CHAT,
                    dictionaryId = 7L,
                    groupId = null,
                ),
            ),
            result.effects(),
        )
    }

    @Test
    fun `invalidated persist without dictionary - no erase effect`() {
        val result = reducer.reduce(
            QuizTabState(),
            loaded(
                dictionaryId = null,
                selectedGroupId = null,
                selectionInvalidated = true,
            ),
        )

        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `options loaded for no-dictionary - card disabled`() {
        val result = reducer.reduce(
            QuizTabState(),
            loaded(
                dictionaryId = null,
                options = options(isAllEligible = false, allWordCount = 0),
                selectedGroupId = null,
            ),
        )

        assertEquals(null, result.state().dictionaryId)
        assertFalse(result.state().isChatCardEnabled)
    }

    // === PickGroup ===

    @Test
    fun `pick group - optimistic state and persist effect with dict and type`() {
        val ready = reducer.reduce(QuizTabState(), loaded(dictionaryId = 7L)).state()

        val result = reducer.reduce(
            ready,
            Msg.PickGroup(quizType = QuizTypes.CHAT, groupId = 5L),
        )

        assertEquals(5L, result.state().selectedGroupId)
        assertEquals(
            setOf(
                QuizTabDatasourceEffect.PersistGroupSelection(
                    quizType = QuizTypes.CHAT,
                    dictionaryId = 7L,
                    groupId = 5L,
                ),
            ),
            result.effects(),
        )
    }

    @Test
    fun `pick All - persist effect with null group`() {
        val ready = reducer
            .reduce(QuizTabState(), loaded(dictionaryId = 7L, selectedGroupId = 5L))
            .state()

        val result = reducer.reduce(
            ready,
            Msg.PickGroup(quizType = QuizTypes.CHAT, groupId = null),
        )

        assertEquals(null, result.state().selectedGroupId)
        assertEquals(
            setOf(
                QuizTabDatasourceEffect.PersistGroupSelection(
                    quizType = QuizTypes.CHAT,
                    dictionaryId = 7L,
                    groupId = null,
                ),
            ),
            result.effects(),
        )
    }

    @Test
    fun `pick same selection - no-op`() {
        val ready = reducer
            .reduce(QuizTabState(), loaded(dictionaryId = 7L, selectedGroupId = 5L))
            .state()

        val result = reducer.reduce(
            ready,
            Msg.PickGroup(quizType = QuizTypes.CHAT, groupId = 5L),
        )

        assertEquals(ready, result.state())
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `pick without dictionary - no-op`() {
        val result = reducer.reduce(
            QuizTabState(),
            Msg.PickGroup(quizType = QuizTypes.CHAT, groupId = 5L),
        )

        assertEquals(QuizTabState(), result.state())
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `pick with unknown quiz type - no-op`() {
        val ready = reducer.reduce(QuizTabState(), loaded(dictionaryId = 7L)).state()

        val result = reducer.reduce(
            ready,
            Msg.PickGroup(quizType = "flip_cards", groupId = 5L),
        )

        assertEquals(ready, result.state())
        assertTrue(result.effects().isEmpty())
    }

    // === OpenChat ===

    @Test
    fun `open chat when enabled - navigation effect`() {
        val result = reducer.reduce(QuizTabState(), Msg.OpenChat(QuizTypes.CHAT))

        assertEquals(
            setOf(QuizTabNavigationEffect.OpenChat(QuizTypes.CHAT)),
            result.effects(),
        )
    }

    @Test
    fun `open chat when disabled - no-op (reducer does not trust UI)`() {
        val disabled = reducer
            .reduce(
                QuizTabState(),
                loaded(options = options(isAllEligible = false, allWordCount = 1)),
            ).state()

        val result = reducer.reduce(disabled, Msg.OpenChat(QuizTypes.CHAT))

        assertTrue(result.effects().isEmpty())
    }

    // === GroupOptionsFailed ===

    @Test
    fun `options failed - state untouched, working default stays`() {
        val result = reducer.reduce(
            QuizTabState(),
            Msg.GroupOptionsFailed(quizType = QuizTypes.CHAT),
        )

        assertEquals(QuizTabState(), result.state())
        assertTrue(result.effects().isEmpty())
    }

    // === Подписки ===

    @Test
    fun `subscriptions - group options always on`() {
        assertEquals(setOf(QuizTabSub.GroupOptions), QuizTabState().subscriptions())
    }
}
