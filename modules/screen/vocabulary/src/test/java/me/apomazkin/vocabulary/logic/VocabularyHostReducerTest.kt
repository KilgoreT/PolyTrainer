package me.apomazkin.vocabulary.logic

import io.github.kilgoret.mate.effects
import io.github.kilgoret.mate.state
import me.apomazkin.vocabulary.VocabularyTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases (IS493):
 * Э1 (stage1_plan §1.2):
 * 1. Boundary case: исходное состояние — вкладка WORDS
 * 2. Standard case: SelectTab(GROUPS) переключает вкладку, без эффектов
 * 3. Boundary case: SelectTab на текущую вкладку — состояние не меняется, без эффектов
 * Э2 (stage2_plan Фаза 4, D9.1):
 * 4. Boundary case: исходное состояние — словарь не отрезолвлен (loading-фаза)
 * 5. Standard case: DictionaryChanged(id) — id в state, isDictResolved = true
 * 6. Boundary case: DictionaryChanged(null) — честное «словарей нет», isDictResolved = true
 * 7. Standard case: SelectTab не трогает поля словаря
 */
class VocabularyHostReducerTest {

    private val reducer = VocabularyHostReducer()

    @Test
    fun `initial state - selected tab is WORDS`() {
        assertEquals(VocabularyTab.WORDS, VocabularyHostState().selectedTab)
    }

    @Test
    fun `initial state - dictionary not resolved`() {
        val initial = VocabularyHostState()
        assertEquals(null, initial.dictionaryId)
        assertEquals(false, initial.isDictResolved)
    }

    @Test
    fun `dictionary changed - id stored, resolved flag set, no effects`() {
        val result = reducer.reduce(
            state = VocabularyHostState(),
            message = Msg.DictionaryChanged(dictionaryId = 42L),
        )
        assertEquals(42L, result.state().dictionaryId)
        assertEquals(true, result.state().isDictResolved)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `dictionary changed to null - resolved flag still set (no dictionaries)`() {
        val result = reducer.reduce(
            state = VocabularyHostState(dictionaryId = 42L, isDictResolved = true),
            message = Msg.DictionaryChanged(dictionaryId = null),
        )
        assertEquals(null, result.state().dictionaryId)
        assertEquals(true, result.state().isDictResolved)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `select tab - dictionary fields untouched`() {
        val initial = VocabularyHostState(dictionaryId = 42L, isDictResolved = true)
        val result = reducer.reduce(
            state = initial,
            message = Msg.SelectTab(tab = VocabularyTab.GROUPS),
        )
        assertEquals(42L, result.state().dictionaryId)
        assertEquals(true, result.state().isDictResolved)
    }

    @Test
    fun `select GROUPS - tab switches, no effects`() {
        val result = reducer.reduce(
            state = VocabularyHostState(),
            message = Msg.SelectTab(tab = VocabularyTab.GROUPS),
        )
        assertEquals(VocabularyTab.GROUPS, result.state().selectedTab)
        assertTrue(result.effects().isEmpty())
    }

    @Test
    fun `select current tab - state unchanged, no effects`() {
        val initial = VocabularyHostState(selectedTab = VocabularyTab.WORDS)
        val result = reducer.reduce(
            state = initial,
            message = Msg.SelectTab(tab = VocabularyTab.WORDS),
        )
        assertEquals(initial, result.state())
        assertTrue(result.effects().isEmpty())
    }
}
