package me.apomazkin.quiz.chat.logic

import io.mockk.every
import io.mockk.mockk
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.logger.LexemeLogger
import me.apomazkin.mate.effects
import me.apomazkin.mate.state
import me.apomazkin.mate.test.assertNoEffects
import me.apomazkin.mate.test.testReduce
import me.apomazkin.ui.resource.ResourceManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Unit tests для IS481 quiz picker branches в `ChatReducer`:
 * - `Msg.PrepareToStart` extended → emit `LoadQuizComponentTypes`.
 * - `Msg.SelectQuizComponent(ref)` → emit `SaveQuizPickerSelection`, state unchanged.
 * - `Msg.QuizComponentTypesLoaded(types, restored)` → updateQuizComponent с resolveSelection.
 */
class ChatReducerTest {

    private val logger = mockk<LexemeLogger>(relaxed = true)
    private val resourceManager = mockk<ResourceManager>().apply {
        every { stringByResId(any()) } returns "Welcome"
        every { stringByResId(any(), any()) } returns "Welcome"
    }
    private val reducer = ChatReducer(logger = logger, resourceManager = resourceManager)

    private fun translationType() = ComponentType(
        id = ComponentTypeId(1L),
        systemKey = BuiltInComponent.TRANSLATION,
        dictionaryId = null,
        name = null,
        template = ComponentTemplate.TEXT,
        position = 0,
        createdAt = Date(0L),
        updatedAt = Date(0L),
    )

    private fun definitionType() = ComponentType(
        id = ComponentTypeId(2L),
        systemKey = null,
        dictionaryId = 1L,
        name = "Definition",
        template = ComponentTemplate.TEXT,
        position = 1,
        createdAt = Date(0L),
        updatedAt = Date(0L),
    )

    private val builtInTranslation = ComponentTypeRef.BuiltIn(BuiltInComponent.TRANSLATION)
    private val userDefinition = ComponentTypeRef.UserDefined("Definition")

    private val initialState = ChatScreenState()

    // ===== PrepareToStart =====

    @Test
    fun `PrepareToStart emits LoadQuizComponentTypes effect`() {
        val result = reducer.testReduce(initialState, Msg.PrepareToStart)

        assertTrue(
            "should emit LoadQuizComponentTypes",
            result.effects().contains(DatasourceEffect.LoadQuizComponentTypes),
        )
    }

    // ===== SelectQuizComponent =====

    @Test
    fun `SelectQuizComponent emits SaveQuizPickerSelection, state unchanged`() {
        val result = reducer.testReduce(
            initialState,
            Msg.SelectQuizComponent(userDefinition),
        )

        assertEquals(initialState, result.state())
        assertEquals(
            setOf(DatasourceEffect.SaveQuizPickerSelection(userDefinition)),
            result.effects(),
        )
    }

    @Test
    fun `SelectQuizComponent without guard emits Save effect even when ref not in availableTypes`() {
        // F8 — intentional: eventual consistency через restore fallback в reducer.
        val stateWithTypes = initialState.updateQuizComponent(
            types = listOf(translationType(), definitionType()),
            selectedRef = builtInTranslation,
        )
        val notInList = ComponentTypeRef.UserDefined("Removed")

        val result = reducer.testReduce(stateWithTypes, Msg.SelectQuizComponent(notInList))

        assertEquals(stateWithTypes, result.state())
        assertEquals(
            setOf(DatasourceEffect.SaveQuizPickerSelection(notInList)),
            result.effects(),
        )
    }

    // ===== QuizComponentTypesLoaded =====

    @Test
    fun `QuizComponentTypesLoaded with empty types sets empty state, null selectedRef`() {
        val result = reducer.testReduce(
            initialState,
            Msg.QuizComponentTypesLoaded(types = emptyList(), restoredSelectedRef = builtInTranslation),
        )

        val qc = result.state().appBarState.itemsState.quizComponent
        assertTrue(qc.availableTypes.isEmpty())
        assertEquals(null, qc.selectedRef)
        result.assertNoEffects()
    }

    @Test
    fun `QuizComponentTypesLoaded restored valid - selectedRef preserved`() {
        val result = reducer.testReduce(
            initialState,
            Msg.QuizComponentTypesLoaded(
                types = listOf(translationType(), definitionType()),
                restoredSelectedRef = userDefinition,
            ),
        )

        val qc = result.state().appBarState.itemsState.quizComponent
        assertEquals(userDefinition, qc.selectedRef)
    }

    @Test
    fun `QuizComponentTypesLoaded restored translation only resolves to translation`() {
        val result = reducer.testReduce(
            initialState,
            Msg.QuizComponentTypesLoaded(
                types = listOf(translationType()),
                restoredSelectedRef = builtInTranslation,
            ),
        )

        val qc = result.state().appBarState.itemsState.quizComponent
        assertEquals(builtInTranslation, qc.selectedRef)
    }

    @Test
    fun `QuizComponentTypesLoaded restored invalid - fallback to first by position`() {
        val result = reducer.testReduce(
            initialState,
            Msg.QuizComponentTypesLoaded(
                types = listOf(translationType(), definitionType()),
                restoredSelectedRef = ComponentTypeRef.UserDefined("Removed"),
            ),
        )

        val qc = result.state().appBarState.itemsState.quizComponent
        assertEquals(builtInTranslation, qc.selectedRef)
    }

    @Test
    fun `QuizComponentTypesLoaded restored invalid single-type - fallback to only available`() {
        // F3 — single-type fallback.
        val result = reducer.testReduce(
            initialState,
            Msg.QuizComponentTypesLoaded(
                types = listOf(translationType()),
                restoredSelectedRef = ComponentTypeRef.UserDefined("Removed"),
            ),
        )

        val qc = result.state().appBarState.itemsState.quizComponent
        assertEquals(builtInTranslation, qc.selectedRef)
        assertEquals(false, qc.isPickerEnabled)
    }

    @Test
    fun `QuizComponentTypesLoaded restored null - default first`() {
        val result = reducer.testReduce(
            initialState,
            Msg.QuizComponentTypesLoaded(
                types = listOf(translationType(), definitionType()),
                restoredSelectedRef = null,
            ),
        )

        val qc = result.state().appBarState.itemsState.quizComponent
        assertEquals(builtInTranslation, qc.selectedRef)
    }

    @Test
    fun `QuizComponentTypesLoaded double-emit idempotent`() {
        // F7 — initial load и FlowHandler initial emit могут прийти подряд;
        // apply дважды → state stable.
        val msg = Msg.QuizComponentTypesLoaded(
            types = listOf(translationType(), definitionType()),
            restoredSelectedRef = builtInTranslation,
        )

        val r1 = reducer.testReduce(initialState, msg)
        val r2 = reducer.testReduce(r1.state(), msg)

        assertEquals(r1.state(), r2.state())
    }
}
