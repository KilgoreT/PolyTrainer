@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package me.apomazkin.quiz.chat.logic

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * IS481 unit tests для `QuizPickerFlowHandler`:
 * - Subscribe + initial DataStore emit → `QuizComponentTypesLoaded`.
 * - Re-emit on prefs write → `QuizComponentTypesLoaded` с обновлённым ref.
 * - No-op без current dict (F1 terminal state).
 * - Each emit re-fetches types.
 */
class QuizPickerFlowHandlerTest {

    private val useCase = mockk<QuizChatUseCase>()
    private val prefsProvider = mockk<PrefsProvider>()

    private val translationType = ComponentType(
        id = ComponentTypeId(1L),
        systemKey = BuiltInComponent.TRANSLATION,
        dictionaryId = null,
        name = null,
        template = ComponentTemplate.TEXT,
        position = 0,
        createdAt = Date(0L),
        updatedAt = Date(0L),
    )

    @Test
    fun `subscribe with current dict emits initial QuizComponentTypesLoaded`() = runTest {
        val flow = MutableSharedFlow<String?>(replay = 1)
        flow.tryEmit(null)
        coEvery { useCase.getCurrentDictionaryId() } returns 1L
        coEvery { useCase.getAvailableTypes(1L) } returns listOf(translationType)
        coEvery { useCase.getQuizPickerSelection(1L) } returns null
        coEvery { prefsProvider.getStringFlowByRawKey("quiz_picker_dict_1") } returns flow

        val handler = QuizPickerFlowHandler(useCase, prefsProvider)
        val emissions = mutableListOf<Msg>()

        handler.subscribe(this) { emissions += it }
        advanceUntilIdle()
        handler.unsubscribe()

        assertEquals(1, emissions.size)
        val loaded = emissions.first() as Msg.QuizComponentTypesLoaded
        assertEquals(listOf(translationType), loaded.types)
        assertEquals(null, loaded.restoredSelectedRef)
    }

    @Test
    fun `subscribe re-emits on each write`() = runTest {
        val flow = MutableSharedFlow<String?>(replay = 1)
        flow.tryEmit(null)
        coEvery { useCase.getCurrentDictionaryId() } returns 1L
        coEvery { useCase.getAvailableTypes(1L) } returns listOf(translationType)
        coEvery { useCase.getQuizPickerSelection(1L) } returnsMany listOf(
            null,
            ComponentTypeRef.UserDefined("Definition"),
        )
        coEvery { prefsProvider.getStringFlowByRawKey("quiz_picker_dict_1") } returns flow

        val handler = QuizPickerFlowHandler(useCase, prefsProvider)
        val emissions = mutableListOf<Msg>()

        handler.subscribe(this) { emissions += it }
        advanceUntilIdle()
        flow.tryEmit("user:Definition")
        advanceUntilIdle()
        handler.unsubscribe()

        assertEquals(2, emissions.size)
        val second = emissions[1] as Msg.QuizComponentTypesLoaded
        assertEquals(ComponentTypeRef.UserDefined("Definition"), second.restoredSelectedRef)
    }

    @Test
    fun `subscribe no-op when no current dict`() = runTest {
        coEvery { useCase.getCurrentDictionaryId() } returns null

        val handler = QuizPickerFlowHandler(useCase, prefsProvider)
        val emissions = mutableListOf<Msg>()

        handler.subscribe(this) { emissions += it }
        advanceUntilIdle()
        handler.unsubscribe()

        assertTrue("no emissions when no current dict", emissions.isEmpty())
        coVerify(exactly = 0) { prefsProvider.getStringFlowByRawKey(any()) }
    }

    @Test
    fun `each emit triggers re-fetch of types`() = runTest {
        val flow = MutableSharedFlow<String?>(replay = 1)
        flow.tryEmit(null)
        coEvery { useCase.getCurrentDictionaryId() } returns 1L
        coEvery { useCase.getAvailableTypes(1L) } returns listOf(translationType)
        coEvery { useCase.getQuizPickerSelection(1L) } returns null
        coEvery { prefsProvider.getStringFlowByRawKey("quiz_picker_dict_1") } returns flow

        val handler = QuizPickerFlowHandler(useCase, prefsProvider)

        handler.subscribe(this) { /* ignore */ }
        advanceUntilIdle()
        flow.tryEmit("builtin:translation")
        advanceUntilIdle()
        handler.unsubscribe()

        coVerify(atLeast = 2) { useCase.getAvailableTypes(1L) }
    }
}
