package me.apomazkin.wordcard.mate

import me.apomazkin.core_resources.R
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertEffects
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * §1.2 ComponentTypesLoaded / ComponentTypesLoadFailed / RetryLoadComponentTypes.
 */
class ComponentTypesFlowTest {

    private val reducer = WordCardReducer()
    private val t1 = ctype(50L, TR)
    private val t2 = ctype(51L, ComponentTypeRef.UserDefined("Example"))

    @Test
    fun `ComponentTypesLoaded stores types`() {
        val result = reducer.testReduce(loaded(availableTypes = emptyList()), Msg.ComponentTypesLoaded(listOf(t1, t2)))
        assertEquals(listOf(t1, t2), result.state().availableComponentTypes)
        result.assertNoEffects()
    }

    @Test
    fun `ComponentTypesLoaded overwrites previous and keeps lexemeList`() {
        val initial = loaded(availableTypes = listOf(t1), lexemes = listOf(lexeme(1L, listOf(savedCv(5L)))))
        val result = reducer.testReduce(initial, Msg.ComponentTypesLoaded(listOf(t2)))
        assertEquals(listOf(t2), result.state().availableComponentTypes)
        assertEquals(initial.lexemeList, result.state().lexemeList)
        result.assertNoEffects()
    }

    @Test
    fun `ComponentTypesLoaded does not touch pending (stays true)`() {
        val initial = loaded(isPendingDbOp = true, availableTypes = listOf(t1), lexemes = listOf(lexeme(1L, listOf(savedCv(5L)))))
        val result = reducer.testReduce(initial, Msg.ComponentTypesLoaded(listOf(t2)))
        assertEquals(true, result.state().isPendingDbOp)
        assertEquals(initial.lexemeList, result.state().lexemeList)
    }

    @Test
    fun `ComponentTypesLoaded empty clears list but keeps saved values rendered`() {
        val initial = loaded(availableTypes = listOf(t1), lexemes = listOf(lexeme(1L, listOf(savedCv(5L)))))
        val result = reducer.testReduce(initial, Msg.ComponentTypesLoaded(emptyList()))
        assertEquals(emptyList<Any>(), result.state().availableComponentTypes)
        assertEquals(initial.lexemeList, result.state().lexemeList)
    }

    @Test
    fun `ComponentTypesLoadFailed emits retry snackbar`() {
        val initial = loaded()
        val result = reducer.testReduce(initial, Msg.ComponentTypesLoadFailed(IOException()))
        assertEquals(initial, result.state())
        result.assertEffects(
            setOf(
                UiEffect.ShowSnackbarWithRetry(
                    messageRes = R.string.word_card_error_load_component_types,
                    actionLabelRes = R.string.word_card_action_retry,
                    retryMsg = Msg.RetryLoadComponentTypes,
                ),
            ),
        )
    }

    @Test
    fun `ComponentTypesLoadFailed not guarded by pending`() {
        val result = reducer.testReduce(loaded(isPendingDbOp = true), Msg.ComponentTypesLoadFailed(IOException()))
        result.assertEffects(
            setOf(
                UiEffect.ShowSnackbarWithRetry(
                    messageRes = R.string.word_card_error_load_component_types,
                    actionLabelRes = R.string.word_card_action_retry,
                    retryMsg = Msg.RetryLoadComponentTypes,
                ),
            ),
        )
    }

    @Test
    fun `RetryLoadComponentTypes emits load with loaded dictionaryId`() {
        val result = reducer.testReduce(loaded(dictionaryId = 9L), Msg.RetryLoadComponentTypes)
        result.assertEffects(setOf(DatasourceEffect.LoadAvailableComponentTypes(9L)))
    }

    @Test
    fun `RetryLoadComponentTypes on NotLoaded is no-op`() {
        val result = reducer.testReduce(WordCardState(), Msg.RetryLoadComponentTypes)
        result.assertNoEffects()
    }
}
