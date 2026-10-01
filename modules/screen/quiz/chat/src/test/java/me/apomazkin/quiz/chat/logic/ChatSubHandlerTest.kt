@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package me.apomazkin.quiz.chat.logic

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import me.apomazkin.lexeme.BuiltInComponent
import me.apomazkin.lexeme.ComponentTemplate
import me.apomazkin.lexeme.ComponentType
import me.apomazkin.lexeme.ComponentTypeId
import me.apomazkin.lexeme.ComponentTypeRef
import me.apomazkin.prefs.PrefKey
import me.apomazkin.prefs.PrefsProvider
import me.apomazkin.quiz.chat.deps.QuizChatUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Тесты [ChatSubHandler] — исполнителя семейства подписок [ChatSub].
 * `flow(sub)` — чистая фабрика Flow: тестируем сбором потока в runTest,
 * без раннера mate. Проверяем оба вида подписки:
 * - AppBarMenu: combine трёх pref-тумблеров → [Msg.UpdateMenu];
 * - QuizPicker: initial emit + re-emit при записи pref-ключа выбора,
 *   каждая эмиссия пере-выбирает доступные типы; без текущего словаря
 *   поток завершается пустым.
 */
class ChatSubHandlerTest {

    private val useCase = mockk<QuizChatUseCase>()
    private val prefsProvider = mockk<PrefsProvider>()

    private val handler get() = ChatSubHandler(useCase, prefsProvider)

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

    // ===== ChatSub.AppBarMenu =====

    @Test
    fun `AppBarMenu combines three pref toggles into UpdateMenu`() = runTest {
        every {
            prefsProvider.getBooleanFlow(PrefKey.CHAT_EARLIEST_REVIEWED_STATUS_BOOLEAN)
        } returns flowOf(true)
        every {
            prefsProvider.getBooleanFlow(PrefKey.CHAT_FREQUENT_MISTAKES_STATUS_BOOLEAN)
        } returns flowOf(false)
        every {
            prefsProvider.getBooleanFlow(PrefKey.CHAT_DEBUG_STATUS_BOOLEAN)
        } returns flowOf(true)

        val emissions = handler.flow(ChatSub.AppBarMenu).toList()

        assertEquals(
            listOf<Msg>(
                Msg.UpdateMenu(
                    isEarliestOn = true,
                    isFrequentMistakesOn = false,
                    isDebugOn = true,
                ),
            ),
            emissions,
        )
    }

    // ===== ChatSub.QuizPicker =====

    @Test
    fun `QuizPicker with current dict emits initial QuizComponentTypesLoaded`() = runTest {
        val prefFlow = MutableSharedFlow<String?>(replay = 1)
        prefFlow.tryEmit(null)
        coEvery { useCase.getCurrentDictionaryId() } returns 1L
        coEvery { useCase.getQuizCoreTypes(1L) } returns listOf(translationType)
        coEvery { useCase.getQuizPickerSelection(1L) } returns emptySet()
        coEvery { prefsProvider.getStringFlowByRawKey("quiz_picker_dict_1") } returns prefFlow

        val emissions = mutableListOf<Msg>()
        val job = launch { handler.flow(ChatSub.QuizPicker).collect { emissions += it } }
        advanceUntilIdle()
        job.cancel()

        assertEquals(1, emissions.size)
        val loaded = emissions.first() as Msg.QuizComponentTypesLoaded
        assertEquals(listOf(translationType), loaded.types)
        assertTrue(loaded.restoredSelectedRefs.isEmpty())
    }

    @Test
    fun `QuizPicker re-emits on each pref write`() = runTest {
        val prefFlow = MutableSharedFlow<String?>(replay = 1)
        prefFlow.tryEmit(null)
        coEvery { useCase.getCurrentDictionaryId() } returns 1L
        coEvery { useCase.getQuizCoreTypes(1L) } returns listOf(translationType)
        coEvery { useCase.getQuizPickerSelection(1L) } returnsMany listOf(
            emptySet(),
            setOf(ComponentTypeRef.UserDefined("Definition")),
        )
        coEvery { prefsProvider.getStringFlowByRawKey("quiz_picker_dict_1") } returns prefFlow

        val emissions = mutableListOf<Msg>()
        val job = launch { handler.flow(ChatSub.QuizPicker).collect { emissions += it } }
        advanceUntilIdle()
        prefFlow.tryEmit("user:Definition")
        advanceUntilIdle()
        job.cancel()

        assertEquals(2, emissions.size)
        val second = emissions[1] as Msg.QuizComponentTypesLoaded
        assertEquals(setOf(ComponentTypeRef.UserDefined("Definition")), second.restoredSelectedRefs)
    }

    @Test
    fun `QuizPicker completes empty when no current dict`() = runTest {
        coEvery { useCase.getCurrentDictionaryId() } returns null

        // Поток завершается сам (без коллекта pref-ключа) — toList не виснет.
        val emissions = handler.flow(ChatSub.QuizPicker).toList()

        assertTrue("no emissions when no current dict", emissions.isEmpty())
        coVerify(exactly = 0) { prefsProvider.getStringFlowByRawKey(any()) }
    }

    @Test
    fun `QuizPicker re-fetches types on each emit`() = runTest {
        val prefFlow = MutableSharedFlow<String?>(replay = 1)
        prefFlow.tryEmit(null)
        coEvery { useCase.getCurrentDictionaryId() } returns 1L
        coEvery { useCase.getQuizCoreTypes(1L) } returns listOf(translationType)
        coEvery { useCase.getQuizPickerSelection(1L) } returns emptySet()
        coEvery { prefsProvider.getStringFlowByRawKey("quiz_picker_dict_1") } returns prefFlow

        val job = launch { handler.flow(ChatSub.QuizPicker).collect { /* ignore */ } }
        advanceUntilIdle()
        prefFlow.tryEmit("builtin:translation")
        advanceUntilIdle()
        job.cancel()

        coVerify(atLeast = 2) { useCase.getQuizCoreTypes(1L) }
    }

    // ===== subscriptions() =====

    @Test
    fun `subscriptions are unconditional for any state`() {
        // Обе подписки живут всё время жизни экрана — набор константный.
        assertEquals(
            setOf(ChatSub.AppBarMenu, ChatSub.QuizPicker),
            ChatScreenState().subscriptions(),
        )
    }
}
