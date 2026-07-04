package me.apomazkin.dictionaryappbar.mate

import me.apomazkin.dictionarypicker.entity.DictUiEntity
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.logger.LogLevel
import me.apomazkin.dictionaryappbar.DictionaryAppBarNavigationEffect
import me.apomazkin.mate.state
import me.apomazkin.mate.effects
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test cases for DictionaryAppBarReducer (IS476 nullable CurrentDict).
 *
 * === Msg.CurrentDict — nullable contract ===
 * 1. Standard: CurrentDict(non-null) → state.currentDict updated, no effects
 * 2. Boundary: CurrentDict(null) → state.currentDict = null, no effects (IS476)
 * 3. Standard: CurrentDict(null) clears previously set currentDict
 *
 * === Msg.AvailableDict ===
 * 4. Standard: AvailableDict sets list and hides loading
 *
 * === Msg.DictMenu* ===
 * 5. Standard: DictMenuOn sets isDropDownMenuOpen = true
 * 6. Standard: DictMenuOff sets isDropDownMenuOpen = false
 *
 * === Msg.OpenPerDictionaryComponents (IS481) ===
 * 7. Standard: OpenPerDictionaryComponents(dictionaryId=1L) on initial state with currentDict=dictEn
 *    → state immutable, emit OpenPerDictionaryComponents(dictionaryId=1L) nav-effect
 * 8. Standard: OpenPerDictionaryComponents(dictionaryId=0L) sentinel — payload prokidyvaetsya as-is
 *    (documents the contract «reducer doesn't validate payload»)
 */
class DictionaryAppBarReducerTest {

    private val reducer = DictionaryAppBarReducer(
        logger = object : LexemeLogger {
            override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {}
        }
    )

    private val dictEn = DictUiEntity(
        id = 1L,
        flagRes = 100,
        title = "English",
        numericCode = 826,
    )

    private val dictEs = DictUiEntity(
        id = 2L,
        flagRes = 200,
        title = "Spanish",
        numericCode = 724,
    )

    // === Msg.CurrentDict — nullable contract ===

    @Test
    fun `should update state currentDict when CurrentDict with non-null received`() {
        // Test case 1: Standard - happy path, словарь выбран
        // Given
        val initialState = DictionaryAppBarState()

        // When
        val result = reducer.testReduce(initialState, Msg.CurrentDict(current = dictEn))

        // Then
        assertEquals("currentDict should be set", dictEn, result.state().currentDict)
        // Immutability checks
        assertEquals(
            "availableDictList should not change",
            initialState.availableDictList,
            result.state().availableDictList,
        )
        assertEquals(
            "isDropDownMenuOpen should not change",
            initialState.isDropDownMenuOpen,
            result.state().isDropDownMenuOpen,
        )
        assertEquals(
            "isLoading should not change",
            initialState.isLoading,
            result.state().isLoading,
        )
        result.assertNoEffects("CurrentDict should produce no effects")
    }

    @Test
    fun `should set state currentDict to null when CurrentDict with null received on default state`() {
        // Test case 2: Boundary - IS476, реактивно получили null от flowCurrentDict
        // Given
        val initialState = DictionaryAppBarState()

        // When
        val result = reducer.testReduce(initialState, Msg.CurrentDict(current = null))

        // Then
        assertNull("currentDict should be null", result.state().currentDict)
        // Immutability checks
        assertEquals(
            "availableDictList should not change",
            initialState.availableDictList,
            result.state().availableDictList,
        )
        assertEquals(
            "isDropDownMenuOpen should not change",
            initialState.isDropDownMenuOpen,
            result.state().isDropDownMenuOpen,
        )
        result.assertNoEffects("CurrentDict(null) should produce no effects")
    }

    @Test
    fun `should clear currentDict when CurrentDict with null received after non-null`() {
        // Test case 3: Standard - пользователь удалил последний словарь,
        // подписка эмитнула null, reducer должен сбросить ранее установленное значение.
        // Given
        val initialState = DictionaryAppBarState(currentDict = dictEs)

        // When
        val result = reducer.testReduce(initialState, Msg.CurrentDict(current = null))

        // Then
        assertNull("currentDict should be cleared to null", result.state().currentDict)
        result.assertNoEffects("CurrentDict(null) should produce no effects")
    }

    // === Msg.AvailableDict ===

    @Test
    fun `should set list and hide loading when AvailableDict received`() {
        // Test case 4: Standard
        // Given
        val initialState = DictionaryAppBarState(isLoading = true)
        val list = listOf(dictEn, dictEs)

        // When
        val result = reducer.testReduce(initialState, Msg.AvailableDict(list = list))

        // Then
        assertEquals("availableDictList should be set", list, result.state().availableDictList)
        assertFalse("isLoading should be hidden", result.state().isLoading)
        result.assertNoEffects("AvailableDict should produce no effects")
    }

    // === Msg.DictMenu* ===

    @Test
    fun `should open dropdown when DictMenuOn received`() {
        // Test case 5
        val initialState = DictionaryAppBarState(isDropDownMenuOpen = false)

        val result = reducer.testReduce(initialState, Msg.DictMenuOn)

        assertTrue("isDropDownMenuOpen should be true", result.state().isDropDownMenuOpen)
        result.assertNoEffects("DictMenuOn should produce no effects")
    }

    @Test
    fun `should close dropdown when DictMenuOff received`() {
        // Test case 6
        val initialState = DictionaryAppBarState(isDropDownMenuOpen = true)

        val result = reducer.testReduce(initialState, Msg.DictMenuOff)

        assertFalse("isDropDownMenuOpen should be false", result.state().isDropDownMenuOpen)
        result.assertNoEffects("DictMenuOff should produce no effects")
    }

    // === Msg.OpenPerDictionaryComponents (IS481) ===

    @Test
    fun `should emit OpenPerDictionaryComponents nav effect carrying dictionaryId when Msg OpenPerDictionaryComponents received`() {
        // Test case 7: Standard - IS481, юзер тапает «молоток» в DictionaryAppBar
        // Given
        val initialState = DictionaryAppBarState(currentDict = dictEn)

        // When
        val result = reducer.testReduce(initialState, Msg.OpenPerDictionaryComponents(dictionaryId = 1L))

        // Then
        assertEquals("state should be unchanged", initialState, result.state())
        result.assertEffects(
            setOf(DictionaryAppBarNavigationEffect.OpenPerDictionaryComponents(dictionaryId = 1L))
        )
    }

    @Test
    fun `should pass dictionaryId payload as-is when Msg OpenPerDictionaryComponents received with sentinel value`() {
        // Test case 8: Payload passthrough - sentinel-0 documents «reducer не валидирует payload, прокидывает as-is»
        // Защита от future require(dictionaryId > 0) рефакторинга на уровне reducer.
        // Given
        val initialState = DictionaryAppBarState()

        // When
        val result = reducer.testReduce(initialState, Msg.OpenPerDictionaryComponents(dictionaryId = 0L))

        // Then
        assertEquals("state should be unchanged", initialState, result.state())
        result.assertEffects(
            setOf(DictionaryAppBarNavigationEffect.OpenPerDictionaryComponents(dictionaryId = 0L))
        )
    }
}
