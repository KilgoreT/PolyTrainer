package me.apomazkin.quiztab.logic

import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import me.apomazkin.quiz.QuizGroup
import me.apomazkin.quiz.QuizGroupLabel
import me.apomazkin.quiz.QuizGroupOptions
import me.apomazkin.quiz.QuizTypes
import me.apomazkin.quiztab.QuizTabNavigationEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Пикер набора групп карточки chat-квиза: дефолт state, применение
 * эмиссий подписки (опции + валидированный набор + подпись + явный
 * enabled-флаг), закрепление очищенного набора, галки групп и «Все» с
 * персистом, guard-ветки, вход в чат.
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

    private fun group(id: Long, count: Int = 5, eligible: Boolean = true, name: String = "g$id") =
        QuizGroup(id = id, name = name, wordCount = count, isEligible = eligible)

    private val threeGroups = options(
        group(1L, name = "Быт"),
        group(2L, name = "Дом"),
        group(3L, name = "Еда"),
        group(4L, name = "Мелкая", count = 1, eligible = false),
    )

    private fun loaded(
        dictionaryId: Long? = 1L,
        options: QuizGroupOptions = options(group(5L)),
        selectedGroupIds: Set<Long> = emptySet(),
        selectionInvalidated: Boolean = false,
    ) = Msg.GroupOptionsLoaded(
        quizType = QuizTypes.CHAT,
        dictionaryId = dictionaryId,
        options = options,
        selectedGroupIds = selectedGroupIds,
        selectionInvalidated = selectionInvalidated,
    )

    private fun ready(selected: Set<Long> = emptySet()): QuizTabState = reducer
        .reduce(QuizTabState(), loaded(dictionaryId = 7L, options = threeGroups, selectedGroupIds = selected))
        .state()

    private fun persist(vararg ids: Long) = setOf(
        QuizTabDatasourceEffect.PersistGroupSelection(
            quizType = QuizTypes.CHAT,
            dictionaryId = 7L,
            groupIds = ids.toSet(),
        ),
    )

    // === Дефолт state ===

    @Test
    fun `default state - card enabled, All selected, no options`() {
        val state = QuizTabState()

        assertTrue(state.isChatCardEnabled)
        assertTrue(state.selectedGroupIds.isEmpty())
        assertNull(state.selectionLabel)
        assertTrue(state.groupOptions.isEmpty())
        assertTrue(state.isAllEligible)
    }

    // === GroupOptionsLoaded ===

    @Test
    fun `options loaded - dict, groups, counts, selection and label applied`() {
        val result = reducer.reduce(
            QuizTabState(),
            loaded(
                dictionaryId = 7L,
                options = options(allWordCount = 20, groups = arrayOf(group(5L, name = "Быт"))),
                selectedGroupIds = setOf(5L),
            ),
        )

        val state = result.state()
        assertEquals(7L, state.dictionaryId)
        assertEquals(listOf(5L), state.groupOptions.map { it.id })
        assertEquals(20, state.allWordCount)
        assertEquals(setOf(5L), state.selectedGroupIds)
        assertEquals(QuizGroupLabel(first = "Быт", more = 0), state.selectionLabel)
        assertTrue(state.isChatCardEnabled)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `options loaded with several groups - label is first in list order plus the rest`() {
        val state = ready(selected = setOf(3L, 1L))

        assertEquals(QuizGroupLabel(first = "Быт", more = 1), state.selectionLabel)
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
    fun `options emission with shrunk set - dropped group leaves selection and label`() {
        // Транзиент «группа умерла/усохла»: воронка подписки выкидывает её
        // на каждом эмите — reducer применяет без мигания.
        val selected = ready(selected = setOf(1L, 2L))

        val result = reducer.reduce(
            selected,
            loaded(dictionaryId = 7L, options = threeGroups, selectedGroupIds = setOf(2L)),
        )

        assertEquals(setOf(2L), result.state().selectedGroupIds)
        assertEquals(QuizGroupLabel(first = "Дом", more = 0), result.state().selectionLabel)
    }

    @Test
    fun `invalidated persist - cleaned set pinned by writing it`() {
        // Выпавшая группа не возвращается сама: очищенный набор пишется.
        val result = reducer.reduce(
            QuizTabState(),
            loaded(
                dictionaryId = 7L,
                options = threeGroups,
                selectedGroupIds = setOf(2L),
                selectionInvalidated = true,
            ),
        )

        assertEquals(persist(2L), result.effects())
    }

    @Test
    fun `invalidated persist with all dropped - All pinned by erasing`() {
        val result = reducer.reduce(
            QuizTabState(),
            loaded(dictionaryId = 7L, selectedGroupIds = emptySet(), selectionInvalidated = true),
        )

        assertEquals(persist(), result.effects())
    }

    @Test
    fun `invalidated persist without dictionary - no write effect`() {
        val result = reducer.reduce(
            QuizTabState(),
            loaded(dictionaryId = null, selectionInvalidated = true),
        )

        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `options loaded for no-dictionary - card disabled`() {
        val result = reducer.reduce(
            QuizTabState(),
            loaded(dictionaryId = null, options = options(isAllEligible = false, allWordCount = 0)),
        )

        assertEquals(null, result.state().dictionaryId)
        assertFalse(result.state().isChatCardEnabled)
    }

    // === ToggleGroup ===

    @Test
    fun `check group from All - All dropped, group selected, persisted`() {
        val result = reducer.reduce(ready(), Msg.ToggleGroup(QuizTypes.CHAT, groupId = 2L, checked = true))

        assertEquals(setOf(2L), result.state().selectedGroupIds)
        assertEquals(QuizGroupLabel(first = "Дом", more = 0), result.state().selectionLabel)
        assertEquals(persist(2L), result.effects())
    }

    @Test
    fun `check second group - added to set`() {
        val result = reducer.reduce(ready(setOf(2L)), Msg.ToggleGroup(QuizTypes.CHAT, groupId = 1L, checked = true))

        assertEquals(setOf(1L, 2L), result.state().selectedGroupIds)
        assertEquals(QuizGroupLabel(first = "Быт", more = 1), result.state().selectionLabel)
        assertEquals(persist(1L, 2L), result.effects())
    }

    @Test
    fun `uncheck one of several - removed from set`() {
        val result = reducer.reduce(ready(setOf(1L, 2L)), Msg.ToggleGroup(QuizTypes.CHAT, groupId = 1L, checked = false))

        assertEquals(setOf(2L), result.state().selectedGroupIds)
        assertEquals(persist(2L), result.effects())
    }

    @Test
    fun `uncheck last group - back to All`() {
        val result = reducer.reduce(ready(setOf(2L)), Msg.ToggleGroup(QuizTypes.CHAT, groupId = 2L, checked = false))

        assertTrue(result.state().selectedGroupIds.isEmpty())
        assertNull(result.state().selectionLabel)
        assertEquals(persist(), result.effects())
    }

    @Test
    fun `check already selected - no-op`() {
        val state = ready(setOf(2L))

        val result = reducer.reduce(state, Msg.ToggleGroup(QuizTypes.CHAT, groupId = 2L, checked = true))

        assertEquals(state, result.state())
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `check ineligible group - no-op (reducer does not trust UI)`() {
        val state = ready()

        val result = reducer.reduce(state, Msg.ToggleGroup(QuizTypes.CHAT, groupId = 4L, checked = true))

        assertEquals(state, result.state())
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `check group missing from options - no-op`() {
        val state = ready()

        val result = reducer.reduce(state, Msg.ToggleGroup(QuizTypes.CHAT, groupId = 99L, checked = true))

        assertEquals(state, result.state())
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `toggle without dictionary - no-op`() {
        val result = reducer.reduce(QuizTabState(), Msg.ToggleGroup(QuizTypes.CHAT, groupId = 5L, checked = true))

        assertEquals(QuizTabState(), result.state())
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `toggle with unknown quiz type - no-op`() {
        val state = ready()

        val result = reducer.reduce(state, Msg.ToggleGroup("flip_cards", groupId = 1L, checked = true))

        assertEquals(state, result.state())
        assertTrue(result.effects().isEmpty())
    }

    // === PickAll ===

    @Test
    fun `pick All - groups dropped, label cleared, erase persisted`() {
        val result = reducer.reduce(ready(setOf(1L, 2L)), Msg.PickAll(QuizTypes.CHAT))

        assertTrue(result.state().selectedGroupIds.isEmpty())
        assertNull(result.state().selectionLabel)
        assertEquals(persist(), result.effects())
    }

    @Test
    fun `pick All when already All - no-op`() {
        val state = ready()

        val result = reducer.reduce(state, Msg.PickAll(QuizTypes.CHAT))

        assertEquals(state, result.state())
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `pick All without dictionary - no-op`() {
        val result = reducer.reduce(QuizTabState(), Msg.PickAll(QuizTypes.CHAT))

        assertEquals(QuizTabState(), result.state())
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `pick All with unknown quiz type - no-op`() {
        val state = ready(setOf(1L))

        val result = reducer.reduce(state, Msg.PickAll("flip_cards"))

        assertEquals(state, result.state())
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
